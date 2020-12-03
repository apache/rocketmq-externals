/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.common.util;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.SimpleCounter;

/**
 * RocketMQ connector metrics.
 */
public class MetricUtils {

    public static final String METRICS_TPS = "tps";

    private static final String METRIC_GROUP_SINK = "sink";
    private static final String METRICS_SINK_IN_TPS = "inTps";
    private static final String METRICS_SINK_OUT_TPS = "outTps";
    private static final String METRICS_SINK_OUT_BPS = "outBps";
    private static final String METRICS_SINK_OUT_Latency = "outLatency";

    public static Meter registerSinkInTps(RuntimeContext context) {
        Counter parserCounter = context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
            .counter(METRICS_SINK_IN_TPS + "_counter", new SimpleCounter());
        return context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
            .meter(METRICS_SINK_IN_TPS, new MeterView(parserCounter, 60));
    }

    public static Meter registerOutTps(RuntimeContext context) {
        Counter parserCounter = context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
            .counter(METRICS_SINK_OUT_TPS + "_counter", new SimpleCounter());
        return context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
                .meter(METRICS_SINK_OUT_TPS, new MeterView(parserCounter, 60));
    }

    public static Meter registerOutBps(RuntimeContext context) {
        Counter bpsCounter = context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
                .counter(METRICS_SINK_OUT_BPS + "_counter", new SimpleCounter());
        return context.getMetricGroup().addGroup(METRIC_GROUP_SINK)
                .meter(METRICS_SINK_OUT_BPS, new MeterView(bpsCounter, 60));
    }

    public static LatencyGauge registerOutLatency(RuntimeContext context) {
        return context.getMetricGroup().addGroup(METRIC_GROUP_SINK).gauge(METRICS_SINK_OUT_Latency, new LatencyGauge());
    }

    public static class LatencyGauge implements Gauge<Double> {
        private double value;

        public void report(long timeDelta, long batchSize) {
            if (batchSize != 0) {
                this.value = (1.0 * timeDelta) / batchSize;
            }
        }

        @Override
        public Double getValue() {
            return value;
        }
    }
}
