/*
 * Copyright (c) 2012 Simple Finance Technology Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simple.sftpfetch.publish;

import java.util.Properties;

public class RabbitConnectionInfo {
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int DEFAULT_PORT = 5672;
    private final String vhost;
    private String exchange;
    private int timeout;
    private final String username;
    private final String password;
    private String hostname;
    private int port;

    /**
     * Initialize using data from the supplied Properties files, using the following required keys
     *
     * <ul>
     *   <li>rabbit.password</li>
     *   <li>rabbit.hostname</li>
     *   <li>rabbit.vhost</li>
     *   <li>rabbit.exchange</li>
     *   <li>rabbit.username</li>
     * </ul>
     *
     * and the following optional keys
     *
     * <ul>
     *   <li>rabbit.port</li>
     *   <li>rabbit.connection.timeout</li>
     * </ul>
     *
     * @param properties Properties containing the above keys
     */
    public RabbitConnectionInfo(Properties properties) {
        this(properties.getProperty("rabbit.hostname"),
                Integer.valueOf(properties.getProperty("rabbit.port", String.valueOf(DEFAULT_PORT))),
                properties.getProperty("rabbit.vhost"),
                properties.getProperty("rabbit.exchange"),
                Integer.valueOf(properties.getProperty("rabbit.connection.timeout", String.valueOf(DEFAULT_TIMEOUT))),
                properties.getProperty("rabbit.username"),
                properties.getProperty("rabbit.password")
        );
    }

    public RabbitConnectionInfo(String hostname, int port, String vhost, String exchange, int timeout, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.vhost = vhost;
        this.exchange = exchange;
        this.timeout = timeout;
        this.username = username;
        this.password = password;
    }

    public String getExchange() {
        return exchange;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getVhost() {
        return vhost;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
