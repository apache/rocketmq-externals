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

package org.apache.rocketmq.jms.util;

import java.util.Map;
import org.junit.Test;

public class URISpecParserTest {

    @Test
    public void parseURI_NormalTest() {
        Map<String, String> result = URISpecParser.parseURI("rocketmq://localhost");
        System.out.println(result);

        result = URISpecParser
            .parseURI("rocketmq://xxx?appId=test&consumerId=testGroup");
        System.out.println(result);

        result = URISpecParser.parseURI("rocketmq:!@#$%^&*()//localhost?appId=test!@#$%^&*()");
        System.out.println(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseURI_AbnormalTest() {
        URISpecParser.parseURI("metaq3://localhost?appId=test");
    }

}