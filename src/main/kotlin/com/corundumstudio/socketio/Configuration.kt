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

import com.corundumstudio.socketio.handler.SuccessAuthorizationListener
import com.corundumstudio.socketio.listener.DefaultExceptionListener
import com.corundumstudio.socketio.listener.ExceptionListener
import com.corundumstudio.socketio.protocol.JsonSupport
import com.corundumstudio.socketio.store.MemoryStoreFactory
import com.corundumstudio.socketio.store.StoreFactory

import javax.net.ssl.KeyManagerFactory
import java.io.InputStream

class Configuration {

    private var exceptionListener: ExceptionListener = DefaultExceptionListener()

    private var context = "/socket.io"

    private var transports = listOf(Transport.WEBSOCKET, Transport.POLLING)

    private var bossThreads = 0 // 0 = current_processors_amount * 2
    private var workerThreads = 0 // 0 = current_processors_amount * 2
    private var useLinuxNativeEpoll = false

    private var allowCustomRequests = false

    private var upgradeTimeout = 10000
    private var pingTimeout = 60000
    private var pingInterval = 25000
    private var firstDataTimeout = 5000

    private var maxHttpContentLength = 64 * 1024
    private var maxFramePayloadLength = 64 * 1024

    private var packagePrefix: String? = null
    private var hostname: String? = null
    private var port = -1

    private var sslProtocol = "TLSv1"

    private var keyStoreFormat = "JKS"
    private var keyStore: InputStream? = null
    private var keyStorePassword: String? = null

    private var trustStoreFormat = "JKS"
    private var trustStore: InputStream? = null
    private var trustStorePassword: String? = null

    private var keyManagerFactoryAlgorithm = KeyManagerFactory.getDefaultAlgorithm()

    private var preferDirectBuffer = true

    private var socketConfig = SocketConfig()

    private var storeFactory: StoreFactory = MemoryStoreFactory()

    private var jsonSupport: JsonSupport? = null

    private var authorizationListener: AuthorizationListener = SuccessAuthorizationListener()

    private var ackMode = AckMode.AUTO_SUCCESS_ONLY

    private var addVersionHeader = true

    private var origin: String? = null

    private var httpCompression = true

    private var websocketCompression = true

    private var randomSession = false

    constructor()

    /**
     * Defend from further modifications by cloning
     *
     * @param conf - Configuration object to clone
     */
    constructor(conf: Configuration? = null) {
        if (conf == null) return

        setBossThreads(conf.getBossThreads())
        setWorkerThreads(conf.getWorkerThreads())
        setUseLinuxNativeEpoll(conf.isUseLinuxNativeEpoll())

        setPingInterval(conf.getPingInterval())
        setPingTimeout(conf.getPingTimeout())

        setHostname(conf.getHostname())
        setPort(conf.getPort())

        if (conf.getJsonSupport() == null) {
            try {
                javaClass.classLoader.loadClass("com.fasterxml.jackson.databind.ObjectMapper")
                try {
                    val jjs = javaClass.classLoader.loadClass("com.corundumstudio.socketio.protocol.JacksonJsonSupport")
                    val js = jjs.getConstructor().newInstance() as JsonSupport
                    conf.setJsonSupport(js)
                } catch (e: Exception) {
                    throw IllegalArgumentException(e)
                }
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Can't find jackson lib in classpath", e)
            }
        }

        setJsonSupport(JsonSupportWrapper(conf.getJsonSupport()))
        setContext(conf.getContext())
        setAllowCustomRequests(conf.isAllowCustomRequests())

        setKeyStorePassword(conf.getKeyStorePassword())
        setKeyStore(conf.getKeyStore())
        setKeyStoreFormat(conf.getKeyStoreFormat())
        setTrustStore(conf.getTrustStore())
        setTrustStoreFormat(conf.getTrustStoreFormat())
        setTrustStorePassword(conf.getTrustStorePassword())
        setKeyManagerFactoryAlgorithm(conf.getKeyManagerFactoryAlgorithm())

        setTransports(*conf.getTransports().toTypedArray())
        setMaxHttpContentLength(conf.getMaxHttpContentLength())
        setPackagePrefix(conf.getPackagePrefix())

        setPreferDirectBuffer(conf.isPreferDirectBuffer())
        setStoreFactory(conf.getStoreFactory())
        setAuthorizationListener(conf.getAuthorizationListener())
        setExceptionListener(conf.getExceptionListener())
        setSocketConfig(conf.getSocketConfig())
        setAckMode(conf.getAckMode())
        setMaxFramePayloadLength(conf.getMaxFramePayloadLength())
        setUpgradeTimeout(conf.getUpgradeTimeout())

        setAddVersionHeader(conf.isAddVersionHeader())
        setOrigin(conf.getOrigin())
        setSSLProtocol(conf.getSSLProtocol())

        setHttpCompression(conf.isHttpCompression())
        setWebsocketCompression(conf.isWebsocketCompression())
        setRandomSession(conf.randomSession)
    }

    fun getJsonSupport() = jsonSupport

    /**
     * Allows to setup custom implementation of
     * JSON serialization/deserialization
     *
     * @param jsonSupport - json mapper
     *
     * @see JsonSupport
     */
    fun setJsonSupport(jsonSupport: JsonSupport) {
        this.jsonSupport = jsonSupport
    }

    fun getHostname() = hostname

    /**
     * Optional parameter. If not set then bind address
     * will be 0.0.0.0 or ::0
     *
     * @param hostname - name of host
     */
    fun setHostname(hostname: String?) {
        this.hostname = hostname
    }

    fun getPort() = port
    
    fun setPort(port: Int) {
        this.port = port
    }

    fun getBossThreads() = bossThreads
    
    fun setBossThreads(bossThreads: Int) {
        this.bossThreads = bossThreads
    }

    fun getWorkerThreads() = workerThreads
    
    fun setWorkerThreads(workerThreads: Int) {
        this.workerThreads = workerThreads
    }

    /**
     * Ping interval
     *
     * @param heartbeatIntervalSecs - time in milliseconds
     */
    fun setPingInterval(heartbeatIntervalSecs: Int) {
        this.pingInterval = heartbeatIntervalSecs
    }
    
    fun getPingInterval() = pingInterval
    
    /**
     * Ping timeout
     * Use <code>0</code> to disable it
     *
     * @param heartbeatTimeoutSecs - time in milliseconds
     */
    fun setPingTimeout(heartbeatTimeoutSecs: Int) {
        this.pingTimeout = heartbeatTimeoutSecs
    }
    
    fun getPingTimeout() = pingTimeout
    
    fun isHeartbeatsEnabled() = pingTimeout > 0

    fun getContext() = context
    
    fun setContext(context: String) {
        this.context = context
    }

    fun isAllowCustomRequests() = allowCustomRequests

    /**
     * Allow to service custom requests differs from socket.io protocol.
     * In this case it's necessary to add own handler which handle them
     * to avoid hang connections.
     * Default is {@code false}
     *
     * @param allowCustomRequests - {@code true} to allow
     */
    fun setAllowCustomRequests(allowCustomRequests: Boolean) {
        this.allowCustomRequests = allowCustomRequests
    }

    /**
     * SSL key store password
     *
     * @param keyStorePassword - password of key store
     */
    fun setKeyStorePassword(keyStorePassword: String?) {
        this.keyStorePassword = keyStorePassword
    }
    
    fun getKeyStorePassword() = keyStorePassword

    /**
     * SSL key store stream, maybe appointed to any source
     *
     * @param keyStore - key store input stream
     */
    fun setKeyStore(keyStore: InputStream?) {
        this.keyStore = keyStore
    }
    
    fun getKeyStore() = keyStore

    /**
     * Key store format
     *
     * @param keyStoreFormat - key store format
     */
    fun setKeyStoreFormat(keyStoreFormat: String) {
        this.keyStoreFormat = keyStoreFormat
    }
    
    fun getKeyStoreFormat() = keyStoreFormat

    /**
     * Set maximum http content length limit
     *
     * @param value
     *        the maximum length of the aggregated http content.
     */
    fun setMaxHttpContentLength(value: Int) {
        this.maxHttpContentLength = value
    }

    fun getMaxHttpContentLength() = maxHttpContentLength

    /**
     * Transports supported by server
     *
     * @param transports - list of transports
     */
    fun setTransports(vararg transports: Transport) {
        if (transports.isEmpty())
            throw IllegalArgumentException("Transports list can't be empty")
        this.transports = transports.toList()
    }

    fun getTransports() = transports

    /**
     * Package prefix for sending json-object from client
     * without full class name.
     *
     * With defined package prefix socket.io client
     * just need to define '@class: 'SomeType'' in json object
     * instead of '@class: 'com.full.package.name.SomeType''
     *
     * @param packagePrefix - prefix string
     *
     */
    fun setPackagePrefix(packagePrefix: String?) {
        this.packagePrefix = packagePrefix
    }

    fun getPackagePrefix() = packagePrefix

    /**
     * Buffer allocation method used during packet encoding.
     * Default is {@code true}
     *
     * @param preferDirectBuffer    {@code true} if a direct buffer should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              buffer, which is backed by an byte array.
     */
    fun setPreferDirectBuffer(preferDirectBuffer: Boolean) {
        this.preferDirectBuffer = preferDirectBuffer
    }

    fun isPreferDirectBuffer() = preferDirectBuffer

    /**
     * Data store - used to store session data and implements distributed pubsub.
     * Default is {@code MemoryStoreFactory}
     *
     * @param clientStoreFactory - implements StoreFactory
     *
     * @see MemoryStoreFactory
     * @see com.corundumstudio.socketio.store.RedissonStoreFactory
     * @see com.corundumstudio.socketio.store.HazelcastStoreFactory
     */
    fun setStoreFactory(clientStoreFactory: StoreFactory) {
        this.storeFactory = clientStoreFactory
    }

    fun getStoreFactory() = storeFactory

    /**
     * Authorization listener invoked on every handshake.
     * Accepts or denies a client by {@code AuthorizationListener.isAuthorized} method.
     * <b>Accepts</b> all clients by default.
     *
     * @param authorizationListener - authorization listener itself
     *
     * @see AuthorizationListener
     */
    fun setAuthorizationListener(authorizationListener: AuthorizationListener) {
        this.authorizationListener = authorizationListener
    }

    fun getAuthorizationListener() = authorizationListener

    /**
     * Exception listener invoked on any exception in
     * SocketIO listener
     *
     * @param exceptionListener - listener
     *
     * @see ExceptionListener
     */
    fun setExceptionListener(exceptionListener: ExceptionListener) {
        this.exceptionListener = exceptionListener
    }

    fun getExceptionListener() = exceptionListener

    fun getSocketConfig() = socketConfig
    /**
     * TCP socket configuration
     *
     * @param socketConfig - config
     */
    fun setSocketConfig(socketConfig: SocketConfig) {
        this.socketConfig = socketConfig
    }

    /**
     * Auto ack-response mode
     * Default is {@code AckMode.AUTO_SUCCESS_ONLY}
     *
     * @see AckMode
     *
     * @param ackMode - ack mode
     */
    fun setAckMode(ackMode: AckMode) {
        this.ackMode = ackMode
    }
    
    fun getAckMode() = ackMode

    fun getTrustStoreFormat() = trustStoreFormat
    
    fun setTrustStoreFormat(trustStoreFormat: String) {
        this.trustStoreFormat = trustStoreFormat
    }

    fun getTrustStore() = trustStore
    
    fun setTrustStore(trustStore: InputStream?) {
        this.trustStore = trustStore
    }

    fun getTrustStorePassword() = trustStorePassword

    fun setTrustStorePassword(trustStorePassword: String?) {
        this.trustStorePassword = trustStorePassword
    }

    fun getKeyManagerFactoryAlgorithm() = keyManagerFactoryAlgorithm

    fun setKeyManagerFactoryAlgorithm(keyManagerFactoryAlgorithm: String) {
        this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm
    }


    /**
     * Set maximum websocket frame content length limit
     *
     * @param maxFramePayloadLength - length
     */
    fun setMaxFramePayloadLength(maxFramePayloadLength: Int) {
        this.maxFramePayloadLength = maxFramePayloadLength
    }

    fun getMaxFramePayloadLength() = maxFramePayloadLength

    /**
     * Transport upgrade timeout in milliseconds
     *
     * @param upgradeTimeout - upgrade timeout
     */
    fun setUpgradeTimeout(upgradeTimeout: Int) {
        this.upgradeTimeout = upgradeTimeout
    }

    fun getUpgradeTimeout() = upgradeTimeout

    /**
     * Adds <b>Server</b> header with lib version to http response.
     * <p>
     * Default is <code>true</code>
     *
     * @param addVersionHeader - <code>true</code> to add header
     */
    fun setAddVersionHeader(addVersionHeader: Boolean) {
        this.addVersionHeader = addVersionHeader
    }

    fun isAddVersionHeader() = addVersionHeader

    /**
     * Set <b>Access-Control-Allow-Origin</b> header value for http each
     * response.
     * Default is <code>null</code>
     *
     * If value is <code>null</code> then request <b>ORIGIN</b> header value used.
     *
     * @param origin - origin
     */
    fun setOrigin(origin: String?) {
        this.origin = origin
    }

    fun getOrigin() = origin

    fun isUseLinuxNativeEpoll() = useLinuxNativeEpoll

    fun setUseLinuxNativeEpoll(useLinuxNativeEpoll: Boolean) {
        this.useLinuxNativeEpoll = useLinuxNativeEpoll
    }

    /**
     * Set the name of the requested SSL protocol
     *
     * @param sslProtocol - name of protocol
     */
    fun setSSLProtocol(sslProtocol: String) {
        this.sslProtocol = sslProtocol
    }

    fun getSSLProtocol() = sslProtocol

    /**
     * Timeout between channel opening and first data transfer
     * Helps to avoid 'silent channel' attack and prevents
     * 'Too many open files' problem in this case
     *
     * @param firstDataTimeout - timeout value
     */
    fun setFirstDataTimeout(firstDataTimeout: Int) {
        this.firstDataTimeout = firstDataTimeout
    }

    fun getFirstDataTimeout() = firstDataTimeout

    /**
     * Activate http protocol compression. Uses {@code gzip} or
     * {@code deflate} encoding choice depends on the {@code "Accept-Encoding"} header value.
     * <p>
     * Default is <code>true</code>
     *
     * @param httpCompression - <code>true</code> to use http compression
     */
    fun setHttpCompression(httpCompression: Boolean) {
        this.httpCompression = httpCompression
    }

    fun isHttpCompression() = httpCompression

    /**
     * Activate websocket protocol compression.
     * Uses {@code permessage-deflate} encoding only.
     * <p>
     * Default is <code>true</code>
     *
     * @param websocketCompression - <code>true</code> to use websocket compression
     */
    fun setWebsocketCompression(websocketCompression: Boolean) {
        this.websocketCompression = websocketCompression
    }

    fun isWebsocketCompression() = websocketCompression

    fun isRandomSession() = randomSession

    fun setRandomSession(randomSession: Boolean) {
        this.randomSession = randomSession
    }
}
