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

class MultiTypeArgs(private val args: List<Any>) : Iterable<Any> {

    fun isEmpty() = args.isEmpty()

    fun size() = args.size

    fun getArgs() = args

    fun <T> first() = get(0) as T?

    fun <T> second() = get(1) as T?

    /**
     * "index out of bounds"-safe method for getting elements
     *
     * @param <T> type of argument
     * @param index to get
     * @return argument
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(index: Int) : T? {
        if (size() <= index)
            return null
        return args[index] as T
    }

    override fun iterator() = args.iterator()

}
