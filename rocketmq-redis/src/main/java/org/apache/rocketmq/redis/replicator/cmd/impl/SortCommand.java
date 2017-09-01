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

package org.apache.rocketmq.redis.replicator.cmd.impl;

import java.util.Arrays;
import org.apache.rocketmq.redis.replicator.cmd.Command;

public class SortCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private String byPattern;
    private Limit limit;
    private String[] getPatterns;
    private OrderType order;
    private Boolean alpha;
    private String destination;
    private byte[] rawKey;
    private byte[] rawByPattern;
    private byte[][] rawGetPatterns;
    private byte[] rawDestination;

    public SortCommand() {
    }

    public SortCommand(String key, String byPattern, Limit limit, String[] getPatterns, OrderType order, Boolean alpha,
        String destination) {
        this(key, byPattern, limit, getPatterns, order, alpha, destination, null, null, null, null);
    }

    public SortCommand(String key, String byPattern, Limit limit, String[] getPatterns, OrderType order, Boolean alpha,
        String destination, byte[] rawKey, byte[] rawByPattern, byte[][] rawGetPatterns, byte[] rawDestination) {
        this.key = key;
        this.byPattern = byPattern;
        this.limit = limit;
        this.getPatterns = getPatterns;
        this.order = order;
        this.alpha = alpha;
        this.destination = destination;
        this.rawKey = rawKey;
        this.rawByPattern = rawByPattern;
        this.rawGetPatterns = rawGetPatterns;
        this.rawDestination = rawDestination;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getByPattern() {
        return byPattern;
    }

    public void setByPattern(String byPattern) {
        this.byPattern = byPattern;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    public String[] getGetPatterns() {
        return getPatterns;
    }

    public void setGetPatterns(String[] getPatterns) {
        this.getPatterns = getPatterns;
    }

    public OrderType getOrder() {
        return order;
    }

    public void setOrder(OrderType order) {
        this.order = order;
    }

    public Boolean getAlpha() {
        return alpha;
    }

    public void setAlpha(Boolean alpha) {
        this.alpha = alpha;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawByPattern() {
        return rawByPattern;
    }

    public void setRawByPattern(byte[] rawByPattern) {
        this.rawByPattern = rawByPattern;
    }

    public byte[][] getRawGetPatterns() {
        return rawGetPatterns;
    }

    public void setRawGetPatterns(byte[][] rawGetPatterns) {
        this.rawGetPatterns = rawGetPatterns;
    }

    public byte[] getRawDestination() {
        return rawDestination;
    }

    public void setRawDestination(byte[] rawDestination) {
        this.rawDestination = rawDestination;
    }

    @Override
    public String toString() {
        return "SortCommand{" +
            "key='" + key + '\'' +
            ", byPattern='" + byPattern + '\'' +
            ", limit=" + limit +
            ", getPatterns=" + Arrays.toString(getPatterns) +
            ", order=" + order +
            ", alpha=" + alpha +
            ", destination='" + destination + '\'' +
            '}';
    }
}