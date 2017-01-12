package org.apache.rocketmq.console.service;

/**
 * Created by tcrow on 2017/1/12 0012.
 * Collect DashBoard Data Scheduled
 */
public interface DashBoardCollectService {

    void collectTopic();
    void collectBroker();
    void saveData();

}
