
package org.apache.rocketmq.connect.runtime.service;

import java.util.List;

/**
 * Interface for cluster management.
 */
public interface ClusterManagementService {

    Long WORKER_TIME_OUT = 30 * 1000L;

    /**
     * Start the cluster manager.
     */
    void start();

    /**
     * Stop the cluster manager.
     */
    void stop();

    /**
     * Check if Cluster Store Topic exists.
     *
     * @return true if Cluster Store Topic exists, otherwise return false.
     */
    boolean hasClusterStoreTopic();


    /**
     * Get all alive workers in the cluster.
     *
     * @return
     */
    List<String> getAllAliveWorkers();

    /**
     * Register a worker status listener to listen the change of alive workers.
     *
     * @param listener
     */
    void registerListener(WorkerStatusListener listener);

    String getCurrentWorker();

    interface WorkerStatusListener {

        /**
         * If a worker online or offline, this method will be invoked. Can use method {@link
         * ClusterManagementService#getAllAliveWorkers()} to get the all current alive workers.
         */
        void onWorkerChange();
    }
}
