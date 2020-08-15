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

package org.apache.rocketmq.console.service.impl;

import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.service.LoginService;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Service
public class LoginServiceImpl implements LoginService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private RMQConfigure rmqConfigure;

    @Autowired
    private UserService userService;


    @Override
    public boolean login(HttpServletRequest request, HttpServletResponse response) {
        if (WebUtil.getValueFromSession(request, WebUtil.USER_NAME) != null) {
            return true;
        }

        auth(request, response);
        return false;
    }

    protected void auth(HttpServletRequest request, HttpServletResponse response) {
        try {
            String url = WebUtil.getUrl(request);
            try {
                url = URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("url encode:{}", url, e);
            }
            WebUtil.redirect(response, request, "/#/login?redirect=" + url);
        } catch (IOException e) {
            logger.error("redirect err", e);
        }
    }
}
