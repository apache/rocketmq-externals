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

package org.apache.rocketmq.connect.runtime.utils;

import java.io.File;
import java.util.Properties;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileAndPropertyUtilTest {

    private String fileDir = System.getProperty("user.home") + File.separator + "fileAndPropertyUtil";
    String str = "testStr";
    String fileName = "testFile";
    String filePath = fileDir + File.separator + fileName;

    @After
    public void destroy() {
        TestUtils.deleteFile(new File(fileDir));
    }

    @Test
    public void testString2File2String() throws Exception {
        FileAndPropertyUtil.string2File(str, filePath);
        String s = FileAndPropertyUtil.file2String(filePath);
        assertEquals(str, s);
    }

    @Test
    public void testString2FileNotSafe() throws Exception {
        FileAndPropertyUtil.string2FileNotSafe(str, filePath);
        String s = FileAndPropertyUtil.file2String(filePath);
        assertEquals(str, s);
    }

    @Test
    public void testProperties2Object() {
        Properties properties = new Properties();
        properties.setProperty("key1", "1");
        properties.setProperty("key2", "2000");
        properties.setProperty("key3", "3.0");
        properties.setProperty("key4", "4");
        class TestObject {
            private String key1;
            private String key2;
            private String key3;
            private String key4;

            public String getKey1() {
                return key1;
            }

            public void setKey1(String key1) {
                this.key1 = key1;
            }

            public String getKey2() {
                return key2;
            }

            public void setKey2(String key2) {
                this.key2 = key2;
            }

            public String getKey3() {
                return key3;
            }

            public void setKey3(String key3) {
                this.key3 = key3;
            }

            public String getKey4() {
                return key4;
            }

            public void setKey4(String key4) {
                this.key4 = key4;
            }
        }
        TestObject testObject = new TestObject();
        FileAndPropertyUtil.properties2Object(properties, testObject);
        assertEquals("1", testObject.getKey1());
        assertEquals("2000", testObject.getKey2());
        assertEquals("3.0", testObject.getKey3());
        assertEquals("4", testObject.getKey4());
    }
}