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

package org.apache.rocketmq.connect.runtime.store;

import java.util.HashMap;
import org.apache.rocketmq.connect.runtime.converter.ByteConverter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileBaseKeyValueStoreTest {

    @Test
    public void testFileBaseKeyValueStore() {
        FileBaseKeyValueStore<byte[], byte[]> fbkvs = new FileBaseKeyValueStore<>(
            "target/unit_test_store/testFileBaseKeyValueStore/000",
            new ByteConverter(),
            new ByteConverter()
        );

        fbkvs.data = new HashMap<>();
        fbkvs.data.put("test_key".getBytes(), "test_value".getBytes());
        fbkvs.persist();
        boolean flag = fbkvs.load();
        assertThat(flag).isEqualTo(true);
    }
}