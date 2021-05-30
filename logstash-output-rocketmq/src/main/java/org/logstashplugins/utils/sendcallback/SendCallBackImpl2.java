package org.logstashplugins.utils.sendcallback;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

public class SendCallBackImpl2 implements SendCallback {


    @Override
    public void onSuccess(SendResult sendResult) {
        SendStatus sendStatus = sendResult.getSendStatus();
        if (!(sendStatus == SendStatus.SEND_OK)) {
            System.out.println(sendResult.toString());
        }
    }

    @Override
    public void onException(Throwable e) {
        e.printStackTrace();
    }
}
