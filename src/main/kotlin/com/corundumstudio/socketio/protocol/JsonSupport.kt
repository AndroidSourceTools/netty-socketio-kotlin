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

import com.corundumstudio.socketio.AckCallback
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream

import java.io.IOException

/**
 * JSON infrastructure interface.
 * Allows to implement custom realizations
 * to JSON support operations.
 *
 */
interface JsonSupport {

    @Throws(IOException::class)
    fun readAckArgs(src: ByteBufInputStream, callback: AckCallback<*>) : AckArgs?

    @Throws(IOException::class)
    fun <T> readValue(namespaceName: String, src: ByteBufInputStream, valueType: Class<T>) : T?

    @Throws(IOException::class)
    fun writeValue(out: ByteBufOutputStream, value: Any)

    fun addEventMapping(namespaceName: String, eventName: String, vararg eventClass: Class<*>)

    fun removeEventMapping(namespaceName: String, eventName: String)

    fun getArrays() : MutableList<ByteArray?>?

}