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

package org.apache.connect.mongo.replicator;

import java.util.Objects;
import org.bson.BsonTimestamp;

public class Position {

    private int timeStamp;
    private int inc;
    private boolean initSync;

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getInc() {
        return inc;
    }

    public void setInc(int inc) {
        this.inc = inc;
    }

    public boolean isInitSync() {
        return initSync;
    }

    public void setInitSync(boolean initSync) {
        this.initSync = initSync;
    }

    public Position() {

    }

    public Position(int timeStamp, int inc, boolean initSync) {
        this.timeStamp = timeStamp;
        this.inc = inc;
        this.initSync = initSync;
    }

    public boolean isValid() {
        return timeStamp > 0 && inc > 0;
    }

    public BsonTimestamp converBsonTimeStamp() {
        return new BsonTimestamp(timeStamp, inc);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Position position = (Position) o;
        return timeStamp == position.timeStamp &&
            inc == position.inc &&
            initSync == position.initSync;
    }

    @Override public int hashCode() {
        return Objects.hash(timeStamp, inc, initSync);
    }
}
