/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink;

import org.apache.commons.lang.Validate;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.flink.common.util.MetricUtils;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * The RocketMQSink provides at-least-once reliability guarantees when
 * checkpoints are enabled and batchFlushOnCheckpoint(true) is set.
 * Otherwise, the sink reliability guarantees depends on rocketmq producer's retry policy.
 */
public class RocketMQSink<IN> extends RichSinkFunction<IN> implements CheckpointedFunction {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(RocketMQSink.class);

    private transient DefaultMQProducer producer;
    private boolean async; // false by default

    private Properties props;

    private boolean batchFlushOnCheckpoint; // false by default
    private int batchSize = 32;
    private List<Message> batchList;

    private Meter sinkInTps;
    private Meter outTps;
    private Meter outBps;
    private MetricUtils.LatencyGauge latencyGauge;

    public RocketMQSink(Properties props) {
        this.props = props;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        Validate.notEmpty(props, "Producer properties can not be empty");

        // with authentication hook
        producer = new DefaultMQProducer(RocketMQConfig.buildAclRPCHook(props));
        producer.setInstanceName(getRuntimeContext().getIndexOfThisSubtask() + "_" + UUID.randomUUID());

        RocketMQConfig.buildProducerConfigs(props, producer);

        batchList = new LinkedList<>();

        if (batchFlushOnCheckpoint && !((StreamingRuntimeContext) getRuntimeContext()).isCheckpointingEnabled()) {
            LOG.info("Flushing on checkpoint is enabled, but checkpointing is not enabled. Disabling flushing.");
            batchFlushOnCheckpoint = false;
        }

        try {
            producer.start();
        } catch (MQClientException e) {
            LOG.error("Flink sink init failed, due to the producer cannot be initialized.");
            throw new RuntimeException(e);
        }
        sinkInTps = MetricUtils.registerSinkInTps(getRuntimeContext());
        outTps = MetricUtils.registerOutTps(getRuntimeContext());
        outBps = MetricUtils.registerOutBps(getRuntimeContext());
        latencyGauge = MetricUtils.registerOutLatency(getRuntimeContext());
    }

    @Override
    public void invoke(IN input, Context context) throws Exception {
        sinkInTps.markEvent();

        Message msg = (Message) input;
        if (batchFlushOnCheckpoint) {
            batchList.add(msg);
            if (batchList.size() >= batchSize) {
                flushSync();
            }
            return;
        }

        long timeStartWriting = System.currentTimeMillis();
        if (async) {
            try {
                producer.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        LOG.debug("Async send message success! result: {}", sendResult);
                        long end = System.currentTimeMillis();
                        latencyGauge.report(end - timeStartWriting, 1);
                        outTps.markEvent();
                        outBps.markEvent(msg.getBody().length);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        if (throwable != null) {
                            LOG.error("Async send message failure!", throwable);
                        }
                    }
                });
            } catch (Exception e) {
                LOG.error("Async send message failure!", e);
            }
        } else {
            try {
                SendResult result = producer.send(msg);
                LOG.debug("Sync send message result: {}", result);
                if (result.getSendStatus() != SendStatus.SEND_OK) {
                    throw new RemotingException(result.toString());
                }
                long end = System.currentTimeMillis();
                latencyGauge.report(end - timeStartWriting, 1);
                outTps.markEvent();
                outBps.markEvent(msg.getBody().length);
            } catch (Exception e) {
                LOG.error("Sync send message exception: ", e);
                throw e;
            }
        }
    }

    public RocketMQSink<IN> withAsync(boolean async) {
        this.async = async;
        return this;
    }

    public RocketMQSink<IN> withBatchFlushOnCheckpoint(boolean batchFlushOnCheckpoint) {
        this.batchFlushOnCheckpoint = batchFlushOnCheckpoint;
        return this;
    }

    public RocketMQSink<IN> withBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    @Override
    public void close() {
        if (producer != null) {
            try {
                flushSync();
            } catch (Exception e) {
                LOG.error("FlushSync failure!", e);
            }
            // make sure producer can be shutdown, thus current producerGroup will be unregistered
            producer.shutdown();
        }
    }

    private void flushSync() throws Exception {
        if (batchFlushOnCheckpoint) {
            synchronized (batchList) {
                if (batchList.size() > 0) {
                    producer.send(batchList);
                    batchList.clear();
                }
            }
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        flushSync();
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        // Nothing to do
    }
}
