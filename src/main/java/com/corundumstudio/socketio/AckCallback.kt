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

import kotlin.reflect.KClass

/**
 * Base ack callback class.
 *
 * Notifies about acknowledgement received from client
 * via {@link #onSuccess} callback method.
 *
 * By default it may wait acknowledgement from client
 * while {@link SocketIOClient} is alive. Timeout can be
 * defined {@link #timeout} as constructor argument.
 *
 * This object is NOT actual anymore if {@link #onSuccess} or
 * {@link #onTimeout} was executed.
 *
 * @param <T> - any serializable type
 *
 * @see VoidAckCallback
 * @see MultiTypeAckCallback
 *
 * Creates AckCallback with timeout
 * @param resultClass - result class
 * @param timeout - callback timeout in seconds
 */
abstract class AckCallback<T>(
    /**
     * Returns class of argument in {@link #onSuccess} method
     * @return - result class
     */
    val resultClass: Class<T>,
    val timeout: Int
) {

    /**
     * Create AckCallback
     * @param resultClass - result class
     */
    constructor(resultClass: Class<T>) : this(resultClass, -1)

    /**
     * Executes only once when acknowledgement received from client.
     * @param result - object sended by client
     */
    abstract fun onSuccess(result: T)

    /**
     * Invoked only once then <code>timeout</code> defined
     */
    open fun onTimeout() {
    }

}