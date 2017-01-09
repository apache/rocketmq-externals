package org.apache.rocketmq.console.aspect.admin;

import org.apache.rocketmq.console.service.client.MQAdminInstance;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;

/**
 * Created by tangjie on 2016/11/18.
 */
@Aspect
@Service
public class MQAdminAspect {
    public MQAdminAspect() {
    }

    @Pointcut("execution(* org.apache.rocketmq.console.service.client.MQAdminExtImpl..*(..))")
    public void mQAdminMethodPointCut(){

    }
    @Pointcut("@annotation(org.apache.rocketmq.console.aspect.admin.annotation.MultiMQAdminCmdMethod)")
    public void multiMQAdminMethodPointCut(){

    }
    @Around(value = "mQAdminMethodPointCut() || multiMQAdminMethodPointCut()")
    public Object aroundMQAdminMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;
        try {
            MQAdminInstance.initMQAdminInstance();
            obj = joinPoint.proceed();
        } finally {
            MQAdminInstance.destroyMQAdminInstance();
        }
        return obj;
    }
}
