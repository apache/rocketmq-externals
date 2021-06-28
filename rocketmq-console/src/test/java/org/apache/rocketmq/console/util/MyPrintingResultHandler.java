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

package org.apache.rocketmq.console.util;

import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

public class MyPrintingResultHandler extends PrintingResultHandler {
    public static MyPrintingResultHandler me() {
        return new MyPrintingResultHandler();
    }

    private static ResultValuePrinter printer;

    static {
        printer = new ResultValuePrinter() {
            private boolean isPrint = true;

            @Override
            public void printHeading(String heading) {
                if (isPrint) {
                    System.out.println();
                    System.out.println(String.format("%s:", heading));
                }
            }

            @Override
            public void printValue(String label, Object value) {
                if (isPrint) {
                    if (value != null && value.getClass().isArray()) {
                        value = CollectionUtils.arrayToList(value);
                    }
                    System.out.println(String.format("%17s = %s", label, value));
                }
            }
        };
    }

    public MyPrintingResultHandler() {
        super(printer);
    }

    @Override
    protected void printResponse(MockHttpServletResponse response) throws Exception {
        this.getPrinter().printValue("Status", response.getStatus());
        this.getPrinter().printValue("Error message", response.getErrorMessage());
        this.getPrinter().printValue("Headers", getResponseHeaders(response));
        this.getPrinter().printValue("Content type", response.getContentType());
        String disposition = response.getHeader("Content-Disposition");
        if (StringUtils.isBlank(disposition)) {
            this.getPrinter().printValue("Body", response.getContentAsString());
        } else {
            this.getPrinter().printValue("Body", "this a file");
        }
        this.getPrinter().printValue("Forwarded URL", response.getForwardedUrl());
        this.getPrinter().printValue("Redirected URL", response.getRedirectedUrl());
        Method method = ReflectionUtils.findMethod(getClass(), "printCookies");
        if (method != null) {
            ReflectionUtils.makeAccessible(method);
            method.invoke(this, response.getCookies());
        }
    }
}
