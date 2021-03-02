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

package org.apache.rocketmq.console.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.client.trace.TraceBean;
import org.apache.rocketmq.client.trace.TraceConstants;
import org.apache.rocketmq.client.trace.TraceContext;
import org.apache.rocketmq.client.trace.TraceType;
import org.apache.rocketmq.common.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.client.trace.TraceType.Pub;

public class MsgTraceDecodeUtil {
    private final static Logger log = LoggerFactory.getLogger(MsgTraceDecodeUtil.class);

    private static final int TRACE_MSG_PUB_V1_LEN = 12;
    private static final int TRACE_MSG_PUB_V2_LEN = 13;
    private static final int TRACE_MSG_PUB_V3_LEN = 14;
    private static final int TRACE_MSG_PUB_V4_LEN = 15;

    private static final int TRACE_MSG_SUBAFTER_V1_LEN = 6;
    private static final int TRACE_MSG_SUBAFTER_V2_LEN = 7;
    private static final int TRACE_MSG_SUBAFTER_V3_LEN = 9;

    public static List<TraceContext> decoderFromTraceDataString(String traceData) {
        List<TraceContext> resList = new ArrayList<TraceContext>();
        if (traceData == null || traceData.length() <= 0) {
            return resList;
        }
        String[] contextList = traceData.split(String.valueOf(TraceConstants.FIELD_SPLITOR));
        for (String context : contextList) {
            String[] line = context.split(String.valueOf(TraceConstants.CONTENT_SPLITOR));
            if (line[0].equals(Pub.name())) {
                TraceContext pubContext = new TraceContext();
                pubContext.setTraceType(Pub);
                pubContext.setTimeStamp(Long.parseLong(line[1]));
                pubContext.setRegionId(line[2]);
                pubContext.setGroupName(line[3]);
                TraceBean bean = new TraceBean();
                bean.setTopic(line[4]);
                bean.setMsgId(line[5]);
                bean.setTags(line[6]);
                bean.setKeys(line[7]);
                bean.setStoreHost(line[8]);
                bean.setBodyLength(Integer.parseInt(line[9]));
                pubContext.setCostTime(Integer.parseInt(line[10]));
                bean.setMsgType(MessageType.values()[Integer.parseInt(line[11])]);
                // compatible with different version
                switch (line.length) {
                    case TRACE_MSG_PUB_V1_LEN:
                        break;
                    case TRACE_MSG_PUB_V2_LEN:
                        pubContext.setSuccess(Boolean.parseBoolean(line[12]));
                        break;
                    case TRACE_MSG_PUB_V3_LEN:
                        bean.setOffsetMsgId(line[12]);
                        pubContext.setSuccess(Boolean.parseBoolean(line[13]));
                        break;
                    case TRACE_MSG_PUB_V4_LEN:
                        bean.setOffsetMsgId(line[12]);
                        pubContext.setSuccess(Boolean.parseBoolean(line[13]));
                        bean.setClientHost(line[14]);
                        break;
                    default:
                        bean.setOffsetMsgId(line[12]);
                        pubContext.setSuccess(Boolean.parseBoolean(line[13]));
                        bean.setClientHost(line[14]);
                        log.warn("Detect new version trace msg of {} type", Pub.name());
                        break;
                }

                pubContext.setTraceBeans(new ArrayList<TraceBean>(1));
                pubContext.getTraceBeans().add(bean);
                resList.add(pubContext);
            } else if (line[0].equals(TraceType.SubBefore.name())) {
                TraceContext subBeforeContext = new TraceContext();
                subBeforeContext.setTraceType(TraceType.SubBefore);
                subBeforeContext.setTimeStamp(Long.parseLong(line[1]));
                subBeforeContext.setRegionId(line[2]);
                subBeforeContext.setGroupName(line[3]);
                subBeforeContext.setRequestId(line[4]);
                TraceBean bean = new TraceBean();
                bean.setMsgId(line[5]);
                bean.setRetryTimes(Integer.parseInt(line[6]));
                bean.setKeys(line[7]);
                bean.setClientHost(line[8]);
                subBeforeContext.setTraceBeans(new ArrayList<TraceBean>(1));
                subBeforeContext.getTraceBeans().add(bean);
                resList.add(subBeforeContext);
            } else if (line[0].equals(TraceType.SubAfter.name())) {
                TraceContext subAfterContext = new TraceContext();
                subAfterContext.setTraceType(TraceType.SubAfter);
                subAfterContext.setRequestId(line[1]);
                TraceBean bean = new TraceBean();
                bean.setMsgId(line[2]);
                bean.setKeys(line[5]);
                subAfterContext.setTraceBeans(new ArrayList<TraceBean>(1));
                subAfterContext.getTraceBeans().add(bean);
                subAfterContext.setCostTime(Integer.parseInt(line[3]));
                subAfterContext.setSuccess(Boolean.parseBoolean(line[4]));
                // compatible with different version
                switch (line.length) {
                    case TRACE_MSG_SUBAFTER_V1_LEN:
                        break;
                    case TRACE_MSG_SUBAFTER_V2_LEN:
                        subAfterContext.setContextCode(Integer.parseInt(line[6]));
                        break;
                    case TRACE_MSG_SUBAFTER_V3_LEN:
                        subAfterContext.setContextCode(Integer.parseInt(line[6]));
                        subAfterContext.setTimeStamp(Long.parseLong(line[7]));
                        subAfterContext.setGroupName(line[8]);
                        break;
                    default:
                        subAfterContext.setContextCode(Integer.parseInt(line[6]));
                        subAfterContext.setTimeStamp(Long.parseLong(line[7]));
                        subAfterContext.setGroupName(line[8]);
                        log.warn("Detect new version trace msg of {} type", TraceType.SubAfter.name());
                        break;
                }
                resList.add(subAfterContext);
            }
        }
        return resList;
    }
}
