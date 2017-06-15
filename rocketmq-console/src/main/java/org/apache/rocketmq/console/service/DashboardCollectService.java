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
package org.apache.rocketmq.console.service;

import com.google.common.cache.LoadingCache;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface DashboardCollectService {
    // todo just move the task to org.apache.rocketmq.console.task.DashboardCollectTask
    // the code can be reconstruct
    LoadingCache<String, List<String>> getBrokerMap();

    LoadingCache<String, List<String>> getTopicMap();

    Map<String, List<String>> jsonDataFile2map(File file);

    Map<String, List<String>> getBrokerCache(String date);

    Map<String, List<String>> getTopicCache(String date);
}
