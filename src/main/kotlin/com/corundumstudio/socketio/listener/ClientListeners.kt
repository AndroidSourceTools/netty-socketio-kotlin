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
package com.corundumstudio.socketio.listener

interface ClientListeners {

    fun addMultiTypeEventListener(eventName: String, listener: MultiTypeEventListener, vararg eventClass: Class<*>)

    fun <T> addEventListener(eventName: String, eventClass: Class<T>, listener: DataListener<T>)

    fun addEventInterceptor(eventInterceptor: EventInterceptor)

    fun addDisconnectListener(listener: DisconnectListener)

    fun addConnectListener(listener: ConnectListener )

    fun addPingListener(listener: PingListener)

    fun addListeners(listeners: Any)

    fun addListeners(listeners: Any, listenersClass: Class<*>)

    fun removeAllListeners(eventName: String)

}
