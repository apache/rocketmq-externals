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

package org.apache.connect.mongo;

import org.apache.connect.mongo.replicator.event.OperationType;
import org.junit.Assert;
import org.junit.Test;

public class OperationTypeTest {

    @Test
    public void testGetOperationType() {
        Assert.assertEquals(OperationType.INSERT, OperationType.getOperationType("i"));
        Assert.assertEquals(OperationType.UPDATE, OperationType.getOperationType("u"));
        Assert.assertEquals(OperationType.DELETE, OperationType.getOperationType("d"));
        Assert.assertEquals(OperationType.NOOP, OperationType.getOperationType("n"));
        Assert.assertEquals(OperationType.DB_COMMAND, OperationType.getOperationType("c"));
        Assert.assertEquals(OperationType.CREATED, OperationType.getOperationType("created"));
        Assert.assertEquals(OperationType.UNKNOWN, OperationType.getOperationType("test"));

    }
}
