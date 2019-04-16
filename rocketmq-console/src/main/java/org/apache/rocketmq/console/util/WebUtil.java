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

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class WebUtil {
    public static final String LOGIN_TOKEN = "TOKEN";
    public static final String USER_NAME = "username";

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

    /**
     * Obtain cookie
     *
     * @param request
     * @param name
     * @return
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public static void setCookie(HttpServletResponse response, String key, String value, boolean removeFlag) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        if (removeFlag) {
            cookie.setMaxAge(0);
        }
        response.addCookie(cookie);
    }

    /**
     * Obtain login cookie info
     *
     * @param request
     * @return
     */
    public static String getLoginCookieValue(HttpServletRequest request) {
        Cookie cookie = getCookie(request, LOGIN_TOKEN);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
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
     * Fetch attribute form request
     *
     * @param request
     * @return
     */
    public static void setAttribute(ServletRequest request, String name, Object obj) {
        request.setAttribute(name, obj);
    }

    /**
     * Set attribute to request
     *
     * @param request
     * @return
     */
    public static Object getAttribute(ServletRequest request, String name) {
        return request.getAttribute(name);
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
}
