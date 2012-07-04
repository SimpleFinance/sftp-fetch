package com.simple.sftpfetch.publish;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RabbitClientTest {

    private ConnectionFactory factory = mock(ConnectionFactory.class);
    private Connection connection = mock(Connection.class);
    private Channel channel = mock(Channel.class);
    public static final String CONN = "amqp://username:password@rabbit.example.com:5672/the/vhost";
    public static final String EXCHANGE = "foobar";
    public static final String ROUTING_KEY = "route.this";
    private RabbitConnectionInfo connectionInfo = mock(RabbitConnectionInfo.class);

    @Before
    public void setUp() throws Exception {
        when(connectionInfo.getExchange()).thenReturn(EXCHANGE);
        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
    }

    @Test
    public void shouldDeclareExchnage() throws Exception {
        new RabbitClient(factory, connectionInfo);
        verify(channel).exchangeDeclare(EXCHANGE, RabbitClient.EXCHANGE_TYPE, true);
    }

    @Test
    public void shouldEstablishConnectionTimeout() throws Exception {
        int timeout = 1337;
        when(connectionInfo.getTimeout()).thenReturn(timeout);
        new RabbitClient(factory, connectionInfo);
        verify(factory).setConnectionTimeout(timeout);
    }

    @Test
    public void shouldCreateCorrectProperties() throws Exception {
        RabbitClient client = new RabbitClient(factory, connectionInfo);
        assertEquals("text/plain", client.amqpProperties.getContentType());
        assertEquals(2, (int) client.amqpProperties.getDeliveryMode());
    }

    @Test
    public void shouldPublishUrlAsUTF8() throws Exception {
        RabbitClient client = new RabbitClient(factory, connectionInfo);
        URL url = new URL("http://google.com");
        client.publishURL(ROUTING_KEY, url);
        verify(channel).basicPublish(EXCHANGE, ROUTING_KEY, client.amqpProperties, url.toString().getBytes("UTF8"));
    }
}
