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
package org.apache.rocketmq.spring.support;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRocketMQListenerContainerTest {

    @Test
    public void testGetMessageType() throws NoSuchMethodException {
        DefaultRocketMQListenerContainer listenerContainer = new DefaultRocketMQListenerContainer();
        Method getMessageType = DefaultRocketMQListenerContainer.class.getDeclaredMethod("getMessageType");
        getMessageType.setAccessible(true);
        try {
            listenerContainer.setRocketMQListener(new BaseStringConsumer());
            Class result = (Class) getMessageType.invoke(listenerContainer);
            assertThat(result.getName().equals(String.class.getName())).isTrue();

            listenerContainer.setRocketMQListener(new BaseMessageExtConsumer());
            result = (Class) getMessageType.invoke(listenerContainer);
            assertThat(result.getName().equals(MessageExt.class.getName())).isTrue();

            listenerContainer.setRocketMQListener(new ConcreteStringConsumer());
            result = (Class) getMessageType.invoke(listenerContainer);
            assertThat(result.getName().equals(String.class.getName())).isTrue();

            listenerContainer.setRocketMQListener(new ConcreteMessageExtConsumer());
            result = (Class) getMessageType.invoke(listenerContainer);
            assertThat(result.getName().equals(MessageExt.class.getName())).isTrue();

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

class BaseStringConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
    }
}

class ConcreteStringConsumer extends BaseStringConsumer {
}

class BaseMessageExtConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt message) {
    }
}

class ConcreteMessageExtConsumer extends BaseMessageExtConsumer {
}

