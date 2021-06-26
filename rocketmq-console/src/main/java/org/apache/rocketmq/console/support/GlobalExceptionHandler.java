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

package org.apache.rocketmq.console.support;

import javax.servlet.http.HttpServletRequest;
import org.apache.rocketmq.console.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice(basePackages = "org.apache.rocketmq.console")
public class GlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public JsonResult<Object> jsonErrorHandler(HttpServletRequest req, Exception ex) throws Exception {
        JsonResult<Object> value = null;
        if (ex != null) {
            logger.error("op=global_exception_handler_print_error", ex);
            if (ex instanceof ServiceException) {
                value = new JsonResult<Object>(((ServiceException) ex).getCode(), ex.getMessage());
            }
            else {
                value = new JsonResult<Object>(-1, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
        }
        return value;
    }
}