package org.apache.rocketmq.iot.benchmark.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentTools {
    private static Logger logger = LoggerFactory.getLogger(ConcurrentTools.class);

    private ExecutorService executor;

    public ConcurrentTools() {
        this.executor = Executors.newCachedThreadPool();
    }

    public void submitTask(List<Runnable> taskList, long timeout) throws InterruptedException {
        logger.info("start submit tasks.");
        List<Future> resultList = new ArrayList<>(taskList.size());
        for (int i = 0; i < taskList.size(); i++) {
            resultList.add(executor.submit(taskList.get(i)));
        }
        logger.info("submit done, await tasks.");

        boolean completeWithinTimeout = executor.awaitTermination(timeout, TimeUnit.SECONDS);
        if (!completeWithinTimeout) {
            logger.warn("execute tasks out of timeout [" + timeout + " ms]");
            // cancel all task
            for (Future r : resultList) {
                r.cancel(true);
            }
        }
        executor.shutdown();
        System.exit(-1);
    }
}