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

package org.apache.rocketmq.console.config;

import org.apache.rocketmq.console.interceptor.AuthInterceptor;
import org.apache.rocketmq.console.model.UserInfo;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class AuthWebMVCConfigurerAdapter extends WebMvcConfigurerAdapter {
    @Autowired
    @Qualifier("authInterceptor")
    private AuthInterceptor authInterceptor;

    @Resource
    RMQConfigure configure;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (configure.isLoginRequired()) {
            registry.addInterceptor(authInterceptor).excludePathPatterns("/error", "/user/guide/**", "/login/**");
        }
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(MethodParameter methodParameter) {
                return methodParameter.getParameterType().isAssignableFrom(UserInfo.class);
            }

            @Override
            public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
                                          NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
                UserInfo userInfo = (UserInfo) WebUtil.getValueFromSession((HttpServletRequest) nativeWebRequest.getNativeRequest(),
                        UserInfo.USER_INFO);
                if (userInfo != null) {
                    return userInfo;
                }
                throw new MissingServletRequestPartException(UserInfo.USER_INFO);
            }
        });

        super.addArgumentResolvers(argumentResolvers);  //REVIEW ME
    }
}
