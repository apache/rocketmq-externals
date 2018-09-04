package org.apache.rocketmq.spring.starter.config;

import io.netty.util.internal.ConcurrentSet;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.spring.starter.core.RocketMQTemplate;
import org.apache.rocketmq.spring.starter.core.RocketMQTxInternalUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TransactionHandlerRegistry implements DisposableBean {
  @Autowired
  private RocketMQTemplate rocketMQTemplate;

  private final Set<String> listenerContainers = new ConcurrentSet<>();

  public TransactionHandlerRegistry() {
  }

  public Collection<String> getAllTrans() {
    return Collections.unmodifiableSet(listenerContainers);
  }

  @Override
  public void destroy() throws Exception {
    listenerContainers.clear();
  }

  public void registerTransactionHandler(TransactionHandler handler) throws MQClientException {
    if (listenerContainers.contains(handler.getName()))
      throw new MQClientException(-1,
          String.format("The transaction name [%s] has been defined in TransactionListener [%s]", handler.getName(),
              handler.getBeanName()));
    listenerContainers.add(handler.getName());

    RocketMQTxInternalUtil.create(rocketMQTemplate).createAndStartTransactionMQProducer(handler.getName(), handler.getListener(), handler.getCheckExecutor());
  }
}
