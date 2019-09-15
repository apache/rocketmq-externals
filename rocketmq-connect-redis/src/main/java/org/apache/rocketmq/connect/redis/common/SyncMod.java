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

package org.apache.rocketmq.connect.redis.common;

public enum SyncMod {
    /**
     * sync data from the last offset.
     */
    LAST_OFFSET,
    /**
     * sync data from the custom offset, offset for - 1 synchronization of all existing data.
     */
    CUSTOM_OFFSET,
    /**
     * sync data form last offset, ignore the position from connector runtime.
     */
    LAST_OFFSET_FORCE,
    /**
     * sync data from the custom offset, ignore the position from connector runtime.
     */
    CUSTOM_OFFSET_FORCE
}
