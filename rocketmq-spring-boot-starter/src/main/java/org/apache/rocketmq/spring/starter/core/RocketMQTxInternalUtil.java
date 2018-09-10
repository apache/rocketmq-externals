package org.apache.rocketmq.spring.starter.core;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionListener;

import java.util.concurrent.ExecutorService;

/**
 * Create and remove txProducer internally
 */
public class RocketMQTxInternalUtil {
  private RocketMQTemplate rocketMQTemplate;

  private static RocketMQTxInternalUtil instance = null;

  private RocketMQTxInternalUtil(RocketMQTemplate rocketMQTemplate) {
    this.rocketMQTemplate = rocketMQTemplate;
  }

  public boolean createAndStartTransactionMQProducer(String name, TransactionListener transactionListener,
                                              ExecutorService executorService) throws MQClientException {
    return rocketMQTemplate.createAndStartTransactionMQProducer(name, transactionListener, executorService);
  }

  public void removeTransactionMQProducer(String name) throws MQClientException {
    this.rocketMQTemplate.removeTransactionMQProducer(name);
  }

  public static RocketMQTxInternalUtil create(RocketMQTemplate rocketMQTemplate) {
    if (instance==null) {
      synchronized (RocketMQTxInternalUtil.class) {
        instance = new RocketMQTxInternalUtil(rocketMQTemplate);
      }
    }

    return instance;
  }
}
