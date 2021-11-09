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
package org.apache.rocketmq.cloudevent.impl;

import io.cloudevents.core.message.impl.MessageUtils;
import io.cloudevents.core.v1.CloudEventV1;

import java.util.Map;

public class RocketMQHeaders {

    public static final String CE_PREFIX = "CE_";

    protected static final Map<String, String> ATTRIBUTES_TO_HEADERS = MessageUtils.generateAttributesToHeadersMapping(v -> CE_PREFIX + v);

    public static final String CONTENT_TYPE = ATTRIBUTES_TO_HEADERS.get(CloudEventV1.DATACONTENTTYPE);

    public static final String SPEC_VERSION = ATTRIBUTES_TO_HEADERS.get(CloudEventV1.SPECVERSION);

}

