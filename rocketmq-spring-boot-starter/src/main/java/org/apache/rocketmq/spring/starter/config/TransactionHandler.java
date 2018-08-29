package org.apache.rocketmq.spring.starter.config;

import org.apache.rocketmq.client.producer.TransactionListener;
import org.springframework.beans.factory.BeanFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TransactionHandler {
  private String name;
  private String beanName;
  private TransactionListener bean;
  private BeanFactory beanFactory;
  private ThreadPoolExecutor checkExecutor;

  public String getBeanName() {
    return beanName;
  }

  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public void setListener(TransactionListener listener) {
    this.bean = listener;
  }

  public TransactionListener getListener() {
    return this.bean;
  }

  public void setCheckExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, int blockingQueueSize) {
    this.checkExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
        keepAliveTime, TimeUnit.MILLISECONDS,
        new LinkedBlockingDeque<>(blockingQueueSize));
  }

  public ThreadPoolExecutor getCheckExecutor() {
    return checkExecutor;
  }
}
