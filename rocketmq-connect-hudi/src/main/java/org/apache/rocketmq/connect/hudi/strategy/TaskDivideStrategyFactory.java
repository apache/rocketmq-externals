package org.apache.rocketmq.connect.hudi.strategy;

/**
 * @author osgoo
 * @date 2021/9/2
 */
public class TaskDivideStrategyFactory {
    public static ITaskDivideStrategy getInstance() {
        return new TaskDivideByQueueStrategy();
    }
}
