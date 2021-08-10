package org.apache.rocketmq.console.admin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.rocketmq.console.aspect.admin.MQAdminAspect;
import org.apache.rocketmq.console.aspect.admin.annotation.MultiMQAdminCmdMethod;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MQAdminAspectTest {

    @Test
    public void testAroundMQAdminMethod() throws Throwable {
        MQAdminAspect mqAdminAspect = new MQAdminAspect();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = mock(Method.class);
        MultiMQAdminCmdMethod annotationValue = mock(MultiMQAdminCmdMethod.class);
        when(annotationValue.timeoutMillis()).thenReturn(0L).thenReturn(3000L);
        when(method.getAnnotation(MultiMQAdminCmdMethod.class)).thenReturn(annotationValue);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);

        RMQConfigure rmqConfigure = mock(RMQConfigure.class);
        when(rmqConfigure.getAccessKey()).thenReturn("rocketmq");
        when(rmqConfigure.getSecretKey()).thenReturn("12345678");
        Field field = mqAdminAspect.getClass().getDeclaredField("rmqConfigure");
        field.setAccessible(true);
        field.set(mqAdminAspect, rmqConfigure);

        mqAdminAspect.aroundMQAdminMethod(joinPoint);
    }
}
