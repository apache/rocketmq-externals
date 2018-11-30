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

package org.apache.rocketmq.spring.config;

import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.springframework.beans.factory.BeanFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class TransactionHandler {
    private String name;
    private String beanName;
    private RocketMQLocalTransactionListener bean;
    private BeanFactory beanFactory;
    private ThreadPoolExecutor checkExecutor;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setListener(RocketMQLocalTransactionListener listener) {
        this.bean = listener;
    }

    public RocketMQLocalTransactionListener getListener() {
        return this.bean;
    }

    public void setCheckExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, int blockingQueueSize) {
        this.checkExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
            keepAliveTime, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<>(blockingQueueSize));
    }

    public ThreadPoolExecutor getCheckExecutor() {
        return checkExecutor;
    }
}
