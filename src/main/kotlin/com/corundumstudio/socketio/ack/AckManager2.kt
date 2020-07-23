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
package com.corundumstudio.socketio.ack

import com.corundumstudio.socketio.*
import com.corundumstudio.socketio.handler.ClientHead
import com.corundumstudio.socketio.protocol.Packet
import com.corundumstudio.socketio.scheduler.CancelableScheduler
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type
import io.netty.util.internal.PlatformDependent
import org.slf4j.LoggerFactory

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class AckManager2(private val scheduler: CancelableScheduler) : Disconnectable {

    class AckEntry {

        val ackCallbacks = PlatformDependent.newConcurrentHashMap<Long, AckCallback<*>>()
        val ackIndex = AtomicLong(-1)

        fun addAckCallback(callback: AckCallback<*>) : Long {
            val index = ackIndex.incrementAndGet()
            ackCallbacks[index] = callback
            return index
        }

        fun getAckIndexes() = ackCallbacks.keys

        fun getAckCallback(index: Long) = ackCallbacks[index]

        fun removeCallback(index: Long) = ackCallbacks.remove(index)

        fun initAckIndex(index: Long) = ackIndex.compareAndSet(-1, index)

    }

    private val log = LoggerFactory.getLogger(AckManager2::class.java)

    private val ackEntries = PlatformDependent.newConcurrentHashMap<UUID, AckEntry>()

    fun initAckIndex(sessionId: UUID, index: Long) {
        val ackEntry = getAckEntry(sessionId)
        ackEntry.initAckIndex(index)
    }

    private fun getAckEntry(sessionId: UUID) : AckEntry {
        var ackEntry = ackEntries[sessionId]
        if (ackEntry == null) {
            ackEntry = AckEntry()
            val oldAckEntry = ackEntries.putIfAbsent(sessionId, ackEntry)
            if (oldAckEntry != null)
                ackEntry = oldAckEntry
        }
        return ackEntry
    }

    fun onAck(client: SocketIOClient, packet: Packet) {
        scheduler.cancel(AckSchedulerKey(Type.ACK_TIMEOUT, client.getSessionId(), packet.getAckId()!!))

        val callback = removeCallback(client.getSessionId(), packet.getAckId()!!) ?: return

        if (callback is MultiTypeAckCallback) {
            callback.onSuccess(MultiTypeArgs(packet.getData()))
        } else {
            val args = packet.getData<List<Any>>()

            var param: Any? = null
            if (args.isNotEmpty())
                param = args[0]

            if (args.size > 1)
                log.error("Wrong ack args amount. Should be only one argument, but current amount is: ${args.size}. Ack id: ${packet.getAckId()}, sessionId: ${client.getSessionId()}")

            //callback.onSuccess(param)
        }
    }

    private fun removeCallback(sessionId: UUID, index: Long) : AckCallback<*>? {
        val ackEntry = ackEntries[sessionId]
        // may be null if client disconnected
        // before timeout occurs
        if (ackEntry != null)
            return ackEntry.removeCallback(index)
        return null
    }

    fun getCallback(sessionId: UUID, index: Long) = getAckEntry(sessionId).getAckCallback(index)

    fun registerAck(sessionId: UUID, callback: AckCallback<*>) : Long {
        val ackEntry = getAckEntry(sessionId)
        ackEntry.initAckIndex(0)
        val index = ackEntry.addAckCallback(callback)

        if (log.isDebugEnabled)
            log.debug("AckCallback registered with id: $index for client: $sessionId")

        scheduleTimeout(index, sessionId, callback)

        return index
    }

    private fun scheduleTimeout(index: Long, sessionId: UUID, callback: AckCallback<*>) {
        if (callback.timeout == -1) {
            return
        }
        val key = AckSchedulerKey(Type.ACK_TIMEOUT, sessionId, index)
        scheduler.scheduleCallback(key, {
            removeCallback(sessionId, index)?.onTimeout()
        }, callback.timeout.toLong(), TimeUnit.SECONDS)
    }

    override fun onDisconnect(client: ClientHead) {
        val e = ackEntries.remove(client.sessionId) ?: return

        val indexes = e.getAckIndexes()
        for (index in indexes) {
            e.getAckCallback(index)?.onTimeout()
            val key = AckSchedulerKey(Type.ACK_TIMEOUT, client.sessionId, index)
            scheduler.cancel(key)
        }
    }

}
