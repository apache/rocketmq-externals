package org.apache.rocketmq.console.service.impl;

import org.apache.rocketmq.console.service.DashBoardCollectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Created by tcrow on 2017/1/12 0012.
 */
@Service
public class DashBoardCollectServiceImpl implements DashBoardCollectService {

    private Logger logger = LoggerFactory.getLogger(DashBoardCollectServiceImpl.class);

    @Scheduled(cron="0/5 * *  * * ? ")
    @Override
    public void collectTopic() {
        logger.error("collect topic >>>>>>");
    }

    @Scheduled(cron="0/5 * *  * * ? ")
    @Override
    public void collectBroker() {
        logger.error("collect broker >>>>>>");
    }

    @Scheduled(cron="0/5 * *  * * ? ")
    @Override
    public void saveData() {
        logger.error("save data >>>>>>");
    }
}
