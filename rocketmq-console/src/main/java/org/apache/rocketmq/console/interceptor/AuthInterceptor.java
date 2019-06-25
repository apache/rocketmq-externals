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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.console.interceptor;

import org.apache.rocketmq.console.model.UserInfo;
import org.apache.rocketmq.console.service.LoginService;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public static final String ADMIN_PATH = "/topic/create.*|/topic/delete.*";

    @Autowired
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        boolean ok = loginService.login(request, response);
        if (!ok) {
            return false;
        }
        if (request.getServletPath().matches(ADMIN_PATH)) {
            UserInfo userInfo = WebUtil.getUser(request);
            if (userInfo != null && userInfo.getUser().getType() != 1) {
                throw new RuntimeException("Please contact the administrator for operation, insufficient permissions");
            }
        }
        return true;
    }
}
