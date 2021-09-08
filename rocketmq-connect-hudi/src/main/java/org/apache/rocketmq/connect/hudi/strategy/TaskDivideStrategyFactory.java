package org.apache.rocketmq.connect.hudi.strategy;


public class TaskDivideStrategyFactory {
    public static ITaskDivideStrategy getInstance() {
        return new TaskDivideByQueueStrategy();
    }
}
