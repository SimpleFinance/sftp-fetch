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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A very simple interface for publishing plain-text messages to RabbitMQ using a direct exchange
 */
public class RabbitClient {
    public static final String EXCHANGE_TYPE = "direct";
    public static final String ENCODING = "UTF8";
    public static final String CONTENT_TYPE = "text/plain";

    AMQP.BasicProperties amqpProperties;
    private Channel channel;
    private String exchange;

    /**
     * Initialize the RabbitClient, establish a connection and declare the exchange
     *
     * @param factory the RabbitMQ ConnectionFactory
     * @param connectionInfo a bean containing the necessary connection information
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws URISyntaxException
     * @throws IOException
     */
    public RabbitClient(ConnectionFactory factory, RabbitConnectionInfo connectionInfo) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException {
        factory.setHost(connectionInfo.getHostname());
        factory.setPort(connectionInfo.getPort());
        factory.setUsername(connectionInfo.getUsername());
        factory.setPassword(connectionInfo.getPassword());
        factory.setVirtualHost(connectionInfo.getVhost());
        factory.setConnectionTimeout(connectionInfo.getTimeout());
        Connection conn = factory.newConnection();
        exchange = connectionInfo.getExchange();
        channel = conn.createChannel();
        channel.exchangeDeclare(exchange, EXCHANGE_TYPE, true);
        this.amqpProperties =  new AMQP.BasicProperties.Builder().contentType(CONTENT_TYPE).deliveryMode(2).build();
    }

    /**
     * Publish the given URL as a plain-text message with the given routing key
     *
     * @param routingKey the routing key to use
     * @param url the URL to publish
     *
     * @throws IOException
     */
    public void publishURL(String routingKey, URL url) throws IOException {
        channel.basicPublish(exchange, routingKey, amqpProperties, url.toString().getBytes(ENCODING));
    }
}
