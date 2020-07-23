/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import com.corundumstudio.socketio.protocol.Packet
import com.corundumstudio.socketio.protocol.PacketType

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ack request received from Socket.IO client.
 * You can always check is it <code>true</code> through
 * {@link #isAckRequested()} method.
 *
 * You can call {@link #sendAckData} methods only during
 * {@link DataListener#onData} invocation. If {@link #sendAckData}
 * not called it will be invoked with empty arguments right after
 * {@link DataListener#onData} method execution by server.
 *
 * This object is NOT actual anymore if {@link #sendAckData} was
 * executed or {@link DataListener#onData} invocation finished.
 *
 */
class AckRequest(
    private val originalPacket: Packet,
    private val client: SocketIOClient
) {

    private val sended = AtomicBoolean()

    /**
     * Check whether ack request was made
     *
     * @return true if ack requested by client
     */
    fun isAckRequested() = originalPacket.isAckRequested()

    /**
     * Send ack data to client.
     * Can be invoked only once during {@link DataListener#onData}
     * method invocation.
     *
     * @param objs - ack data objects
     */
    fun sendAckData(vararg objs: Any?) {
        sendAckData(objs.toList())
    }

    /**
     * Send ack data to client.
     * Can be invoked only once during {@link DataListener#onData}
     * method invocation.
     *
     * @param objs - ack data object list
     */
    fun sendAckData(objs: List<Any>) {
        if (!isAckRequested() || !sended.compareAndSet(false, true))
            return
        val ackPacket = Packet(PacketType.MESSAGE).apply {
            setSubType(PacketType.ACK)
            setAckId(originalPacket.getAckId())
            setData(objs)
        }
        client.send(ackPacket)
    }

}