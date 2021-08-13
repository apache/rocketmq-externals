package org.apache.rocketmq.console.permission;

import org.apache.rocketmq.console.BaseTest;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.UserInfo;
import org.apache.rocketmq.console.service.impl.PermissionServiceImpl;
import org.apache.rocketmq.console.util.WebUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionAspectTest extends BaseTest {

    @InjectMocks
    private PermissionAspect permissionAspect;

    @Mock
    private RMQConfigure configure;

    @Spy
    private PermissionServiceImpl permissionService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        autoInjection();
        when(configure.isLoginRequired()).thenReturn(true);
        when(configure.getRocketMqConsoleDataPath()).thenReturn("/tmp/rocketmq-console/test/data");
    }

    @Test
    public void testCheckPermission() throws Throwable {
        ReflectionTestUtils.setField(permissionAspect, "configure", configure);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        permissionService.afterPropertiesSet();
        ReflectionTestUtils.setField(permissionAspect, "permissionService", permissionService);

        // user not login
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            permissionAspect.checkPermission(joinPoint);
        } catch (Throwable throwable) {
            Assert.assertEquals(throwable.getMessage(), "user not login");
        }
        // userRole is admin
        UserInfo info = new UserInfo();
        User adminUser = new User("admin", "admin", 1);
        info.setUser(adminUser);
        request.getSession().setAttribute(WebUtil.USER_INFO, info);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        permissionAspect.checkPermission(joinPoint);

        // userRole is ordinary
        User ordinaryUser = new User("user1", "user1", 0);
        info.setUser(ordinaryUser);
        request = new MockHttpServletRequest("", "/topic/deleteTopic.do");
        request.getSession().setAttribute(WebUtil.USER_INFO, info);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            permissionAspect.checkPermission(joinPoint);
        } catch (Throwable throwable) {
            Assert.assertEquals(throwable.getMessage(), "no permission");
        }

        // no permission
        request = new MockHttpServletRequest("", "/topic/route.query");
        request.getSession().setAttribute(WebUtil.USER_INFO, info);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        permissionAspect.checkPermission(joinPoint);
    }
}
