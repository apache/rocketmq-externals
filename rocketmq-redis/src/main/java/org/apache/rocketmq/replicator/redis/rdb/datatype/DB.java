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
public class DB implements Serializable {
    private long dbNumber;
    /* rdb version 7 */
    private Long dbsize = null;
    /* rdb version 7 */
    private Long expires = null;

    public DB() {
    }

    public DB(long dbNumber) {
        this.dbNumber = dbNumber;
    }

    public DB(long dbNumber, long dbsize, long expires) {
        this.dbNumber = dbNumber;
        this.dbsize = dbsize;
        this.expires = expires;
    }

    public long getDbNumber() {
        return dbNumber;
    }

    public void setDbNumber(long dbNumber) {
        this.dbNumber = dbNumber;
    }

    public Long getDbsize() {
        return dbsize;
    }

    public void setDbsize(Long dbsize) {
        this.dbsize = dbsize;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    @Override
    public String toString() {
        return "DB{" +
                "dbNumber=" + dbNumber +
                ", dbsize=" + dbsize +
                ", expires=" + expires +
                '}';
    }
}
