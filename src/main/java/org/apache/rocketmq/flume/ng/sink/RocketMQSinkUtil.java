/**
 * Copyright 2006-2014 handu.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.flume.ng.sink;

import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.MQProducer;
import com.google.common.base.Preconditions;
import org.apache.flume.Context;

/**
 * Sink工具类
 *
 * @author Jinkai.Ma<majinkai@handu.com>
 * @since 2015-01-16
 */
public class RocketMQSinkUtil {

    /**
     * Topic配置项，如：a1.sinks.s1.topic=TestTopic
     */
    public static final String TOPIC_CONFIG = "topic";
    public static final String TOPIC_DEFAULT = "FLUME_ROCKETMQ";
    /**
     * Tags配置项，如：a1.sinks.s1.tags=Tag1,Tag2
     */
    public static final String TAG_CONFIG = "tag";
    public static final String TAG_DEFAULT = "";
    /**
     * Producer分组配置项，如：a1.sinks.s1.producerGroup=please_rename_unique_group_name
     */
    public static final String PRODUCER_GROUP_CONFIG = "producerGroup";
    public static final String PRODUCER_GROUP_DEFAULT = "DEFAULT_PRODUCER";
    /**
     * Namesrv地址配置项，如：a1.sinks.s1.namesrvAddr=localhost:9876
     */
    public static final String NAMESRV_ADDR_CONFIG = "namesrvAddr";

    public static MQProducer getProducer(Context context) {
        final String producerGroup = context.getString(PRODUCER_GROUP_CONFIG, PRODUCER_GROUP_DEFAULT);
        final String namesrvAddr = Preconditions.checkNotNull(context.getString(NAMESRV_ADDR_CONFIG), "RocketMQ namesrvAddr must be specified. For example: a1.sinks.s1.namesrvAddr=127.0.0.1:9876");

        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(namesrvAddr);

        return producer;
    }

}
