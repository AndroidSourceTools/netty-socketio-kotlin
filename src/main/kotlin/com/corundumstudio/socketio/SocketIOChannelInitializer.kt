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
package com.corundumstudio.socketio

import com.corundumstudio.socketio.ack.AckManager2
import com.corundumstudio.socketio.handler.*
import com.corundumstudio.socketio.namespace.NamespacesHub
import com.corundumstudio.socketio.protocol.PacketDecoder
import com.corundumstudio.socketio.protocol.PacketEncoder
import com.corundumstudio.socketio.scheduler.HashedWheelTimeoutScheduler
import com.corundumstudio.socketio.store.pubsub.DisconnectMessage
import com.corundumstudio.socketio.store.pubsub.PubSubType
import com.corundumstudio.socketio.transport.PollingTransport
import com.corundumstudio.socketio.transport.WebSocketTransport
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.ssl.SslHandler
import org.slf4j.LoggerFactory

import javax.net.ssl.*
import java.security.KeyStore

class SocketIOChannelInitializer : ChannelInitializer<Channel>(), DisconnectableHub {

    companion object {
        const val SOCKETIO_ENCODER = "socketioEncoder"
        const val WEB_SOCKET_TRANSPORT_COMPRESSION = "webSocketTransportCompression"
        const val WEB_SOCKET_TRANSPORT = "webSocketTransport"
        const val WEB_SOCKET_AGGREGATOR = "webSocketAggregator"
        const val XHR_POLLING_TRANSPORT = "xhrPollingTransport"
        const val AUTHORIZE_HANDLER = "authorizeHandler"
        const val PACKET_HANDLER = "packetHandler"
        const val HTTP_ENCODER = "httpEncoder"
        const val HTTP_COMPRESSION = "httpCompression"
        const val HTTP_AGGREGATOR = "httpAggregator"
        const val HTTP_REQUEST_DECODER = "httpDecoder"
        const val SSL_HANDLER = "ssl"
        const val RESOURCE_HANDLER = "resourceHandler"
        const val WRONG_URL_HANDLER = "wrongUrlBlocker"
    }

    private val log = LoggerFactory.getLogger(SocketIOChannelInitializer::class.java)

    private var ackManager: AckManager2? = null

    private var clientsBox = ClientsBox()
    private var authorizeHandler: AuthorizeHandler? = null
    private var xhrPollingTransport: PollingTransport? = null
    private var webSocketTransport: WebSocketTransport? = null
    private var webSocketTransportCompression = WebSocketServerCompressionHandler()
    private var encoderHandler: EncoderHandler? = null
    private var wrongUrlHandler: WrongUrlHandler? = null

    private var scheduler = HashedWheelTimeoutScheduler()

    private var packetHandler: InPacketHandler? = null
    private var sslContext: SSLContext? = null
    private var configuration: Configuration? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        scheduler.update(ctx)
    }

    fun start(configuration: Configuration, namespacesHub: NamespacesHub) {
        this.configuration = configuration

        ackManager = AckManager2(scheduler)

        val jsonSupport = configuration.getJsonSupport()
        val encoder = PacketEncoder(configuration, jsonSupport)
        val decoder = PacketDecoder(jsonSupport, ackManager)

        val connectPath = configuration.getContext() + "/"

        val isSsl = configuration.getKeyStore() != null
        if (isSsl) {
            try {
                sslContext = createSSLContext(configuration)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }

        val factory = configuration.getStoreFactory()
        authorizeHandler = AuthorizeHandler(connectPath, scheduler, configuration, namespacesHub, factory, this, ackManager, clientsBox)
        factory.init(namespacesHub, authorizeHandler, jsonSupport)
        xhrPollingTransport = PollingTransport(decoder, authorizeHandler, clientsBox)
        webSocketTransport = WebSocketTransport(isSsl, authorizeHandler, configuration, scheduler, clientsBox)

        val packetListener = PacketListener(ackManager, namespacesHub, xhrPollingTransport, scheduler)

        packetHandler = InPacketHandler(packetListener, decoder, namespacesHub, configuration.getExceptionListener())

        try {
            encoderHandler = EncoderHandler(configuration, encoder)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        wrongUrlHandler = WrongUrlHandler()
    }

    @Throws(Exception::class)
    override fun initChannel(ch: Channel) {
        val pipeline = ch.pipeline()
        addSslHandler(pipeline)
        addSocketioHandlers(pipeline)
    }

    /**
     * Adds the ssl handler
     *
     * @param pipeline - channel pipeline
     */
    protected fun addSslHandler(pipeline: ChannelPipeline) {
        sslContext?.let { sslContext ->
            val engine = sslContext.createSSLEngine()
            engine.useClientMode = false
            pipeline.addLast(SSL_HANDLER, SslHandler(engine))
        }
    }

    /**
     * Adds the socketio channel handlers
     *
     * @param pipeline - channel pipeline
     */
    protected fun addSocketioHandlers(pipeline: ChannelPipeline) {
        pipeline.addLast(HTTP_REQUEST_DECODER, HttpRequestDecoder())
        pipeline.addLast(HTTP_AGGREGATOR, object : HttpObjectAggregator(configuration!!.getMaxHttpContentLength()) {
            override fun newContinueResponse(start: HttpMessage?, maxContentLength: Int, pipeline: ChannelPipeline?): Any? {
                return null
            }
        })
        pipeline.addLast(HTTP_ENCODER, HttpResponseEncoder())

        if (configuration!!.isHttpCompression()) {
            pipeline.addLast(HTTP_COMPRESSION, HttpContentCompressor())
        }

        pipeline.addLast(PACKET_HANDLER, packetHandler)

        pipeline.addLast(AUTHORIZE_HANDLER, authorizeHandler)
        pipeline.addLast(XHR_POLLING_TRANSPORT, xhrPollingTransport)

        // TODO use single instance when https://github.com/netty/netty/issues/4755 will be resolved
        if (configuration!!.isWebsocketCompression()) {
            pipeline.addLast(WEB_SOCKET_TRANSPORT_COMPRESSION, WebSocketServerCompressionHandler())
        }

        pipeline.addLast(WEB_SOCKET_TRANSPORT, webSocketTransport)

        pipeline.addLast(SOCKETIO_ENCODER, encoderHandler)

        pipeline.addLast(WRONG_URL_HANDLER, wrongUrlHandler)
    }

    @Throws(Exception::class)
    private fun createSSLContext(configuration: Configuration) : SSLContext {
        var managers: Array<TrustManager>? = null
        if (configuration.getTrustStore() != null) {
            val ts = KeyStore.getInstance(configuration.getTrustStoreFormat())
            ts.load(configuration.getTrustStore(), configuration.getTrustStorePassword()?.toCharArray())
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(ts)
            managers = tmf.trustManagers
        }

        val ks = KeyStore.getInstance(configuration.getKeyStoreFormat())
        ks.load(configuration.getKeyStore(), configuration.getKeyStorePassword()?.toCharArray())

        val kmf = KeyManagerFactory.getInstance(configuration.getKeyManagerFactoryAlgorithm())
        kmf.init(ks, configuration.getKeyStorePassword()?.toCharArray())

        val serverContext = SSLContext.getInstance(configuration.getSSLProtocol())
        serverContext.init(kmf.keyManagers, managers, null)
        return serverContext
    }

    override fun onDisconnect(client: ClientHead) {
        ackManager?.onDisconnect(client)
        authorizeHandler?.onDisconnect(client)
        configuration?.getStoreFactory()?.onDisconnect(client)

        configuration?.getStoreFactory()?.pubSubStore()?.publish(PubSubType.DISCONNECT, DisconnectMessage(client.sessionId))

        log.debug("Client with sessionId: ${client.sessionId} disconnected")
    }

    fun stop() {
        val factory = configuration?.getStoreFactory()
        factory?.shutdown()
        scheduler.shutdown()
    }

}
