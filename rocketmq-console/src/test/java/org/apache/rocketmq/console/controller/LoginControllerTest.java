/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.console.controller;

import java.lang.reflect.Field;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.service.impl.UserServiceImpl;
import org.apache.rocketmq.console.util.WebUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LoginControllerTest extends BaseControllerTest {

    @InjectMocks
    private LoginController loginController;

    @Spy
    private UserServiceImpl userService;

    private String contextPath = "/rocketmq-console";

    @Before
    public void init() {
        super.mockRmqConfigure();
        when(configure.isLoginRequired()).thenReturn(true);
        when(configure.getRocketMqConsoleDataPath()).thenReturn("");
        Field contextPathField = ReflectionUtils.findField(LoginController.class, "contextPath");
        ReflectionUtils.makeAccessible(contextPathField);
        ReflectionUtils.setField(contextPathField, loginController, contextPath);
    }

    @Test
    public void testCheck() throws Exception {
        final String url = "/login/check.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.sessionAttr(WebUtil.USER_NAME, "admin");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.logined").value(true))
            .andExpect(jsonPath("$.data.loginRequired").value(true));

    }

    @Test
    public void testLogin() throws Exception {
        final String url = "/login/login.do";
        final String username = "admin";
        final String rightPwd = "admin";
        final String wrongPwd = "rocketmq";
        {
            UserServiceImpl.FileBasedUserInfoStore store
                = new UserServiceImpl.FileBasedUserInfoStore(configure);
            User user = store.queryByName(username);
            Assert.assertNotNull(user);
            Assert.assertEquals(user.getPassword(), rightPwd);
            ReflectionTestUtils.setField(userService, "fileBasedUserInfoStore", store);
        }

        // 1、login fail
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("username", username)
            .param("password", wrongPwd);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.status").value(-1))
            .andExpect(jsonPath("$.errMsg").value("Bad username or password!"));

        // 2、login success
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("username", username)
            .param("password", rightPwd);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(contextPath));

}

    @Test
    public void testLogout() throws Exception {
        final String url = "/login/logout.do";
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.sessionAttr(WebUtil.USER_NAME, "root");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(contextPath));
    }

    @Override protected Object getTestController() {
        return loginController;
    }
}
