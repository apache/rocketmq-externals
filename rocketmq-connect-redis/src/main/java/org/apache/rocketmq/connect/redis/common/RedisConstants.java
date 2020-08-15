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

public class RedisConstants {
    public static final String DB_INDEX = "dbIndex";
    public static final String POSITION = "nextPosition";

    public static final String NX = "NX";
    public static final String PX = "PX";
    public static final String EX = "EX";
    public static final String XX = "XX";
    public static final String CH = "CH";
    public static final String INCR = "INCR";
    public static final String IDLE = "IDLE";
    public static final String TIME = "TIME";
    public static final String KEEPTTL = "KEEPTTL";
    public static final String RETRYCOUNT = "RETRYCOUNT";
    public static final String FORCE = "FORCE";
    public static final String JUSTID = "JUSTID";

    public static final String REDIS_INFO_REPLICATION = "Replication";
    public static final String REDIS_INFO_REPLICATION_MASTER_REPLID = "master_replid";
    public static final String REDIS_INFO_REPLICATION_MASTER_REPL_OFFSET = "master_repl_offset";

    public static final String ALL_COMMAND = "*";

    public static final Integer EVENT_COMMIT_RETRY_TIMES = 5;
    public static final Long EVENT_COMMIT_RETRY_INTERVAL = 100L;

}
