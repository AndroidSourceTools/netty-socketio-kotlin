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

import com.corundumstudio.socketio.misc.IterableCollection
import com.corundumstudio.socketio.namespace.Namespace
import com.corundumstudio.socketio.protocol.Packet
import com.corundumstudio.socketio.protocol.PacketType
import com.corundumstudio.socketio.store.StoreFactory
import com.corundumstudio.socketio.store.pubsub.DispatchMessage
import com.corundumstudio.socketio.store.pubsub.PubSubType

import kotlin.collections.HashSet

/**
 * Fully thread-safe.
 */
class BroadcastOperations(
    private val clients: Iterable<SocketIOClient>,
    private val storeFactory: StoreFactory
) : ClientOperations {

    private fun dispatch(packet: Packet) {
        val namespaceRooms = hashMapOf<String, HashSet<String>>()
        for (socketIOClient in clients) {
            val namespace = socketIOClient.getNamespace() as Namespace
            val rooms = namespace.getRooms(socketIOClient)

            var roomsList = namespaceRooms[namespace.getName()]
            if (roomsList == null) {
                roomsList = hashSetOf()
                namespaceRooms[namespace.getName()] = roomsList
            }
            roomsList.addAll(rooms)
        }
        namespaceRooms.entries.forEach { entry ->
            entry.value.forEach { room ->
                storeFactory.pubSubStore().publish(PubSubType.DISPATCH, DispatchMessage(room, packet, entry.key))
            }
        }
    }

    fun getClients() = IterableCollection(clients)

    override fun send(packet: Packet) {
        clients.forEach { client -> client.send(packet) }
        dispatch(packet)
    }

    fun <T> send(packet: Packet, ackCallback: BroadcastAckCallback<T>) {
        clients.forEach { client ->
            client.send(packet, ackCallback.createClientCallback(client))
        }
        ackCallback.loopFinished()
    }

    override fun disconnect() = clients.forEach { client -> client.disconnect() }

    fun sendEvent(name: String, excludedClient: SocketIOClient, vararg data: Any?) {
        val packet = Packet(PacketType.MESSAGE).apply {
            setSubType(PacketType.EVENT)
            setName(name)
            setData(listOf(data))
        }
        for (client in clients) {
            if (client.getSessionId() == excludedClient.getSessionId())
                continue
            client.send(packet)
        }
        dispatch(packet)
    }

    override fun sendEvent(name: String, vararg data: Any?) =
        send(Packet(PacketType.MESSAGE).apply {
            setSubType(PacketType.EVENT)
            setName(name)
            setData(listOf(data))
        })

    fun <T> sendEvent(name: String, data: Any, ackCallback: BroadcastAckCallback<T>) {
        for (client in clients)
            client.sendEvent(name, ackCallback.createClientCallback(client), data)
        ackCallback.loopFinished()
    }

    fun <T> sendEvent(name: String, data: Any, excludedClient: SocketIOClient, ackCallback: BroadcastAckCallback<T>) {
        for (client in clients) {
            if (client.getSessionId() == excludedClient.getSessionId())
                continue
            client.sendEvent(name, ackCallback.createClientCallback(client), data)
        }
        ackCallback.loopFinished()
    }


}