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

import java.io.Serializable;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ZSetEntry implements Serializable {
    private String element;
    private double score;

    public ZSetEntry() {
    }

    public ZSetEntry(String element, double score) {
        this.element = element;
        this.score = score;
    }

    public String getElement() {
        return element;
    }

    public double getScore() {
        return score;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "[" + element + ", " + score + "]";
    }
}
