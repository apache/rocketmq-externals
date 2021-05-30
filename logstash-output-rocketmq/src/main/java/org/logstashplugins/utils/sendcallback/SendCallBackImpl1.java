package org.logstashplugins.utils.sendcallback;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

public class SendCallBackImpl1 implements SendCallback {

    private final SendEmail sendEmail;

    public SendCallBackImpl1(SendEmail sendEmail) {
        super();
        this.sendEmail = sendEmail;
    }

    @Override
    public void onSuccess(SendResult sendResult) {
        SendStatus sendStatus = sendResult.getSendStatus();
        if (!(sendStatus == SendStatus.SEND_OK)) {
            try {
                sendEmail.sendAnEmail(sendResult, null, null);
                System.out.println("sent an email");
            } catch (Exception exception) {
                System.out.println("send email failed");
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void onException(Throwable e) {
        e.printStackTrace();
    }
}
