/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis.rdb.datatype;

import org.apache.rocketmq.replicator.redis.event.Event;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class AuxField implements Event {

    private String auxKey;
    private String auxValue;

    public AuxField() {
    }

    public AuxField(String auxKey, String auxValue) {
        this.auxKey = auxKey;
        this.auxValue = auxValue;
    }

    public String getAuxKey() {
        return auxKey;
    }

    public String getAuxValue() {
        return auxValue;
    }

    public void setAuxKey(String auxKey) {
        this.auxKey = auxKey;
    }

    public void setAuxValue(String auxValue) {
        this.auxValue = auxValue;
    }

    @Override
    public String toString() {
        return "AuxField{" +
                "auxKey='" + auxKey + '\'' +
                ", auxValue='" + auxValue + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuxField auxField = (AuxField) o;

        return auxKey.equals(auxField.auxKey);
    }

    @Override
    public int hashCode() {
        return auxKey.hashCode();
    }
}
