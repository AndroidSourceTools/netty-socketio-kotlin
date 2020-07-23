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
package com.corundumstudio.socketio.protocol

import com.corundumstudio.socketio.namespace.Namespace
import io.netty.buffer.ByteBuf
import java.io.Serializable

class Packet constructor(
    private val type: PacketType
) : Serializable {

    companion object {
        private const val serialVersionUID = 4560159536486711426L
    }

    private var subType: PacketType? = null
    private var ackId: Long? = null
    private var name: String? = null
    private var nsp: String = Namespace.DEFAULT_NAME
    private var data: Any? = null

    private var dataSource: ByteBuf? = null
    private var attachmentsCount = 0
    private var attachments = arrayListOf<ByteBuf>()

    fun getSubType() = subType

    fun setSubType(subType: PacketType?) {
        this.subType = subType
    }

    fun getType() = type

    fun setData(data: Any?) {
        this.data = data
    }

    /**
     * Get packet data
     *
     * @param <T> the type data
     *
     * <pre>
     * @return <b>json object</b> for PacketType.JSON type
     * <b>message</b> for PacketType.MESSAGE type
     * </pre>
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData() = data as T

    /**
     * Creates a copy of #{@link Packet} with new namespace set
     * if it differs from current namespace.
     * Otherwise, returns original object unchanged
     */
    fun withNsp(namespace: String) : Packet {
        return if (this.nsp.equals(namespace, true)) {
            this
        } else {
            val newPacket = Packet(type)
            newPacket.setAckId(ackId)
            newPacket.setData(data)
            newPacket.setDataSource(dataSource)
            newPacket.setName(name)
            newPacket.setSubType(subType)
            newPacket.setNsp(namespace)
            newPacket.attachments = this.attachments
            newPacket.attachmentsCount = this.attachmentsCount
            newPacket
        }
    }

    fun setNsp(endpoint: String) {
        this.nsp = endpoint
    }

    fun getNsp() = nsp

    fun getName() = name

    fun setName(name: String?) {
        this.name = name
    }

    fun getAckId() = ackId

    fun setAckId(ackId: Long?) {
        this.ackId = ackId
    }

    fun isAckRequested() = getAckId() != null

    fun initAttachments(attachmentsCount: Int) {
        this.attachmentsCount = attachmentsCount
        this.attachments = ArrayList(attachmentsCount)
    }
    fun addAttachment(attachment: ByteBuf) {
        if (this.attachments.size < attachmentsCount) {
            this.attachments.add(attachment)
        }
    }
    fun getAttachments() = attachments

    fun hasAttachments() = attachmentsCount != 0

    fun isAttachmentsLoaded() = attachments.size == attachmentsCount

    fun getDataSource() = dataSource

    fun setDataSource(dataSource: ByteBuf?) {
        this.dataSource = dataSource
    }

    override fun toString() = "Packet [type=$type, ackId=$ackId]"

}
