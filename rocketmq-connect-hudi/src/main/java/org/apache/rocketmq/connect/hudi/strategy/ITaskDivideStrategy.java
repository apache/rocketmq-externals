package org.apache.rocketmq.connect.hudi.strategy;

import io.openmessaging.KeyValue;

import java.util.List;

/**
 * @author osgoo
 * @date 2021/9/2
 */
public interface ITaskDivideStrategy {
    List<KeyValue> divide(KeyValue source);
}
