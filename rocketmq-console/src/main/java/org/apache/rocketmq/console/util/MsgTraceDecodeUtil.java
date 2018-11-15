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
import org.apache.rocketmq.client.trace.core.common.TrackTraceBean;
import org.apache.rocketmq.client.trace.core.common.TrackTraceConstants;
import org.apache.rocketmq.client.trace.core.common.TrackTraceContext;
import org.apache.rocketmq.client.trace.core.common.TrackTraceType;
import org.apache.rocketmq.common.message.MessageType;

import static org.apache.rocketmq.client.trace.core.common.TrackTraceType.Pub;

public class MsgTraceDecodeUtil {

    public static List<TrackTraceContext> decoderFromTraceDataString(String traceData) {
        List<TrackTraceContext> resList = new ArrayList<TrackTraceContext>();
        if (traceData == null || traceData.length() <= 0) {
            return resList;
        }
        String[] contextList = traceData.split(String.valueOf(TrackTraceConstants.FIELD_SPLITOR));
        for (String context : contextList) {
            String[] line = context.split(String.valueOf(TrackTraceConstants.CONTENT_SPLITOR));
            if (line[0].equals(Pub.name())) {
                TrackTraceContext pubContext = new TrackTraceContext();
                pubContext.setTraceType(Pub);
                pubContext.setTimeStamp(Long.parseLong(line[1]));
                pubContext.setRegionId(line[2]);
                pubContext.setGroupName(line[3]);
                TrackTraceBean bean = new TrackTraceBean();
                bean.setTopic(line[4]);
                bean.setMsgId(line[5]);
                bean.setTags(line[6]);
                bean.setKeys(line[7]);
                bean.setStoreHost(line[8]);
                bean.setBodyLength(Integer.parseInt(line[9]));
                pubContext.setCostTime(Integer.parseInt(line[10]));
                bean.setMsgType(MessageType.values()[Integer.parseInt(line[11])]);

                if (line.length == 13) {
                    pubContext.setSuccess(Boolean.parseBoolean(line[12]));
                } else if (line.length == 14) {
                    bean.setOffsetMsgId(line[12]);
                    pubContext.setSuccess(Boolean.parseBoolean(line[13]));
                }
                pubContext.setTraceBeans(new ArrayList<TrackTraceBean>(1));
                pubContext.getTraceBeans().add(bean);
                resList.add(pubContext);
            } else if (line[0].equals(TrackTraceType.SubBefore.name())) {
                TrackTraceContext subBeforeContext = new TrackTraceContext();
                subBeforeContext.setTraceType(TrackTraceType.SubBefore);
                subBeforeContext.setTimeStamp(Long.parseLong(line[1]));
                subBeforeContext.setRegionId(line[2]);
                subBeforeContext.setGroupName(line[3]);
                subBeforeContext.setRequestId(line[4]);
                TrackTraceBean bean = new TrackTraceBean();
                bean.setMsgId(line[5]);
                bean.setRetryTimes(Integer.parseInt(line[6]));
                bean.setKeys(line[7]);
                subBeforeContext.setTraceBeans(new ArrayList<TrackTraceBean>(1));
                subBeforeContext.getTraceBeans().add(bean);
                resList.add(subBeforeContext);
            } else if (line[0].equals(TrackTraceType.SubAfter.name())) {
                TrackTraceContext subAfterContext = new TrackTraceContext();
                subAfterContext.setTraceType(TrackTraceType.SubAfter);
                subAfterContext.setRequestId(line[1]);
                TrackTraceBean bean = new TrackTraceBean();
                bean.setMsgId(line[2]);
                bean.setKeys(line[5]);
                subAfterContext.setTraceBeans(new ArrayList<TrackTraceBean>(1));
                subAfterContext.getTraceBeans().add(bean);
                subAfterContext.setCostTime(Integer.parseInt(line[3]));
                subAfterContext.setSuccess(Boolean.parseBoolean(line[4]));
                if (line.length >= 7) {
                    // add the context type
                    subAfterContext.setContextCode(Integer.parseInt(line[6]));
                }
                resList.add(subAfterContext);
            }
        }
        return resList;
    }
}
