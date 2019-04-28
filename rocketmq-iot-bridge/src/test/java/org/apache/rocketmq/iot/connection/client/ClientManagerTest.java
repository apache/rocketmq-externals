/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
