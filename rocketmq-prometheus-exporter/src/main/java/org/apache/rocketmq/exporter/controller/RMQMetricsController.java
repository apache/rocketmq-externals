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
package org.apache.rocketmq.exporter.controller;


import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;

@RestController
@EnableAutoConfiguration
public class RMQMetricsController {

    private final static Logger log = LoggerFactory.getLogger(RMQMetricsController.class);

    @Resource
    RMQMetricsService metricsService;

    @RequestMapping(value = "${rocketmq.config.webTelemetryPath}")
    @ResponseBody
    private void metrics(HttpServletResponse response) throws IOException {

        StringWriter writer = new StringWriter();
        metricsService.Metrics(writer);

        response.setHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
        response.getOutputStream().print(writer.toString());
    }
}
