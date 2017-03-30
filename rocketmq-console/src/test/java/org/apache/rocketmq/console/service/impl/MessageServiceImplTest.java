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

package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.service.MessageService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class MessageServiceImplTest extends RocketMQConsoleTestBase {

    @Resource
    private MessageService messageService;
    @Resource
    private MQAdminExt mqAdminExt;
    private String msgId;
    @Before
    public void setUp() throws Exception {
        initMQClientEnv();
        registerTestMQTopic();
        msgId = sendTestTopicMessage().getMsgId();
    }

    @After
    public void tearDown() throws Exception {
        destroyMQClientEnv();
    }
    @Test
    public void viewMessage() throws Exception {
        final String messageId = msgId;
        Pair<MessageView, List<MessageTrack>> messageViewListPair=new RetryTempLate<Pair<MessageView, List<MessageTrack>>>() {
            @Override protected Pair<MessageView, List<MessageTrack>> process() throws Exception {
                return  messageService.viewMessage(TEST_CONSOLE_TOPIC,messageId);
            }
        }.execute(10,1000);

        MessageView messageView = messageViewListPair.getObject1();
        Assert.assertEquals(messageView.getMessageBody(),TEST_TOPIC_MESSAGE_BODY);
    }

    @Test
    public void queryMessageByTopicAndKey() throws Exception {
        final String messageId = msgId;
        List<MessageView> messageViewList=new RetryTempLate<List<MessageView>>() {
            @Override protected List<MessageView> process() throws Exception {
                return  messageService.queryMessageByTopicAndKey(TEST_CONSOLE_TOPIC,TEST_TOPIC_MESSAGE_KEY);
            }
        }.execute(10,1000);
        Assert.assertTrue(CollectionUtils.isNotEmpty(messageViewList));
        Assert.assertTrue(Lists.transform(messageViewList, new Function<MessageView, String>() {
            @Override public String apply(MessageView input) {
                return input.getMsgId();
            }
        }).contains(messageId));
    }

    @Test
    public void queryMessageByTopic() throws Exception {
        final String messageId = msgId;
        List<MessageView> messageViewList = null;
        for (int i = 0; i < 10; i++) {
            messageViewList = messageService.queryMessageByTopic(TEST_CONSOLE_TOPIC, System.currentTimeMillis() - 100000, System.currentTimeMillis());
            if (CollectionUtils.isEmpty(messageViewList)) {
                Thread.sleep(1000);
                continue;
            }
            break;
        }
        Assert.assertTrue(CollectionUtils.isNotEmpty(messageViewList));
        Assert.assertTrue(Lists.transform(messageViewList, new Function<MessageView, String>() {
            @Override public String apply(MessageView input) {
                return input.getMsgId();
            }
        }).contains(messageId));
    }

    @Test
    public void messageTrackDetail() throws Exception {
        final String messageId = msgId;
        Pair<MessageView, List<MessageTrack>> messageViewListPair=new RetryTempLate<Pair<MessageView, List<MessageTrack>>>() {
            @Override protected Pair<MessageView, List<MessageTrack>> process() throws Exception {
                return  messageService.viewMessage(TEST_CONSOLE_TOPIC,messageId);
            }
        }.execute(10,1000); // make the topic can be found
        final MessageExt messageExt = mqAdminExt.viewMessage(TEST_CONSOLE_TOPIC,messageId);
        Assert.assertNotNull(messageService.messageTrackDetail(messageExt));
    }

    @Test
    public void consumeMessageDirectly() throws Exception {
//        final String messageId = msgId;
//        ConsumeMessageDirectlyResult messageDirectlyResult=new RetryTempLate<ConsumeMessageDirectlyResult>() {
//            @Override protected ConsumeMessageDirectlyResult process() throws Exception {
//                return  messageService.consumeMessageDirectly(TEST_CONSOLE_TOPIC,messageId,TEST_CONSUMER_GROUP,null);
//            }
//        }.execute(20,1000); // todo test
    }


}