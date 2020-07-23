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

/**
 * TCP socket configuration contains configuration for main server channel
 * and client channels
 *
 * @see java.net.SocketOptions
 */
class SocketConfig {

    private var tcpNoDelay = true

    private var tcpSendBufferSize = -1

    private var tcpReceiveBufferSize = -1

    private var tcpKeepAlive = false

    private var soLinger = -1

    private var reuseAddress = false

    private var acceptBackLog = 1024

    fun isTcpNoDelay() = tcpNoDelay
    
    fun setTcpNoDelay(tcpNoDelay: Boolean) {
        this.tcpNoDelay = tcpNoDelay
    }

    fun getTcpSendBufferSize() = tcpSendBufferSize

    fun setTcpSendBufferSize(tcpSendBufferSize: Int) {
        this.tcpSendBufferSize = tcpSendBufferSize
    }

    fun getTcpReceiveBufferSize() = tcpReceiveBufferSize

    fun setTcpReceiveBufferSize(tcpReceiveBufferSize: Int) {
        this.tcpReceiveBufferSize = tcpReceiveBufferSize
    }

    fun isTcpKeepAlive() = tcpKeepAlive

    fun setTcpKeepAlive(tcpKeepAlive: Boolean) {
        this.tcpKeepAlive = tcpKeepAlive
    }

    fun getSoLinger() = soLinger

    fun setSoLinger(soLinger: Int) {
        this.soLinger = soLinger
    }

    fun isReuseAddress() = reuseAddress

    fun setReuseAddress(reuseAddress: Boolean) {
        this.reuseAddress = reuseAddress
    }

    fun getAcceptBackLog() = acceptBackLog

    fun setAcceptBackLog(acceptBackLog: Int) {
        this.acceptBackLog = acceptBackLog
    }

}
