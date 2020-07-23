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

import com.corundumstudio.socketio.listener.*
import com.corundumstudio.socketio.namespace.Namespace
import com.corundumstudio.socketio.namespace.NamespacesHub
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.FixedRecvByteBufAllocator
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*

/**
 * Fully thread-safe.
 */
class SocketIOServer(private val configuration: Configuration) : ClientListeners {

    private val log = LoggerFactory.getLogger(SocketIOServer::class.java)

    private val configCopy: Configuration

    private val namespacesHub: NamespacesHub
    private val mainNamespace: SocketIONamespace

    private var pipelineFactory = SocketIOChannelInitializer()

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null

    init {
        this.configCopy = Configuration(configuration)
        namespacesHub = NamespacesHub(configCopy)
        mainNamespace = addNamespace(Namespace.DEFAULT_NAME)
    }

    fun setPipelineFactory(pipelineFactory: SocketIOChannelInitializer) {
        this.pipelineFactory = pipelineFactory
    }

    /**
     * Get all clients connected to default namespace
     *
     * @return clients collection
     */
    fun getAllClients() = namespacesHub.get(Namespace.DEFAULT_NAME).getAllClients()

    /**
     * Get client by uuid from default namespace
     *
     * @param uuid - id of client
     * @return client
     */
    fun getClient(uuid: UUID) = namespacesHub.get(Namespace.DEFAULT_NAME).getClient(uuid)

    /**
     * Get all namespaces
     *
     * @return namespaces collection
     */
    fun getAllNamespaces() = namespacesHub.getAllNamespaces()

    fun getBroadcastOperations() = BroadcastOperations(getAllClients(), configCopy.getStoreFactory())

    /**
     * Get broadcast operations for clients within
     * room by <code>room</code> name
     *
     * @param room - name of room
     * @return broadcast operations
     */
    fun getRoomOperations(room: String) =
        BroadcastOperations(namespacesHub.getRoomClients(room), configCopy.getStoreFactory())

    /**
     * Start server
     */
    fun start() {
        startAsync().syncUninterruptibly()
    }

    /**
     * Start server asynchronously
     *
     * @return void
     */
    fun startAsync() : Future<Void> {
        log.info("Session store / pubsub factory used: {}", configCopy.getStoreFactory())
        initGroups()

        pipelineFactory.start(configCopy, namespacesHub)

        var channelClass: Class<out ServerChannel> = NioServerSocketChannel::class.java
        if (configCopy.isUseLinuxNativeEpoll()) {
            channelClass = EpollServerSocketChannel::class.java
        }

        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            .channel(channelClass)
            .childHandler(pipelineFactory)
        applyConnectionOptions(b)

        var addr = InetSocketAddress(configCopy.getPort())
        if (configCopy.getHostname() != null) {
            addr = InetSocketAddress(configCopy.getHostname(), configCopy.getPort())
        }

        return b.bind(addr).addListener(FutureListener { future ->
            if (future.isSuccess)
                log.info("SocketIO server started at port: ${configCopy.getPort()}")
            else
                log.error("SocketIO server start failed at port: ${configCopy.getPort()}!")
        })
    }

    protected fun applyConnectionOptions(bootstrap: ServerBootstrap) {
        val config = configCopy.getSocketConfig()
        bootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
        if (config.getTcpSendBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getTcpSendBufferSize())
        }
        if (config.getTcpReceiveBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, config.getTcpReceiveBufferSize())
            bootstrap.childOption(
                ChannelOption.RCVBUF_ALLOCATOR,
                FixedRecvByteBufAllocator (config.getTcpReceiveBufferSize())
            )
        }
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive())
        bootstrap.childOption(ChannelOption.SO_LINGER, config.getSoLinger())

        bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress())
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog())
    }

    protected fun initGroups() {
        if (configCopy.isUseLinuxNativeEpoll()) {
            bossGroup = EpollEventLoopGroup(configCopy.getBossThreads())
            workerGroup = EpollEventLoopGroup(configCopy.getWorkerThreads())
        } else {
            bossGroup = NioEventLoopGroup(configCopy.getBossThreads())
            workerGroup = NioEventLoopGroup(configCopy.getWorkerThreads())
        }
    }

    /**
     * Stop server
     */
    fun stop() {
        bossGroup?.shutdownGracefully()?.syncUninterruptibly()
        workerGroup?.shutdownGracefully()?.syncUninterruptibly()

        pipelineFactory.stop()
        log.info("SocketIO server stopped")
    }

    fun addNamespace(name: String) = namespacesHub.create(name)

    fun getNamespace(name: String) = namespacesHub.get(name)

    fun removeNamespace(name: String) = namespacesHub.remove(name)

    /**
     * Allows to get configuration provided
     * during server creation. Further changes on
     * this object not affect server.
     *
     * @return Configuration object
     */
    fun getConfiguration() = configuration

    override fun addMultiTypeEventListener(eventName: String, listener: MultiTypeEventListener, vararg eventClass: Class<*>) {
        mainNamespace.addMultiTypeEventListener(eventName, listener, *eventClass)
    }

    override fun <T> addEventListener(eventName: String, eventClass: Class<T>, listener: DataListener<T>) {
        mainNamespace.addEventListener(eventName, eventClass, listener)
    }

    override fun addEventInterceptor(eventInterceptor: EventInterceptor) {
        mainNamespace.addEventInterceptor(eventInterceptor)
    }

    override fun removeAllListeners(eventName: String) {
        mainNamespace.removeAllListeners(eventName)
    }

    override fun addDisconnectListener(listener: DisconnectListener) {
        mainNamespace.addDisconnectListener(listener)
    }

    override fun addConnectListener(listener: ConnectListener) {
        mainNamespace.addConnectListener(listener)
    }

    override fun addPingListener(listener: PingListener) {
        mainNamespace.addPingListener(listener)
    }

    override fun addListeners(listeners: Any) {
        mainNamespace.addListeners(listeners)
    }

    override fun addListeners(listeners: Any, listenersClass: Class<*>) {
        mainNamespace.addListeners(listeners, listenersClass)
    }

}
