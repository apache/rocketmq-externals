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

import org.apache.rocketmq.console.BaseTest;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.support.GlobalExceptionHandler;
import org.apache.rocketmq.console.support.GlobalRestfulResponseBodyAdvice;
import org.apache.rocketmq.console.util.MyPrintingResultHandler;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseControllerTest extends BaseTest {

    protected MockMvc mockMvc;

    @Mock
    protected MQAdminExt mqAdminExt;

    @Mock
    protected RMQConfigure configure;

    protected MockHttpServletRequestBuilder requestBuilder = null;

    protected ResultActions perform;

    protected abstract Object getTestController();

    @Before
    public void beforeInit() throws Exception {
        MockitoAnnotations.initMocks(this);
        autoInjection();
        createMockMvc();
    }

    protected void mockRmqConfigure() {
        when(configure.getAccessKey()).thenReturn("12345678");
        when(configure.getSecretKey()).thenReturn("rocketmq");
        when(configure.getNamesrvAddr()).thenReturn("127.0.0.1:9876");
        when(configure.isACLEnabled()).thenReturn(true);
        when(configure.isUseTLS()).thenReturn(false);
    }

    protected ResultActions performOkExpect(ResultActions perform) throws Exception {
        return perform.andExpect(status().isOk())
            .andExpect(jsonPath("$").exists())
            .andExpect(jsonPath("$").isMap())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.errMsg").doesNotExist());
    }

    protected ResultActions performErrorExpect(ResultActions perform) throws Exception {
        return perform.andExpect(status().isOk())
            .andExpect(jsonPath("$").exists())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.status").value(-1))
            .andExpect(jsonPath("$.errMsg").isNotEmpty());
    }

    protected MockMvc createMockMvc() {
        MockMvc innerMockMvc = MockMvcBuilders.standaloneSetup(getTestController())
            .alwaysDo(MyPrintingResultHandler.me())
            .setControllerAdvice(new GlobalExceptionHandler(), new GlobalRestfulResponseBodyAdvice())
            .build();
        this.mockMvc = innerMockMvc;
        return innerMockMvc;
    }

}
