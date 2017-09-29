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

package org.apache.rocketmq.redis.replicator;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.rocketmq.redis.replicator.cmd.OffsetHandler;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.net.RedisSocketFactory;
import org.apache.rocketmq.redis.replicator.cmd.BulkReplyHandler;
import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandName;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.ReplyParser;
import org.apache.rocketmq.redis.replicator.io.AsyncBufferedInputStream;
import org.apache.rocketmq.redis.replicator.io.RateLimitInputStream;
import org.apache.rocketmq.redis.replicator.io.RedisOutputStream;
import org.apache.rocketmq.redis.replicator.rdb.RdbParser;
import org.apache.rocketmq.redis.replicator.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.rocketmq.redis.replicator.RedisConstants.DOLLAR;
import static org.apache.rocketmq.redis.replicator.RedisConstants.STAR;
import static org.apache.rocketmq.redis.replicator.Status.CONNECTED;
import static org.apache.rocketmq.redis.replicator.Status.CONNECTING;
import static org.apache.rocketmq.redis.replicator.Status.DISCONNECTED;
import static org.apache.rocketmq.redis.replicator.Status.DISCONNECTING;

public class RedisSocketReplicator extends AbstractReplicator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisSocketReplicator.class);

    protected final int port;
    protected Timer heartbeat;
    protected final String host;
    protected volatile Socket socket;
    protected volatile ReplyParser replyParser;
    protected volatile RedisOutputStream outputStream;
    protected final RedisSocketFactory socketFactory;

    public RedisSocketReplicator(String host, int port, Configuration configuration) {
        Objects.requireNonNull(host);
        if (port <= 0 || port > 65535)
            throw new IllegalArgumentException("illegal argument port: " + port);
        Objects.requireNonNull(configuration);
        this.host = host;
        this.port = port;
        this.configuration = configuration;
        this.socketFactory = new RedisSocketFactory(configuration);
        builtInCommandParserRegister();
        if (configuration.isUseDefaultExceptionListener())
            addExceptionListener(new DefaultExceptionListener());
    }

    /**
     * PSYNC
     *
     * @throws IOException when read timeout or connect timeout
     */
    @Override
    public void open() throws IOException {
        try {
            doOpen();
        } finally {
            doClose();
            doCloseListener(this);
        }
    }

    /**
     * PSYNC
     *
     * @throws IOException when read timeout or connect timeout
     */
    protected void doOpen() throws IOException {
        IOException exception = null;
        for (int i = 0; i < configuration.getRetries() || configuration.getRetries() <= 0; i++) {
            exception = null;
            try {
                establishConnection();
                //reset retries
                i = 0;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("PSYNC " + configuration.getReplId() + " " + String.valueOf(configuration.getReplOffset()));
                }
                send("PSYNC".getBytes(), configuration.getReplId().getBytes(), String.valueOf(configuration.getReplOffset()).getBytes());
                final String reply = new String((byte[]) reply(), UTF_8);

                SyncMode syncMode = trySync(reply);
                if (syncMode == SyncMode.PSYNC && getStatus() == CONNECTED) {
                    heartbeat();
                } else if (syncMode == SyncMode.SYNC_LATER && getStatus() == CONNECTED) {
                    i = 0;
                    doClose();
                    try {
                        Thread.sleep(configuration.getRetryTimeInterval());
                    } catch (InterruptedException interrupt) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                final long[] offset = new long[1];
                while (getStatus() == CONNECTED) {
                    Object obj = replyParser.parse(new OffsetHandler() {
                        @Override
                        public void handle(long len) {
                            offset[0] = len;
                        }
                    });
                    //command
                    if (obj instanceof Object[]) {
                        if (configuration.isVerbose() && LOGGER.isDebugEnabled())
                            LOGGER.debug(Arrays.deepToString((Object[]) obj));
                        Object[] command = (Object[]) obj;
                        CommandName cmdName = CommandName.name(new String((byte[]) command[0], UTF_8));
                        final CommandParser<? extends Command> operations;
                        if ((operations = commands.get(cmdName)) == null) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn("command [" + cmdName + "] not register. raw command:[" + Arrays.deepToString(command) + "]");
                            }
                            continue;
                        }
                        Command parsedCommand = operations.parse(command);
                        this.submitEvent(parsedCommand);
                    } else {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("redis reply:" + obj);
                        }
                    }
                    // add offset after event consumed. after that reset offset to 0L.
                    configuration.addOffset(offset[0]);
                    offset[0] = 0L;
                }
                //getStatus() != CONNECTED
                exception = null;
                break;
            } catch (IOException | UncheckedIOException e) {
                //close socket manual
                if (getStatus() != CONNECTED) {
                    exception = null;
                    break;
                }
                if (e instanceof UncheckedIOException) {
                    exception = ((UncheckedIOException) e).getCause();
                } else {
                    exception = (IOException) e;
                }
                LOGGER.error("[redis-replicator] socket error", exception);
                doClose();
                //retry psync in next loop.
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("reconnect to redis-server. retry times:" + (i + 1));
                }
                try {
                    Thread.sleep(configuration.getRetryTimeInterval());
                } catch (InterruptedException interrupt) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (exception != null) throw exception;
    }

    protected SyncMode trySync(final String reply) throws IOException {
        LOGGER.info(reply);
        if (reply.startsWith("FULLRESYNC")) {
            //sync rdb dump file
            parseDump(this);
            //after parsed dump file,cache master run id and offset so that next psync.
            String[] ary = reply.split(" ");
            configuration.setReplId(ary[1]);
            configuration.setReplOffset(Long.parseLong(ary[2]));
            return SyncMode.PSYNC;
        } else if (reply.startsWith("CONTINUE")) {
            String[] ary = reply.split(" ");
            //redis-4.0 compatible
            String masterRunId = configuration.getReplId();
            if (ary.length > 1 && masterRunId != null && !masterRunId.equals(ary[1]))
                configuration.setReplId(ary[1]);
            return SyncMode.PSYNC;
        } else if (reply.startsWith("NOMASTERLINK") || reply.startsWith("LOADING")) {
            return SyncMode.SYNC_LATER;
        } else {
            //server don't support psync
            LOGGER.info("SYNC");
            send("SYNC".getBytes());
            parseDump(this);
            return SyncMode.SYNC;
        }
    }

    protected void parseDump(final AbstractReplicator replicator) throws IOException {
        //sync dump
        byte[] rawReply = reply(new BulkReplyHandler() {
            @Override
            public byte[] handle(long len, RedisInputStream in) throws IOException {
                if (LOGGER.isInfoEnabled()) {
                    if (len != -1) {
                        LOGGER.info("RDB dump file size:" + len);
                    } else {
                        LOGGER.info("Disk-less replication.");
                    }
                }
                if (len != -1 && configuration.isDiscardRdbEvent()) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("discard " + len + " bytes");
                    }
                    in.skip(len);
                } else {
                    RdbParser parser = new RdbParser(in, replicator);
                    parser.parse();
                    if (len == -1) in.skip(40, false); // skip 40 bytes delimiter when disk-less replication
                }
                return "OK".getBytes();
            }
        });
        //sync command
        String reply = new String(rawReply, UTF_8);
        if ("OK".equals(reply))
            return;
        throw new IOException("SYNC failed. reason : [" + reply + "]");
    }

    protected void establishConnection() throws IOException {
        connect();
        if (configuration.getAuthPassword() != null)
            auth(configuration.getAuthPassword());
        sendPing();
        sendSlavePort();
        sendSlaveIp();
        sendSlaveCapa("eof");
        sendSlaveCapa("psync2");
    }

    protected void auth(String password) throws IOException {
        if (password != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("AUTH " + password);
            }
            send("AUTH".getBytes(), password.getBytes());
            final String reply = new String((byte[]) reply(), UTF_8);
            LOGGER.info(reply);
            if ("OK".equals(reply))
                return;
            if (reply.contains("no password")) {
                LOGGER.warn("[AUTH " + password + "] failed. " + reply);
                return;
            }
            throw new AssertionError("[AUTH " + password + "] failed. " + reply);
        }
    }

    protected void sendPing() throws IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("PING");
        }
        send("PING".getBytes());
        final String reply = new String((byte[]) reply(), UTF_8);
        LOGGER.info(reply);
        if ("PONG".equalsIgnoreCase(reply))
            return;
        if (reply.contains("NOAUTH"))
            throw new AssertionError(reply);
        if (reply.contains("operation not permitted"))
            throw new AssertionError("-NOAUTH Authentication required.");
        LOGGER.warn("[PING] failed. " + reply);
    }

    protected void sendSlavePort() throws IOException {
        //REPLCONF listening-prot ${port}
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("REPLCONF listening-port " + socket.getLocalPort());
        }
        send("REPLCONF".getBytes(), "listening-port".getBytes(), String.valueOf(socket.getLocalPort()).getBytes());
        final String reply = new String((byte[]) reply(), UTF_8);
        LOGGER.info(reply);
        if ("OK".equals(reply))
            return;
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[REPLCONF listening-port " + socket.getLocalPort() + "] failed. " + reply);
        }
    }

    protected void sendSlaveIp() throws IOException {
        //REPLCONF ip-address ${address}
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("REPLCONF ip-address " + socket.getLocalAddress().getHostAddress());
        }
        send("REPLCONF".getBytes(), "ip-address".getBytes(), socket.getLocalAddress().getHostAddress().getBytes());
        final String reply = new String((byte[]) reply(), UTF_8);
        LOGGER.info(reply);
        if ("OK".equals(reply))
            return;
        //redis 3.2+
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[REPLCONF ip-address " + socket.getLocalAddress().getHostAddress() + "] failed. " + reply);
        }
    }

    protected void sendSlaveCapa(String cmd) throws IOException {
        //REPLCONF capa eof
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("REPLCONF capa " + cmd);
        }
        send("REPLCONF".getBytes(), "capa".getBytes(), cmd.getBytes());
        final String reply = new String((byte[]) reply(), UTF_8);
        LOGGER.info(reply);
        if ("OK".equals(reply))
            return;
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("[REPLCONF capa " + cmd + "] failed. " + reply);
        }
    }

    protected synchronized void heartbeat() {
        heartbeat = new Timer("heartbeat", true);
        //bug fix. in this point closed by other thread. multi-thread issue
        heartbeat.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    send("REPLCONF".getBytes(), "ACK".getBytes(), String.valueOf(configuration.getReplOffset()).getBytes());
                } catch (IOException e) {
                    //NOP
                }
            }
        }, configuration.getHeartBeatPeriod(), configuration.getHeartBeatPeriod());
        LOGGER.info("heartbeat thread started.");
    }

    protected void send(byte[] command) throws IOException {
        send(command, new byte[0][]);
    }

    protected void send(byte[] command, final byte[]... args) throws IOException {
        outputStream.write(STAR);
        outputStream.write(String.valueOf(args.length + 1).getBytes());
        outputStream.writeCrLf();
        outputStream.write(DOLLAR);
        outputStream.write(String.valueOf(command.length).getBytes());
        outputStream.writeCrLf();
        outputStream.write(command);
        outputStream.writeCrLf();
        for (final byte[] arg : args) {
            outputStream.write(DOLLAR);
            outputStream.write(String.valueOf(arg.length).getBytes());
            outputStream.writeCrLf();
            outputStream.write(arg);
            outputStream.writeCrLf();
        }
        outputStream.flush();
    }

    @SuppressWarnings("unchecked")
    protected <T> T reply() throws IOException {
        return (T) replyParser.parse();
    }

    @SuppressWarnings("unchecked")
    protected <T> T reply(BulkReplyHandler handler) throws IOException {
        return (T) replyParser.parse(handler);
    }

    protected void connect() throws IOException {
        if (!connected.compareAndSet(DISCONNECTED, CONNECTING))
            return;
        try {
            socket = socketFactory.createSocket(host, port, configuration.getConnectionTimeout());
            outputStream = new RedisOutputStream(socket.getOutputStream());
            InputStream inputStream = socket.getInputStream();
            if (configuration.getAsyncCachedBytes() > 0) {
                inputStream = new AsyncBufferedInputStream(inputStream, configuration.getAsyncCachedBytes());
            }
            if (configuration.getRateLimit() > 0) {
                inputStream = new RateLimitInputStream(inputStream, configuration.getRateLimit());
            }
            this.inputStream = new RedisInputStream(inputStream, configuration.getBufferSize());
            this.inputStream.setRawByteListeners(this.rawByteListeners);
            replyParser = new ReplyParser(this.inputStream);
        } finally {
            connected.set(CONNECTED);
        }
    }

    @Override
    protected void doClose() throws IOException {
        connected.compareAndSet(CONNECTED, DISCONNECTING);

        try {
            synchronized (this) {
                if (heartbeat != null) {
                    heartbeat.cancel();
                    heartbeat = null;
                    LOGGER.info("heartbeat canceled.");
                }
            }

            try {
                if (inputStream != null) {
                    inputStream.setRawByteListeners(null);
                    inputStream.close();
                }
            } catch (IOException e) {
                //NOP
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                //NOP
            }
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                //NOP
            }
            LOGGER.info("socket closed");
        } finally {
            connected.set(DISCONNECTED);
        }
    }

    protected enum SyncMode { SYNC, PSYNC, SYNC_LATER }

}
