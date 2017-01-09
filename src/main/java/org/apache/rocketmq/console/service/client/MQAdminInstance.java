package org.apache.rocketmq.console.service.client;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.impl.MQClientAPIImpl;
import com.alibaba.rocketmq.client.impl.factory.MQClientInstance;
import com.alibaba.rocketmq.remoting.RemotingClient;
import com.alibaba.rocketmq.tools.admin.DefaultMQAdminExt;
import com.alibaba.rocketmq.tools.admin.DefaultMQAdminExtImpl;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.console.util.joor.Reflect;

/**
 * Created by tangjie on 2016/11/18.
 */
public class MQAdminInstance {
    private static final ThreadLocal<DefaultMQAdminExt> mqAdminExtThreadLocal = new ThreadLocal<DefaultMQAdminExt>();
    private static final ThreadLocal<Integer> initCounter = new ThreadLocal<Integer>();

    public static MQAdminExt threadLocalMQAdminExt() {
        DefaultMQAdminExt defaultMQAdminExt = mqAdminExtThreadLocal.get();
        if (defaultMQAdminExt == null) {
            throw new IllegalStateException("defaultMQAdminExt should be init before you get this");
        }
        return defaultMQAdminExt;
    }

    /**
     * 使用反射获取到DefaultMQAdminExt中的RemotingCommand
     *
     * @return
     */
    public static RemotingClient threadLocalRemotingClient() {
        DefaultMQAdminExtImpl defaultMQAdminExtImpl = Reflect.on(MQAdminInstance.threadLocalMQAdminExt()).get("defaultMQAdminExtImpl");
        MQClientInstance mqClientInstance = Reflect.on(defaultMQAdminExtImpl).get("mqClientInstance");
        MQClientAPIImpl mQClientAPIImpl = Reflect.on(mqClientInstance).get("mQClientAPIImpl");
        return Reflect.on(mQClientAPIImpl).get("remotingClient");
    }

    public static void initMQAdminInstance() throws MQClientException {
        Integer nowCount = initCounter.get();
        if (nowCount == null) {
            DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt();
            defaultMQAdminExt.setInstanceName(Long.toString(System.currentTimeMillis()));
            defaultMQAdminExt.start();
            mqAdminExtThreadLocal.set(defaultMQAdminExt);
            initCounter.set(1);
        } else {
            initCounter.set(nowCount + 1);
        }

    }

    public static void destroyMQAdminInstance() {
        Integer nowCount = initCounter.get() - 1;
        if (nowCount > 0) {
            initCounter.set(nowCount);
            return;
        }
        MQAdminExt mqAdminExt = mqAdminExtThreadLocal.get();
        if (mqAdminExt != null) {
            mqAdminExt.shutdown();
            mqAdminExtThreadLocal.remove();
            initCounter.remove();
        }
    }
}
