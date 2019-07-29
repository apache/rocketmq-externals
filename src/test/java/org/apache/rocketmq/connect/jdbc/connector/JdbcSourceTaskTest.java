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

package org.apache.rocketmq.connect.jdbc.connector;
import java.util.Collection;
import org.junit.Test;


import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;

public class JdbcSourceTaskTest {


    @Test
    public void test() throws InterruptedException {
        KeyValue kv = new DefaultKeyValue();
        kv.put("jdbcUrl","localhost:3306");
        kv.put("jdbcUsername","root");
        kv.put("jdbcPassword","199812160");
        kv.put("mode","bulk");
        kv.put("rocketmqTopic","JdbcTopic");
        JdbcSourceTask task = new JdbcSourceTask();
        task.start(kv);
            Collection<SourceDataEntry> sourceDataEntry = task.poll();
            System.out.println(sourceDataEntry);

    }
}
