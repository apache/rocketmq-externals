package org.apache.rocketmq.console.service.impl;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.protocol.body.ProducerConnection;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.console.service.ProducerService;
import com.google.common.base.Throwables;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Created by tangjie
 * 2016/12/1
 * styletang.me@gmail.com
 */
@Service
public class ProducerServiceImpl implements ProducerService {
    @Resource
    private MQAdminExt mqAdminExt;

    @Override
    public ProducerConnection getProducerConnection(String producerGroup, String topic) {
        try {
            return mqAdminExt.examineProducerConnectionInfo(producerGroup, topic);
        } catch (RemotingException | MQClientException | MQBrokerException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }
}
