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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.aspect.admin;

import org.apache.rocketmq.console.service.client.MQAdminInstance;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;

/**
 * Created by tangjie on 2016/11/18.
 */
@Aspect
@Service
public class MQAdminAspect {
    public MQAdminAspect() {
    }

    @Pointcut("execution(* org.apache.rocketmq.console.service.client.MQAdminExtImpl..*(..))")
    public void mQAdminMethodPointCut() {

    }

    @Pointcut("@annotation(org.apache.rocketmq.console.aspect.admin.annotation.MultiMQAdminCmdMethod)")
    public void multiMQAdminMethodPointCut() {

    }

    @Around(value = "mQAdminMethodPointCut() || multiMQAdminMethodPointCut()")
    public Object aroundMQAdminMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;
        try {
            MQAdminInstance.initMQAdminInstance();
            obj = joinPoint.proceed();
        }
        finally {
            MQAdminInstance.destroyMQAdminInstance();
        }
        return obj;
    }
}
