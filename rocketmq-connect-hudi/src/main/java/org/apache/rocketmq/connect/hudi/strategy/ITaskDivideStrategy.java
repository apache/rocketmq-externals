package org.apache.rocketmq.connect.hudi.strategy;

import io.openmessaging.KeyValue;

import java.util.List;


public interface ITaskDivideStrategy {
    List<KeyValue> divide(KeyValue source);
}
