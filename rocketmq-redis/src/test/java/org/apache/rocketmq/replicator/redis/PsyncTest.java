/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis;

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandListener;
import org.apache.rocketmq.replicator.redis.cmd.CommandName;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetCommand;
import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class PsyncTest {

    @Test
    public void psync() throws IOException {

        final Configuration configuration = Configuration.defaultSetting().
                setAuthPassword("test").
                setConnectionTimeout(3000).
                setReadTimeout(3000).
                setBufferSize(64).
                setAsyncCachedBytes(0).
                setHeartBeatPeriod(200).
                setReceiveBufferSize(0).
                setSendBufferSize(0).
                setDiscardRdbEvent(true).
                setRetryTimeInterval(1000);
        System.out.println(configuration);
        Replicator replicator = new TestRedisSocketReplicator("127.0.0.1", 6380, configuration);
        final AtomicBoolean flag = new AtomicBoolean(false);
        final Set<AuxField> set = new LinkedHashSet<>();
        replicator.addAuxFieldListener(new AuxFieldListener() {
            @Override
            public void handle(Replicator replicator, AuxField auxField) {
                set.add(auxField);
            }
        });
        final AtomicInteger acc = new AtomicInteger();
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                if (flag.compareAndSet(false, true)) {
                    Thread thread = new Thread(new JRun());
                    thread.setDaemon(true);
                    thread.start();
                    replicator.removeCommandParser(CommandName.name("PING"));
                }
                if (command instanceof SetCommand && ((SetCommand) command).getKey().startsWith("psync")) {
                    SetCommand setCommand = (SetCommand) command;
                    int num = Integer.parseInt(setCommand.getKey().split(" ")[1]);
                    acc.incrementAndGet();
                    if (acc.get() == 200) {
                        System.out.println("close for psync");
                        //close current process port;
                        //that will auto trigger psync command
                        close(replicator);
                    }
                    if (acc.get() == 980) {
                        configuration.setVerbose(true);
                    }
                    if (acc.get() == 1000) {
                        try {
                            replicator.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                assertEquals(1000, acc.get());
                for (AuxField auxField : set) {
                    System.out.println(auxField.getAuxKey() + "=" + auxField.getAuxValue());
                }
            }
        });
        replicator.open();
    }

    private static void close(Replicator replicator) {
        try {
            ((TestRedisSocketReplicator) replicator).getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ((TestRedisSocketReplicator) replicator).getInputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ((TestRedisSocketReplicator) replicator).getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class JRun implements Runnable {

        @Override
        public void run() {
            System.out.println("start jedis insert");
            Jedis jedis = new Jedis("127.0.0.1", 6380);
            jedis.auth("test");
            for (int i = 0; i < 1000; i++) {
                jedis.set("psync " + i, "psync" + i);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            jedis.close();
            System.out.println("stop jedis insert");
        }
    }

    private static class TestRedisSocketReplicator extends RedisSocketReplicator {

        public TestRedisSocketReplicator(String host, int port, Configuration configuration) {
            super(host, port, configuration);
        }

        public Socket getSocket(){
            return super.socket;
        }

        public InputStream getInputStream(){
            return super.inputStream;
        }

        public OutputStream getOutputStream(){
            return super.outputStream;
        }
    }

}
