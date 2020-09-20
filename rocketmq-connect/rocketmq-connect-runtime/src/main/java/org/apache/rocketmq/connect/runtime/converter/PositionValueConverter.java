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

package org.apache.rocketmq.connect.runtime.converter;

import io.openmessaging.connector.api.data.Converter;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.common.PositionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class PositionValueConverter implements Converter<PositionValue> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    @Override
    public byte[] objectToByte(PositionValue positionValue) {

        try {

            String partition = Base64.getEncoder().encodeToString(positionValue.getPartition().array());
            String position = Base64.getEncoder().encodeToString(positionValue.getPosition().array());
            return (partition + "+" + position).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("PositionValueConverter#objectToByte failed", e);
        }
        return new byte[0];
    }

    @Override
    public PositionValue byteToObject(byte[] bytes) {

        try {
            String[] rawString = new String(bytes, "UTF-8").split("\\+");
            byte[] partition = Base64.getDecoder().decode(rawString[0]);
            byte[] position = Base64.getDecoder().decode(rawString[1]);
//             log.info("byteToObject Partition is:{}", new String(partition));
            return new PositionValue(ByteBuffer.wrap(partition), ByteBuffer.wrap(position));
        } catch (UnsupportedEncodingException e) {
            log.error("PositionValueConverter#byteToObject failed", e);
        }
        return null;
    }

}
