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

import com.corundumstudio.socketio.protocol.Packet
import com.corundumstudio.socketio.store.Store

import java.net.SocketAddress
import java.util.UUID

/**
 * Fully thread-safe.
 */
interface SocketIOClient : ClientOperations, Store {

    /**
     * Handshake data used during client connection
     *
     * @return HandshakeData
     */
    fun getHandshakeData() : HandshakeData

    /**
     * Current client transport protocol
     *
     * @return transport protocol
     */
    fun getTransport() : Transport

    /**
     * Send event with ack callback
     *
     * @param name - event name
     * @param data - event data
     * @param ackCallback - ack callback
     */
    fun sendEvent(name: String, ackCallback: AckCallback<*>, vararg data: Any?)

    /**
     * Send packet with ack callback
     *
     * @param packet - packet to send
     * @param ackCallback - ack callback
     */
    fun send(packet: Packet, ackCallback: AckCallback<*>)

    /**
     * Client namespace
     *
     * @return - namespace
     */
    fun getNamespace() : SocketIONamespace

    /**
     * Client session id, uses {@link UUID} object
     *
     * @return - session id
     */
    fun getSessionId() : UUID

    /**
     * Get client remote address
     *
     * @return remote address
     */
    fun getRemoteAddress() : SocketAddress

    /**
     * Check is underlying channel open
     *
     * @return <code>true</code> if channel open, otherwise <code>false</code>
     */
    fun isChannelOpen() : Boolean

    /**
     * Join client to room
     *
     * @param room - name of room
     */
    fun joinRoom(room: String)

    /**
     * Join client to room
     *
     * @param room - name of room
     */
    fun leaveRoom(room: String)

    /**
     * Get all rooms a client is joined in.
     *
     * @return name of rooms
     */
    fun getAllRooms() : Set<String>

}
