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

package org.apache.rocketmq.console.support;

import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.apache.rocketmq.console.util.JsonUtil;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

public class JsonBodyExceptionResolver extends SimpleMappingExceptionResolver {
    private Logger logger = LoggerFactory.getLogger(JsonBodyExceptionResolver.class);

    public JsonBodyExceptionResolver() {
        this.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
        Object theHandler, Exception ex) {
        HandlerMethod handler = (HandlerMethod) theHandler;
        if (handler == null) {
            return null;
        }
        Method method = handler.getMethod();
        if (method.isAnnotationPresent(JsonBody.class) && ex != null) {
            logger.error("server is error", ex);
            Object value = null;
            if (ex instanceof ServiceException) {
                value = new JsonResult<Object>(((ServiceException) ex).getCode(), ex.getMessage());
            }
            else {
                value = new JsonResult<Object>(-1, ex.getMessage());
            }
            try {
                JsonUtil.writeValue(response.getWriter(), value);
            }
            catch (IOException e) {
                Throwables.propagateIfPossible(e);
            }
            return ModelAndViewResolver.UNRESOLVED;
        }
        return null;
    }
}
