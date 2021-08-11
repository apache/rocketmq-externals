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

import java.util.HashSet;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.console.interceptor.AuthInterceptor;
import org.apache.rocketmq.console.service.impl.LoginServiceImpl;
import org.apache.rocketmq.console.service.impl.ProducerServiceImpl;
import org.apache.rocketmq.console.support.GlobalExceptionHandler;
import org.apache.rocketmq.console.support.GlobalRestfulResponseBodyAdvice;
import org.apache.rocketmq.console.util.MyPrintingResultHandler;
import org.apache.rocketmq.console.util.WebUtil;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProducerControllerTest extends BaseControllerTest {

    @InjectMocks
    private ProducerController producerController;

    @Spy
    private ProducerServiceImpl producerService;

    @Override protected MockMvc createMockMvc() {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(authInterceptor, "loginService", new LoginServiceImpl());
        MockMvc innerMockMvc = MockMvcBuilders.standaloneSetup(getTestController())
            .addInterceptors(authInterceptor)
            .alwaysDo(MyPrintingResultHandler.me())
            .setControllerAdvice(new GlobalExceptionHandler(), new GlobalRestfulResponseBodyAdvice())
            .build();
        this.mockMvc = innerMockMvc;
        return innerMockMvc;
    }

    @Before
    public void init(){
        createMockMvc();
    }

    @Test
    public void testProducerConnection() throws Exception {
        final String url = "/producer/producerConnection.query";
        // user not login， request will redirect
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("producerGroup", "producer_test")
            .param("topic", "topic_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().is3xxRedirection());
        // user login
        {
            ProducerConnection producerConnection = new ProducerConnection();
            HashSet<Connection> connections = new HashSet<>();
            Connection conn = new Connection();
            conn.setClientAddr("127.0.0.1");
            conn.setClientId("clientId");
            conn.setVersion(LanguageCode.JAVA.getCode());
            connections.add(conn);
            producerConnection.setConnectionSet(connections);
            when(mqAdminExt.examineProducerConnectionInfo(anyString(), anyString()))
                .thenThrow(new MQClientException("Not found the producer group connection", null))
                .thenReturn(producerConnection);
        }
        // 1、no producer connection
        requestBuilder.sessionAttr(WebUtil.USER_NAME, "admin");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$").exists())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.status").value(-1))
            .andExpect(jsonPath("$.errMsg").isNotEmpty());

        // 2、producer connection exist
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data.connectionSet").isArray())
            .andExpect(jsonPath("$.data.connectionSet", hasSize(1)))
            .andExpect(jsonPath("$.data.connectionSet[0].clientAddr").value("127.0.0.1"))
            .andExpect(jsonPath("$.data.connectionSet[0].clientId").value("clientId"));
    }

    @Override
    protected Object getTestController() {
        return producerController;
    }
}
