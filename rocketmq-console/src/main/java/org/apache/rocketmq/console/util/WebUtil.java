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

package org.apache.rocketmq.console.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.UserInfo;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class WebUtil {
    public static final String USER_INFO = "userInfo";
    public static final String USER_NAME = "username";
    public static final String NEED_LOGIN = "needLogin";

    /**
     * Obtain ServletRequest header value
     *
     * @param request
     * @param name
     * @return
     */
    public static String getHeaderValue(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null) {
            return null;
        }
        return v.trim();
    }

    /**
     * Fetch request ip address
     *
     * @param request
     * @return
     */
    public static String getIp(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        String addr = getHeaderValue(req, "X-Forwarded-For");
        if (StringUtils.isNotEmpty(addr) && addr.contains(",")) {
            addr = addr.split(",")[0];
        }
        if (StringUtils.isEmpty(addr)) {
            addr = getHeaderValue(req, "X-Real-IP");
        }
        if (StringUtils.isEmpty(addr)) {
            addr = req.getRemoteAddr();
        }
        return addr;
    }

    public static void redirect(HttpServletResponse response, HttpServletRequest request, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * Obtain the full url path
     *
     * @param request
     * @return
     */
    public static String getUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            url += "?" + request.getQueryString();
        }
        return url;
    }

    /**
     * Write content to front-page/response
     *
     * @param response
     * @param result
     * @throws IOException
     */
    public static void print(HttpServletResponse response, String result) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(result);
        out.flush();
        out.close();
    }

    public static Object getValueFromSession(HttpServletRequest request, String key) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            return  session.getAttribute(key);
        }

        return null;
    }

    public static UserInfo setLoginInfo(HttpServletRequest request, HttpServletResponse response, User user) {
        String ip = WebUtil.getIp(request);
        UserInfo userInfo = new UserInfo();
        userInfo.setIp(ip);
        userInfo.setLoginTime(System.currentTimeMillis());

        userInfo.setUser(user);

        return userInfo;
    }

    public static void removeSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate();
    }

    public static void setSessionValue(HttpServletRequest request, String key, Object value) {
        HttpSession session = request.getSession();
        session.setAttribute(key, value);
    }

    public static String getSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }
}
