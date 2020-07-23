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

import io.netty.handler.codec.http.HttpHeaders

import java.io.Serializable
import java.net.InetSocketAddress
import java.util.Date

class HandshakeData : Serializable {

    companion object {
        private const val serialVersionUID = 1196350300161819978L
    }

    private var headers: HttpHeaders? = null
    private var address: InetSocketAddress? = null
    private val time = Date()
    private var local: InetSocketAddress? = null
    private var url: String? = null
    private var urlParams: Map<String, List<String>>? = null
    private var xdomain = false

    constructor(/*needed for correct deserialization*/)

    constructor(headers: HttpHeaders, urlParams: Map<String, List<String>>, address: InetSocketAddress, url: String, xdomain: Boolean) :
        this(headers, urlParams, address, null, url, xdomain)

    constructor(headers: HttpHeaders, urlParams: Map<String, List<String>>, address: InetSocketAddress, local: InetSocketAddress?, url: String, xdomain: Boolean) {
        this.headers = headers
        this.urlParams = urlParams
        this.address = address
        this.local = local
        this.url = url
        this.xdomain = xdomain
    }

    /**
     * Client network address
     * @return network address
     */
    fun getAddress() = address

    /**
     * Connection local address
     *
     * @return local address
     */
    fun getLocal() = local

    /**
     * Http headers sent during first client request
     *
     * @return headers
     */
    fun getHttpHeaders() = headers

    /**
     * Client connection date
     *
     * @return date
     */
    fun getTime() = time

    /**
     * Url used by client during first request
     *
     * @return url
     */
    fun getUrl() = url

    fun isXdomain() = xdomain

    /**
     * Url params stored in url used by client during first request
     *
     * @return map
     */
    fun getUrlParams() = urlParams

    fun getSingleUrlParam(name: String) : String? {
        urlParams?.get(name)?.let { values ->
            if (values.size == 1)
                return values.iterator().next()
        }
        return null
    }

}
