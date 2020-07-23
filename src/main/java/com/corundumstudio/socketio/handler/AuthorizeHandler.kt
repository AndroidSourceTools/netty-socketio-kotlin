/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler

import com.corundumstudio.socketio.*
import com.corundumstudio.socketio.ack.AckManager2
import com.corundumstudio.socketio.messages.HttpErrorMessage
import com.corundumstudio.socketio.namespace.Namespace
import com.corundumstudio.socketio.namespace.NamespacesHub
import com.corundumstudio.socketio.protocol.AuthPacket
import com.corundumstudio.socketio.protocol.Packet
import com.corundumstudio.socketio.protocol.PacketType
import com.corundumstudio.socketio.scheduler.CancelableScheduler
import com.corundumstudio.socketio.scheduler.SchedulerKey
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type
import com.corundumstudio.socketio.store.StoreFactory
import com.corundumstudio.socketio.store.pubsub.ConnectMessage
import com.corundumstudio.socketio.store.pubsub.PubSubType
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import org.slf4j.LoggerFactory

import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

@Sharable
class AuthorizeHandler(
    private val connectPath: String,
    private val disconnectScheduler: CancelableScheduler,
    private val configuration: Configuration,
    private val namespacesHub: NamespacesHub,
    private val storeFactory: StoreFactory,
    private val disconnectable: DisconnectableHub,
    private val ackManager: AckManager2?,
    private val clientsBox: ClientsBox
) : ChannelInboundHandlerAdapter(), Disconnectable {

    private val log = LoggerFactory.getLogger(AuthorizeHandler::class.java)

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        val key = SchedulerKey(Type.PING_TIMEOUT, ctx.channel())
        disconnectScheduler.schedule(key, {
            ctx.channel().close()
            log.debug("Client with ip {} opened channel but doesn't send any data! Channel closed!", ctx.channel().remoteAddress())
        }, configuration.getFirstDataTimeout().toLong(), TimeUnit.MILLISECONDS)
        super.channelActive(ctx)
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, req: Any) {
        val key = SchedulerKey(Type.PING_TIMEOUT, ctx.channel())
        disconnectScheduler.cancel(key)

        if (req is FullHttpRequest) {
            val channel = ctx.channel()
            val queryDecoder = QueryStringDecoder(req.uri())

            if (!configuration.isAllowCustomRequests()
                && !queryDecoder.path().startsWith(connectPath)) {
                val res = DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
                channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE)
                req.release()
                return
            }

            val sid = queryDecoder.parameters().get("sid")
            if (queryDecoder.path() == connectPath && sid == null) {
                val origin = req.headers().get(HttpHeaderNames.ORIGIN)
                if (!authorize(ctx, channel, origin, queryDecoder.parameters(), req)) {
                    req.release()
                    return
                }
                // forward message to polling or websocket handler to bind channel
            }
        }
        ctx.fireChannelRead(req)
    }

    @Throws(Exception::class)
    private fun authorize(ctx: ChannelHandlerContext, channel: Channel, origin: String?, params: Map<String, List<String>>, req: FullHttpRequest) : Boolean {
        val headers = HashMap<String, List<String>>(req.headers().names().size)
        for (name in req.headers().names()) {
            val values = req.headers().getAll(name)
            headers[name] = values
        }

        val data = HandshakeData(req.headers(), params,
            channel.remoteAddress() as InetSocketAddress,
            channel.localAddress() as InetSocketAddress,
            req.uri(), origin != null && !origin.equals("null", true))

        var result = false
        try {
            result = configuration.getAuthorizationListener().isAuthorized(data)
        } catch (e: Exception) {
            log.error("Authorization error", e)
        }

        if (!result) {
            val res = DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
            channel.writeAndFlush(res)
                .addListener(ChannelFutureListener.CLOSE)
            log.debug("Handshake unauthorized, query params: {} headers: {}", params, headers)
            return false
        }

        val sessionId = if (configuration.isRandomSession()) {
            UUID.randomUUID()
        } else {
            this.generateOrGetSessionIdFromRequest(req.headers())
        }

        val transportValue = params["transport"]
        if (transportValue == null) {
            log.error("Got no transports for request {}", req.uri())

            val res = DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE)
            return false
        }

        val transport = Transport.byName(transportValue[0])
        if (!configuration.getTransports().contains(transport)) {
            val errorData = HashMap<String, Any>()
            errorData["code"] = 0
            errorData["message"] = "Transport unknown"

            channel.attr(EncoderHandler.ORIGIN).set(origin)
            channel.writeAndFlush(HttpErrorMessage(errorData))
            return false
        }

        val client = ClientHead(sessionId, ackManager, disconnectable, storeFactory, data, clientsBox, transport, disconnectScheduler, configuration)
        channel.attr(ClientHead.CLIENT).set(client)
        clientsBox.addClient(client)

        var transports: Array<String>? = null
        if (configuration.getTransports().contains(Transport.WEBSOCKET)) {
            transports = arrayOf("websocket")
        }

        val authPacket = AuthPacket(sessionId, transports, configuration.getPingInterval(),
            configuration.getPingTimeout())

        val packet = Packet(PacketType.OPEN)
        packet.setData(authPacket)
        client.send(packet)

        client.schedulePingTimeout()
        log.debug("Handshake authorized for sessionId: $sessionId, query params: $params headers: $headers")
        return true
    }

    /**
     * This method will either generate a new random sessionId or will retrieve the value stored
     * in the "io" cookie.  Failures to parse will cause a logging warning to be generated and a
     * random uuid to be generated instead (same as not passing a cookie in the first place).
     */
    private fun generateOrGetSessionIdFromRequest(headers: HttpHeaders) : UUID {
        val values = headers.getAll("io")
        if (values.size == 1) {
            try {
                return UUID.fromString(values[0])
            } catch (e: IllegalArgumentException) {
                log.warn("Malformed UUID received for session! io=${values[0]}")
            }
        }

        for (cookieHeader in headers.getAll(HttpHeaderNames.COOKIE)) {
            val cookies = ServerCookieDecoder.LAX.decode(cookieHeader)

            for (cookie in cookies) {
                if (cookie.name() == "io") {
                    try {
                        return UUID.fromString(cookie.value())
                    } catch (e: IllegalArgumentException) {
                        log.warn("Malformed UUID received for session! io=${cookie.value()}")
                    }
                }
            }
        }

        return UUID.randomUUID()
    }

    fun connect(sessionId: UUID) {
        val key = SchedulerKey(Type.PING_TIMEOUT, sessionId)
        disconnectScheduler.cancel(key)
    }

    fun connect(client: ClientHead) {
        val ns = namespacesHub.get(Namespace.DEFAULT_NAME)

        if (!client.namespaces.contains(ns)) {
            val packet = Packet(PacketType.MESSAGE)
            packet.setSubType(PacketType.CONNECT)
            client.send(packet)

            configuration.getStoreFactory().pubSubStore().publish(PubSubType.CONNECT, ConnectMessage(client.getSessionId()))

            val nsClient = client.addNamespaceClient(ns)
            ns.onConnect(nsClient)
        }
    }

    override fun onDisconnect(client: ClientHead) {
        clientsBox.removeClient(client.sessionId)
    }

}
