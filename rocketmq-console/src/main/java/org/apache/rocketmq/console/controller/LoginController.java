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

package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.util.CipherHelper;
import org.apache.rocketmq.console.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("/login")
public class LoginController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private CipherHelper cipherHelper;

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index(Map<String, Object> map) {
        return "login";
    }

    @RequestMapping(value = "/check", method = RequestMethod.POST)
    @ResponseBody
    public Object check(@RequestParam("username") String username,
                           @RequestParam(value = "password") String password,
                           HttpServletResponse response) throws Exception {
        logger.info("user:{} login", username);
        User user = userService.queryByUsernameAndPassword(username, password);

        if (user == null) {
            throw new IllegalArgumentException("Bad username or password!");
        } else {
            WebUtil.setCookie(response, WebUtil.LOGIN_TOKEN, cipherHelper.encrypt(username), false);
            WebUtil.setCookie(response, WebUtil.USER_NAME, username, false);
            return Boolean.TRUE;
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public void logout(HttpServletResponse response) {
        WebUtil.setCookie(response, WebUtil.LOGIN_TOKEN,"", true);
        WebUtil.setCookie(response, WebUtil.USER_NAME, "", true);
    }
}
