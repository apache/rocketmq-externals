/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.rocketmq.flink;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.Validate;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.util.Preconditions;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.shaded.curator.org.apache.curator.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.flink.common.serialization.KeyValueDeserializationSchema;
import org.apache.rocketmq.flink.common.util.MetricUtils;
import org.apache.rocketmq.flink.common.util.RetryUtil;
import org.apache.rocketmq.flink.common.util.RocketMQUtils;
import org.apache.rocketmq.flink.common.watermark.WaterMarkForAll;
import org.apache.rocketmq.flink.common.watermark.WaterMarkPerQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.rocketmq.flink.RocketMQConfig.*;
import static org.apache.rocketmq.flink.common.util.RocketMQUtils.getInteger;
import static org.apache.rocketmq.flink.common.util.RocketMQUtils.getLong;

/**
 * The RocketMQSource is based on RocketMQ pull consumer mode, and provides exactly once reliability guarantees when
 * checkpoints are enabled. Otherwise, the source doesn't provide any reliability guarantees.
 */
public class RocketMQSource<OUT> extends RichParallelSourceFunction<OUT>
        implements CheckpointedFunction, CheckpointListener, ResultTypeQueryable<OUT> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RocketMQSource.class);
    private static final String OFFSETS_STATE_NAME = "topic-partition-offset-states";
    private RunningChecker runningChecker;
    private transient DefaultMQPullConsumer consumer;
    private KeyValueDeserializationSchema<OUT> schema;
    private transient ListState<Tuple2<MessageQueue, Long>> unionOffsetStates;
    private Map<MessageQueue, Long> offsetTable;
    private Map<MessageQueue, Long> restoredOffsets;
    private List<MessageQueue> messageQueues;
    private ExecutorService executor;

    // watermark in source
    private WaterMarkPerQueue waterMarkPerQueue;
    private WaterMarkForAll waterMarkForAll;

    private ScheduledExecutorService timer;
    /**
     * Data for pending but uncommitted offsets.
     */
    private LinkedMap pendingOffsetsToCommit;
    private Properties props;
    private String topic;
    private String group;
    private transient volatile boolean restored;
    private transient boolean enableCheckpoint;
    private volatile Object checkPointLock;

    private Meter tpsMetric;

    public RocketMQSource(KeyValueDeserializationSchema<OUT> schema, Properties props) {
        this.schema = schema;
        this.props = props;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        log.debug("source open....");
        Validate.notEmpty(props, "Consumer properties can not be empty");

        this.topic = props.getProperty(RocketMQConfig.CONSUMER_TOPIC);
        this.group = props.getProperty(RocketMQConfig.CONSUMER_GROUP);

        Validate.notEmpty(topic, "Consumer topic can not be empty");
        Validate.notEmpty(group, "Consumer group can not be empty");

        this.enableCheckpoint = ((StreamingRuntimeContext) getRuntimeContext()).isCheckpointingEnabled();

        if (offsetTable == null) {
            offsetTable = new ConcurrentHashMap<>();
        }
        if (restoredOffsets == null) {
            restoredOffsets = new ConcurrentHashMap<>();
        }

        //use restoredOffsets to init offset table.
        initOffsetTableFromRestoredOffsets();

        if (pendingOffsetsToCommit == null) {
            pendingOffsetsToCommit = new LinkedMap();
        }
        if (checkPointLock == null) {
            checkPointLock = new ReentrantLock();
        }
        if (waterMarkPerQueue == null) {
            waterMarkPerQueue = new WaterMarkPerQueue(5000);
        }
        if (waterMarkForAll == null) {
            waterMarkForAll = new WaterMarkForAll(5000);
        }
        if (timer == null) {
            timer = Executors.newSingleThreadScheduledExecutor();
        }

        runningChecker = new RunningChecker();
        runningChecker.setRunning(true);

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true).setNameFormat("rmq-pull-thread-%d").build();
        executor = Executors.newCachedThreadPool(threadFactory);

        int indexOfThisSubTask = getRuntimeContext().getIndexOfThisSubtask();
        consumer = new DefaultMQPullConsumer(group, RocketMQConfig.buildAclRPCHook(props));
        RocketMQConfig.buildConsumerConfigs(props, consumer);

        // set unique instance name, avoid exception: https://help.aliyun.com/document_detail/29646.html
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String instanceName = RocketMQUtils.getInstanceName(runtimeName, topic, group,
                String.valueOf(indexOfThisSubTask), String.valueOf(System.nanoTime()));
        consumer.setInstanceName(instanceName);
        consumer.start();

        Counter outputCounter = getRuntimeContext().getMetricGroup()
                .counter(MetricUtils.METRICS_TPS + "_counter", new SimpleCounter());
        tpsMetric = getRuntimeContext().getMetricGroup()
                .meter(MetricUtils.METRICS_TPS, new MeterView(outputCounter, 60));
    }

    @Override
    public void run(SourceContext context) throws Exception {
        String tag = props.getProperty(RocketMQConfig.CONSUMER_TAG, RocketMQConfig.DEFAULT_CONSUMER_TAG);
        int pullBatchSize = getInteger(props, CONSUMER_BATCH_SIZE, DEFAULT_CONSUMER_BATCH_SIZE);

        final RuntimeContext ctx = getRuntimeContext();
        // The lock that guarantees that record emission and state updates are atomic,
        // from the view of taking a checkpoint.
        int taskNumber = ctx.getNumberOfParallelSubtasks();
        int taskIndex = ctx.getIndexOfThisSubtask();
        log.info("Source run, NumberOfTotalTask={}, IndexOfThisSubTask={}", taskNumber, taskIndex);


        timer.scheduleAtFixedRate(() -> {
            // context.emitWatermark(waterMarkPerQueue.getCurrentWatermark());
            context.emitWatermark(waterMarkForAll.getCurrentWatermark());
        }, 5, 5, TimeUnit.SECONDS);

        Collection<MessageQueue> totalQueues = consumer.fetchSubscribeMessageQueues(topic);
        messageQueues = RocketMQUtils.allocate(totalQueues, taskNumber, ctx.getIndexOfThisSubtask());
        for (MessageQueue mq : messageQueues) {
            this.executor.execute(() -> {
                RetryUtil.call(() -> {
                    while (runningChecker.isRunning()) {
                        try {
                            long offset = getMessageQueueOffset(mq);
                            PullResult pullResult = consumer.pullBlockIfNotFound(mq, tag, offset, pullBatchSize);

                            boolean found = false;
                            switch (pullResult.getPullStatus()) {
                                case FOUND:
                                    List<MessageExt> messages = pullResult.getMsgFoundList();
                                    for (MessageExt msg : messages) {
                                        byte[] key = msg.getKeys() != null ? msg.getKeys().getBytes(StandardCharsets.UTF_8) : null;
                                        byte[] value = msg.getBody();
                                        OUT data = schema.deserializeKeyAndValue(key, value);

                                        // output and state update are atomic
                                        synchronized (checkPointLock) {
                                            log.debug(msg.getMsgId() + "_" + msg.getBrokerName() + " " + msg.getQueueId() + " " + msg.getQueueOffset());
                                            context.collectWithTimestamp(data, msg.getBornTimestamp());

                                            // update max eventTime per queue
                                            // waterMarkPerQueue.extractTimestamp(mq, msg.getBornTimestamp());
                                            waterMarkForAll.extractTimestamp(msg.getBornTimestamp());
                                            tpsMetric.markEvent();
                                        }
                                    }
                                    found = true;
                                    break;
                                case NO_MATCHED_MSG:
                                    log.debug("No matched message after offset {} for queue {}", offset, mq);
                                    break;
                                case NO_NEW_MSG:
                                    log.debug("No new message after offset {} for queue {}", offset, mq);
                                    break;
                                case OFFSET_ILLEGAL:
                                    log.warn("Offset {} is illegal for queue {}", offset, mq);
                                    break;
                                default:
                                    break;
                            }

                            synchronized (checkPointLock) {
                                updateMessageQueueOffset(mq, pullResult.getNextBeginOffset());
                            }

                            if (!found) {
                                RetryUtil.waitForMs(RocketMQConfig.DEFAULT_CONSUMER_DELAY_WHEN_MESSAGE_NOT_FOUND);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return true;
                }, "RuntimeException");
            });
        }

        awaitTermination();
    }

    private void awaitTermination() throws InterruptedException {
        while (runningChecker.isRunning()) {
            Thread.sleep(50);
        }
    }

    private long getMessageQueueOffset(MessageQueue mq) throws MQClientException {
        Long offset = offsetTable.get(mq);
        // restoredOffsets(unionOffsetStates) is the restored global union state;
        // should only snapshot mqs that actually belong to us
        if (offset == null) {
            // fetchConsumeOffset from broker
            offset = consumer.fetchConsumeOffset(mq, false);
            if (!restored || offset < 0) {
                String initialOffset = props.getProperty(RocketMQConfig.CONSUMER_OFFSET_RESET_TO, CONSUMER_OFFSET_LATEST);
                switch (initialOffset) {
                    case CONSUMER_OFFSET_EARLIEST:
                        offset = consumer.minOffset(mq);
                        break;
                    case CONSUMER_OFFSET_LATEST:
                        offset = consumer.maxOffset(mq);
                        break;
                    case CONSUMER_OFFSET_TIMESTAMP:
                        offset = consumer.searchOffset(mq, getLong(props,
                                RocketMQConfig.CONSUMER_OFFSET_FROM_TIMESTAMP, System.currentTimeMillis()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown value for CONSUMER_OFFSET_RESET_TO.");
                }
            }
        }
        offsetTable.put(mq, offset);
        return offsetTable.get(mq);
    }

    private void updateMessageQueueOffset(MessageQueue mq, long offset) throws MQClientException {
        offsetTable.put(mq, offset);
        if (!enableCheckpoint) {
            consumer.updateConsumeOffset(mq, offset);
        }
    }

    @Override
    public void cancel() {
        log.debug("cancel ...");
        runningChecker.setRunning(false);

        if (consumer != null) {
            consumer.shutdown();
        }

        if (offsetTable != null) {
            offsetTable.clear();
        }
        if (restoredOffsets != null) {
            restoredOffsets.clear();
        }
        if (pendingOffsetsToCommit != null) {
            pendingOffsetsToCommit.clear();
        }
    }

    @Override
    public void close() throws Exception {
        log.debug("close ...");
        // pretty much the same logic as cancelling
        try {
            cancel();
        } finally {
            super.close();
        }
    }

    public void initOffsetTableFromRestoredOffsets() {
        Preconditions.checkNotNull(restoredOffsets, "restoredOffsets can't be null");
        restoredOffsets.forEach((mq, offset) -> {
            if (!offsetTable.containsKey(mq) || offsetTable.get(mq) < offset) {
                offsetTable.put(mq, offset);
            }
        });
        log.info("init offset table from restoredOffsets successful.", offsetTable);
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        // called when a snapshot for a checkpoint is requested
        log.info("Snapshotting state {} ...", context.getCheckpointId());
        if (!runningChecker.isRunning()) {
            log.info("snapshotState() called on closed source; returning null.");
            return;
        }

        // Discovery topic Route change when snapshot
        RetryUtil.call(() -> {
            Collection<MessageQueue> totalQueues = consumer.fetchSubscribeMessageQueues(topic);
            int taskNumber = getRuntimeContext().getNumberOfParallelSubtasks();
            int taskIndex = getRuntimeContext().getIndexOfThisSubtask();
            List<MessageQueue> newQueues = RocketMQUtils.allocate(totalQueues, taskNumber, taskIndex);
            Collections.sort(newQueues);
            log.debug(taskIndex + " Topic route is same.");
            if (!messageQueues.equals(newQueues)) {
                throw new RuntimeException();
            }
            return true;
        }, "RuntimeException due to topic route changed");

        unionOffsetStates.clear();
        HashMap<MessageQueue, Long> currentOffsets = new HashMap<>(offsetTable.size());
        for (Map.Entry<MessageQueue, Long> entry : offsetTable.entrySet()) {
            unionOffsetStates.add(Tuple2.of(entry.getKey(), entry.getValue()));
            currentOffsets.put(entry.getKey(), entry.getValue());
        }
        pendingOffsetsToCommit.put(context.getCheckpointId(), currentOffsets);
        log.info("Snapshotted state, last processed offsets: {}, checkpoint id: {}, timestamp: {}",
                offsetTable, context.getCheckpointId(), context.getCheckpointTimestamp());
    }

    /**
     * called every time the user-defined function is initialized,
     * be that when the function is first initialized or be that
     * when the function is actually recovering from an earlier checkpoint.
     * Given this, initializeState() is not only the place where different types of state are initialized,
     * but also where state recovery logic is included.
     */
    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        log.info("initialize State ...");

        this.unionOffsetStates = context.getOperatorStateStore().getUnionListState(new ListStateDescriptor<>(
                OFFSETS_STATE_NAME, TypeInformation.of(new TypeHint<Tuple2<MessageQueue, Long>>() {
        })));
        this.restored = context.isRestored();

        if (restored) {
            if (restoredOffsets == null) {
                restoredOffsets = new ConcurrentHashMap<>();
            }
            for (Tuple2<MessageQueue, Long> mqOffsets : unionOffsetStates.get()) {
                if (!restoredOffsets.containsKey(mqOffsets.f0) || restoredOffsets.get(mqOffsets.f0) < mqOffsets.f1) {
                    restoredOffsets.put(mqOffsets.f0, mqOffsets.f1);
                }
            }
            log.info("Setting restore state in the consumer. Using the following offsets: {}", restoredOffsets);
        } else {
            log.info("No restore state for the consumer.");
        }
    }

    @Override
    public TypeInformation getProducedType() {
        return schema.getProducedType();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
        // callback when checkpoint complete
        if (!runningChecker.isRunning()) {
            log.info("notifyCheckpointComplete() called on closed source; returning null.");
            return;
        }

        final int posInMap = pendingOffsetsToCommit.indexOf(checkpointId);
        if (posInMap == -1) {
            log.warn("Received confirmation for unknown checkpoint id {}", checkpointId);
            return;
        }

        Map<MessageQueue, Long> offsets = (Map<MessageQueue, Long>) pendingOffsetsToCommit.remove(posInMap);

        // remove older checkpoints in map
        for (int i = 0; i < posInMap; i++) {
            pendingOffsetsToCommit.remove(0);
        }

        if (offsets == null || offsets.size() == 0) {
            log.debug("Checkpoint state was empty.");
            return;
        }

        for (Map.Entry<MessageQueue, Long> entry : offsets.entrySet()) {
            consumer.updateConsumeOffset(entry.getKey(), entry.getValue());
        }
    }
}
