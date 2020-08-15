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

package org.apache.connect.mongo.replicator.event;

import org.apache.commons.lang3.StringUtils;

public enum OperationType {

    INSERT("i"),
    UPDATE("u"),
    DELETE("d"),
    NOOP("n"),
    DB_COMMAND("c"),
    CREATED("created"),
    UNKNOWN("unknown");

    private final String operation;

    OperationType(String operation) {
        this.operation = operation;
    }

    public static OperationType getOperationType(String operation) {

        if (StringUtils.isEmpty(operation)) {
            return UNKNOWN;
        }

        switch (operation) {
            case "i":
                return INSERT;
            case "u":
                return UPDATE;
            case "d":
                return DELETE;
            case "n":
                return NOOP;
            case "c":
                return DB_COMMAND;
            case "created":
                return CREATED;
            default:
                return UNKNOWN;
        }
    }

}
