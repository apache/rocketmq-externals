package org.apache.rocketmq.spring.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.spring.starter.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.starter.config.TransactionHandler;
import org.apache.rocketmq.spring.starter.config.TransactionHandlerRegistry;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.*;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RocketMQTransactionAnnotationProcessor
    implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {
  private BeanFactory beanFactory;
  private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();
  private final Set<Class<?>> nonAnnotatedClasses =
      Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));

  @Autowired
  private TransactionHandlerRegistry transactionHandlerRegistry;


  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
    if (beanFactory instanceof ConfigurableListableBeanFactory) {
      this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
    }
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
      Class<?> targetClass = AopUtils.getTargetClass(bean);
      RocketMQTransactionListener listener = AnnotationUtils.findAnnotation(targetClass, RocketMQTransactionListener.class);;
      this.nonAnnotatedClasses.add(bean.getClass());
      if (listener == null) { //for quick search
          log.trace("No @RocketMQTransactionListener annotations found on bean type: {}", bean.getClass());
      }else {
            try{
              processTransactionListenerAnnotation(listener, bean, beanName);
            } catch (MQClientException e) {
              log.error("failed to process annotation " + listener, e);
              throw new BeanCreationException("failed to process annotation " + listener, e);
            }
          }
    }

    return bean;
  }

  private void processTransactionListenerAnnotation(RocketMQTransactionListener anno, Object bean, String beanName) throws MQClientException {
    if (!TransactionListener.class.isAssignableFrom(bean.getClass())) {
      throw new MQClientException(-1, "Bad usage of @RocketMQTransactionListener, the class must implements interface org.apache.rocketmq.client.producer.TransactionListener");
    }
    TransactionHandler transactionHandler = new TransactionHandler();
    transactionHandler.setBeanFactory(this.beanFactory);
    transactionHandler.setName(anno.transName());
    transactionHandler.setBeanName(bean.getClass().getName());
    transactionHandler.setListener((TransactionListener)bean);
    transactionHandler.setCheckExecutor(anno.corePoolSize(), anno.maximumPoolSize(),
        anno.keepAliveTime(), anno.blockingQueueSize());

    transactionHandlerRegistry.registerTransactionHandler(transactionHandler);
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  @Override
  public void afterSingletonsInstantiated() {
    //do nothing
  }
}
