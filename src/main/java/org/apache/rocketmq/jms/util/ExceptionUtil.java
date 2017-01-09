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

package org.apache.rocketmq.jms.util;

import com.google.common.base.Preconditions;
import javax.jms.JMSException;

public class ExceptionUtil {
    public static final boolean SKIP_SET_EXCEPTION
        = Boolean.parseBoolean(System.getProperty("skip.set.exception", "false"));

    public static void handleUnSupportedException() {
        if (!ExceptionUtil.SKIP_SET_EXCEPTION) {
            throw new UnsupportedOperationException("Operation unsupported! If you want to skip this Exception," +
                " use '-Dskip.set.exception=true' in JVM options.");
        }
    }

    public static JMSException convertToJmsException(Exception e, String extra) {
        Preconditions.checkNotNull(extra);
        Preconditions.checkNotNull(e);
        JMSException jmsException = new JMSException(extra);
        jmsException.initCause(e);
        return jmsException;
    }
}