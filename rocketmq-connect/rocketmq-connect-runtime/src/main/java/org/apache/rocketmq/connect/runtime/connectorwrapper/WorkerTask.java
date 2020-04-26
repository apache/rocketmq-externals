package org.apache.rocketmq.connect.runtime.connectorwrapper;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;

public interface WorkerTask extends Runnable {

    public abstract WorkerTaskState getState();

    public abstract void stop();

    public abstract void cleanup();

    public abstract String getConnectorName();

    public abstract ConnectKeyValue getTaskConfig();
}
