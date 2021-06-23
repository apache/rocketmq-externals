package org.apache.rocketmq.replicator.offset;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

public class OffsetSyncStore {

    private DefaultMQAdminExt adminExt;
    private TaskConfig taskConfig;

    private DefaultMQPullConsumer consumer;
    private Map<MessageQueue, OffsetSync> syncs;
    private long lastOffset;

    public OffsetSyncStore(DefaultMQAdminExt adminExt,
        TaskConfig taskConfig) {
        this.adminExt = adminExt;
        this.taskConfig = taskConfig;
        this.syncs = new HashMap<MessageQueue, OffsetSync>();
        this.consumer = new DefaultMQPullConsumer();
    }

    public long convertTargetOffset(MessageQueue mq, long srcOffset) {
        OffsetSync offsetSync = latestOffsetSync(mq);
        if (offsetSync.getSrcOffset() > srcOffset) {
            return -1;
        }
        long delta = srcOffset - offsetSync.getSrcOffset();
        return offsetSync.getTargtOffset() + delta;
    }

    private boolean sync(
        Duration pullTimeout) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        TopicRouteData route = adminExt.examineTopicRouteInfo(taskConfig.getOffsetSyncTopic());
        String brokerName = route.getQueueDatas().get(0).getBrokerName();
        MessageQueue mq = new MessageQueue(taskConfig.getOffsetSyncTopic(), brokerName, 0);

        PullResult pr = consumer.pull(mq, "", lastOffset, 0, pullTimeout.getNano() / Duration.ofMillis(1).getNano());
        if (pr.getPullStatus() != PullStatus.FOUND) {
            return false;
        }
        handle(pr);
        return true;
    }

    private void handle(PullResult result) {
        for (MessageExt msg : result.getMsgFoundList()) {
            byte[] body = msg.getBody();
            OffsetSync sync = OffsetSync.decode(body);
            syncs.put(sync.getMq(), sync);
        }
    }

    private OffsetSync latestOffsetSync(MessageQueue queue) {
        return syncs.computeIfAbsent(queue, new Function<MessageQueue, OffsetSync>() {
            @Override public OffsetSync apply(MessageQueue queue) {
                return new OffsetSync(queue, -1, -1);
            }
        });
    }

}
