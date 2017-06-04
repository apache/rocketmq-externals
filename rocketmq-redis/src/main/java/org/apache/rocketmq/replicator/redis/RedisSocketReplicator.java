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
 *    2. change commons-logging to slf4j
 *    3. add zookeeper leader selector
 *    4. move configuration.addOffset(offset) after "submit redis event"
 *       to make sure data not loss
 *
 */

package org.apache.rocketmq.replicator.redis;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.rocketmq.replicator.redis.cmd.BulkReplyHandler;
import org.apache.rocketmq.replicator.redis.cmd.ParseResult;
import org.apache.rocketmq.replicator.redis.cmd.ReplyParser;
import org.apache.rocketmq.replicator.redis.conf.Configure;
import org.apache.rocketmq.replicator.redis.io.AsyncBufferedInputStream;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.io.RedisOutputStream;
import org.apache.rocketmq.replicator.redis.net.RedisSocketFactory;
import org.apache.rocketmq.replicator.redis.rdb.RdbParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.DEPLOY_MODEL;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.DEPLOY_MODEL_CLUSTER;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ROOT_DIR;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisSocketReplicator extends AbstractReplicator {

    protected static final Logger logger = LoggerFactory.getLogger(RedisSocketReplicator.class);

    protected Socket socket;
    protected final int port;
    protected Timer heartbeat;
    protected final String host;
    protected ReplyParser replyParser;
    protected RedisOutputStream outputStream;
    protected final RedisSocketFactory socketFactory;
    protected final AtomicBoolean connected = new AtomicBoolean(false);

    private LeaderSelector selector;
    private boolean isDeployCluster = false;

    public RedisSocketReplicator(String host, int port, Configuration configuration) {
        this.host = host;
        this.port = port;
        this.configuration = configuration;
        this.socketFactory = new RedisSocketFactory(configuration);
        this.isDeployCluster = Configure.get(DEPLOY_MODEL).equals(DEPLOY_MODEL_CLUSTER);

        if (isDeployCluster) {
            initLeaderSelector(host, port);
        }

        builtInCommandParserRegister();
        addExceptionListener(new DefaultExceptionListener());
    }

    /**
     * Init zookeeper leader selector to acquire leader.
     *
     * @param host redis master's host
     * @param port redis master's port
     */
    private void initLeaderSelector(String host, int port) {
        CuratorFramework client = ZookeeperClientFactory.get();

        String slaveRootPath = ROOT_DIR + "/" + host + "-" + port;
        this.selector = new LeaderSelector(client, slaveRootPath, new LeaderSelectorListenerAdapter() {
            @Override public void takeLeadership(CuratorFramework client) throws Exception {
                logger.info("Acquire leader successfully, and begin to start");

                try {
                    doOpen();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    close();
                }

            }
        });
    }

    /**
     * PSYNC
     *
     * @throws IOException when read timeout or connect timeout
     */
    @Override
    public void open() throws IOException {
        if (isDeployCluster) {
            this.selector.start();
        }
        else {
            doOpen();
        }

    }

    /**
     * PSYNC
     *
     * @throws IOException when read timeout or connect timeout
     */
    protected void doOpen() throws IOException {
        for (int i = 0; i < configuration.getRetries() || configuration.getRetries() <= 0; i++) {
            try {
                establishConnection();
                //reset retries
                i = 0;

                logger.info("PSYNC " + configuration.getReplId() + " " + String.valueOf(configuration.getReplOffset()));
                send("PSYNC".getBytes(), configuration.getReplId().getBytes(), String.valueOf(configuration.getReplOffset()).getBytes());
                final String reply = (String)(((ParseResult)reply()).getContent());

                SyncMode syncMode = trySync(reply);
                //bug fix.
                if (syncMode == SyncMode.PSYNC && connected.get()) {
                    //heartbeat send REPLCONF ACK ${slave offset}
                    heartbeat();
                }
                else if (syncMode == SyncMode.SYNC_LATER && connected.get()) {
                    //sync later
                    i = 0;
                    close();
                    try {
                        Thread.sleep(configuration.getRetryTimeInterval());
                    }
                    catch (InterruptedException interrupt) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                //sync command
                while (connected.get()) {
                    ParseResult parseResult = replyParser.parse();

                    Object obj = parseResult.getContent();
                    //command
                    submitObject(obj);

                    configuration.addOffset(parseResult.getLen());
                }
                //connected = false
                break;
            }
            catch (/*bug fix*/IOException e) {
                //close socket manual
                if (!connected.get())
                    break;
                logger.error("socket error", e);
                //connect refused,connect timeout,read timeout,connect abort,server disconnect,connection EOFException
                close();
                //retry psync in next loop.
                logger.info("reconnect to redis-server. retry times:" + (i + 1));
                try {
                    Thread.sleep(configuration.getRetryTimeInterval());
                }
                catch (InterruptedException interrupt) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    protected SyncMode trySync(final String reply) throws IOException {
        logger.info(reply);
        if (reply.startsWith("FULLRESYNC")) {
            //sync rdb dump file
            parseDump(this);
            //after parsed dump file,cache master run id and offset so that next psync.
            String[] ary = reply.split(" ");
            configuration.setReplId(ary[1]);
            configuration.setReplOffset(Long.parseLong(ary[2]));
            return SyncMode.PSYNC;
        }
        else if (reply.startsWith("CONTINUE")) {
            String[] ary = reply.split(" ");
            //redis-4.0 compatible
            String masterRunId = configuration.getReplId();
            if (ary.length > 1 && masterRunId != null && !masterRunId.equals(ary[1]))
                configuration.setReplId(ary[1]);
            return SyncMode.PSYNC;
        }
        else if (reply.startsWith("NOMASTERLINK") || reply.startsWith("LOADING")) {
            return SyncMode.SYNC_LATER;
        }
        else {
            //server don't support psync
            logger.info("SYNC");
            send("SYNC".getBytes());
            parseDump(this);
            return SyncMode.SYNC;
        }
    }

    protected void parseDump(final AbstractReplicator replicator) throws IOException {
        //sync dump
        String reply = (String)replyParser.parse(new BulkReplyHandler() {
            @Override
            public String handle(long len, RedisInputStream in) throws IOException {
                logger.info("RDB dump file size:" + len);
                if (configuration.isDiscardRdbEvent()) {
                    logger.info("discard " + len + " bytes");
                    in.skip(len);
                }
                else {
                    RdbParser parser = new RdbParser(in, replicator);
                    parser.parse();
                }
                return "OK";
            }
        }).getContent();
        //sync command
        if (reply.equals("OK"))
            return;
        throw new AssertionError("SYNC failed." + reply);
    }

    protected void establishConnection() throws IOException {
        connect();
        if (configuration.getAuthPassword() != null)
            auth(configuration.getAuthPassword());
        sendSlavePort();
        sendSlaveIp();
        sendSlaveCapa("eof");
        sendSlaveCapa("psync2");
    }

    protected void auth(String password) throws IOException {
        if (password != null) {
            logger.info("AUTH " + password);
            send("AUTH".getBytes(), password.getBytes());
            final String reply = (String)(((ParseResult)reply()).getContent());
            if (reply.equals("OK"))
                return;
            throw new AssertionError("[AUTH " + password + "] failed." + reply);
        }
    }

    protected void sendSlavePort() throws IOException {
        //REPLCONF listening-prot ${port}
        logger.info("REPLCONF listening-port " + socket.getLocalPort());
        send("REPLCONF".getBytes(), "listening-port".getBytes(), String.valueOf(socket.getLocalPort()).getBytes());
        final String reply = (String)(((ParseResult)reply()).getContent());
        if (reply.equals("OK"))
            return;
        logger.warn("[REPLCONF listening-port " + socket.getLocalPort() + "] failed." + reply);
    }

    protected void sendSlaveIp() throws IOException {
        //REPLCONF ip-address ${address}
        logger.info("REPLCONF ip-address " + socket.getLocalAddress().getHostAddress());
        send("REPLCONF".getBytes(), "ip-address".getBytes(), socket.getLocalAddress().getHostAddress().getBytes());
        final String reply = (String)(((ParseResult)reply()).getContent());
        if (reply.equals("OK"))
            return;
        //redis 3.2+
        logger.warn("[REPLCONF ip-address " + socket.getLocalAddress().getHostAddress() + "] failed." + reply);
    }

    protected void sendSlaveCapa(String cmd) throws IOException {
        //REPLCONF capa eof
        logger.info("REPLCONF capa " + cmd);
        send("REPLCONF".getBytes(), "capa".getBytes(), cmd.getBytes());
        final String reply = (String)(((ParseResult)reply()).getContent());
        if (reply.equals("OK"))
            return;
        logger.warn("[REPLCONF capa " + cmd + "] failed." + reply);
    }

    protected synchronized void heartbeat() {
        heartbeat = new Timer("heartbeat", true);
        //bug fix. in this point closed by other thread. multi-thread issue
        heartbeat.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    send("REPLCONF".getBytes(), "ACK".getBytes(), String.valueOf(configuration.getReplOffset()).getBytes());
                }
                catch (IOException e) {
                    //NOP
                }
            }
        }, configuration.getHeartBeatPeriod(), configuration.getHeartBeatPeriod());
        logger.info("heartbeat thread started.");
    }

    protected void send(byte[] command) throws IOException {
        send(command, new byte[0][]);
    }

    protected void send(byte[] command, final byte[]... args) throws IOException {
        outputStream.write(RedisConstants.STAR);
        outputStream.write(String.valueOf(args.length + 1).getBytes());
        outputStream.writeCrLf();
        outputStream.write(RedisConstants.DOLLAR);
        outputStream.write(String.valueOf(command.length).getBytes());
        outputStream.writeCrLf();
        outputStream.write(command);
        outputStream.writeCrLf();
        for (final byte[] arg : args) {
            outputStream.write(RedisConstants.DOLLAR);
            outputStream.write(String.valueOf(arg.length).getBytes());
            outputStream.writeCrLf();
            outputStream.write(arg);
            outputStream.writeCrLf();
        }
        outputStream.flush();
    }

    protected Object reply() throws IOException {
        return replyParser.parse();
    }

    protected Object reply(BulkReplyHandler handler) throws IOException {
        return replyParser.parse(handler);
    }

    protected void connect() throws IOException {
        if (!connected.compareAndSet(false, true))
            return;
        socket = socketFactory.createSocket(host, port, configuration.getConnectionTimeout());
        outputStream = new RedisOutputStream(socket.getOutputStream());
        inputStream = new RedisInputStream(configuration.getAsyncCachedBytes() > 0 ? new AsyncBufferedInputStream(socket.getInputStream(), configuration.getAsyncCachedBytes()) : socket.getInputStream(), configuration.getBufferSize());
        inputStream.addRawByteListener(this);
        replyParser = new ReplyParser(inputStream);
    }

    @Override
    public void close() {
        if (!connected.compareAndSet(true, false))
            return;

        synchronized (this) {
            if (heartbeat != null) {
                heartbeat.cancel();
                heartbeat = null;
                logger.info("heartbeat canceled.");
            }
        }

        try {
            if (inputStream != null) {
                inputStream.removeRawByteListener(this);
                inputStream.close();
            }
        }
        catch (IOException e) {
            //NOP
        }
        try {
            if (outputStream != null)
                outputStream.close();
        }
        catch (IOException e) {
            //NOP
        }
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        }
        catch (IOException e) {
            //NOP
        }

        doCloseListener(this);

        if (isDeployCluster) {
            releaseLeaderSelector();
        }

        logger.info("channel closed");
    }

    public void releaseLeaderSelector() {
        this.selector.close();
    }

    protected enum SyncMode {SYNC, PSYNC, SYNC_LATER}
}
