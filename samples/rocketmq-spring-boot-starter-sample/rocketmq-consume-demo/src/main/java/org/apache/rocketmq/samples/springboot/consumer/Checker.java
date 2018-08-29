package org.apache.rocketmq.samples.springboot.consumer;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.starter.annotation.RocketMQTransactionListener;

/**
 * Nagitive testing to tell @RocketMQTransactionListener can not be used on consumer side!!!
 * You can try this after uncomment the annotation declaration.
 *
 * Created by wiseking on 18/9/10.
 */
//@RocketMQTransactionListener
public class Checker implements TransactionListener  {
  @Override
  public LocalTransactionState executeLocalTransaction(Message message, Object o) {
    return null;
  }

  @Override
  public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
    return null;
  }
}
