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

package org.apache.rocketmq.jms.msg.serialize;

import com.google.common.base.Charsets;
import java.nio.charset.Charset;
import javax.jms.JMSException;

public class StringSerialize implements Serialize<String> {

    private static final String EMPTY_STRING = "";
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
    private static StringSerialize ins = new StringSerialize();

    public static StringSerialize instance() {
        return ins;
    }

    private StringSerialize() {
    }

    @Override public byte[] serialize(String s) throws JMSException {
        if (null == s) {
            return EMPTY_BYTES;
        }
        try {
            return s.getBytes(DEFAULT_CHARSET);
        }
        catch (Exception e) {
            throw new JMSException(e.getMessage());
        }
    }

    @Override public String deserialize(byte[] bytes) throws JMSException {
        if (null == bytes) {
            return EMPTY_STRING;
        }
        try {
            return new String(bytes, DEFAULT_CHARSET);
        }
        catch (Exception e) {
            throw new JMSException(e.getMessage());
        }
    }
}
