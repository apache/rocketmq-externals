package org.apache.rocketmq.iot.connection.client;

import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.rocketmq.iot.connection.client.impl.ClientManagerImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ClientManagerTest {

    private ClientManager clientManager;
    private Client client;
    private Channel channel;
    private Map<Channel, Client> channel2client;

    @Before
    public void setup() throws IllegalAccessException {
        clientManager = new ClientManagerImpl();
        client = Mockito.mock(Client.class);
        channel = Mockito.mock(Channel.class);
        channel2client = (Map<Channel, Client>) FieldUtils.getDeclaredField(ClientManagerImpl.class, "channel2Client", true).get(clientManager);
        channel2client.put(channel, client);
    }

    @After
    public void teardown() {

    }

    @Test
    public void testGet() {
        /* Normal */
        Assert.assertEquals(client, clientManager.get(channel));

        /* Abnormal */
        Channel fakeChannel = Mockito.mock(Channel.class);
        Assert.assertNull(clientManager.get(fakeChannel));
    }

    @Test
    public void testPut() {
        Client newClient = Mockito.mock(Client.class);
        clientManager.put(channel, newClient);
        Assert.assertEquals(newClient, channel2client.get(channel));
        Assert.assertNotEquals(client, channel2client.get(channel));

        Channel anotherChannel = Mockito.mock(Channel.class);
        Client anotherClient = Mockito.mock(Client.class);
        clientManager.put(anotherChannel, anotherClient);
        Assert.assertEquals(anotherClient, clientManager.get(anotherChannel));

        Channel channelWithNoClient = Mockito.mock(Channel.class);
        Assert.assertNull(clientManager.get(channelWithNoClient));
    }

    @Test
    public void testRemove() {
        clientManager.remove(channel);
        Assert.assertNull(clientManager.get(channel));
    }
}
