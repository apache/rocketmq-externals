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

public class ZUnionStoreCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String destination;
    private int numkeys;
    private String[] keys;
    private double[] weights;
    private AggregateType aggregateType;
    private byte[] rawDestination;
    private byte[][] rawKeys;

    public ZUnionStoreCommand() {
    }

    public ZUnionStoreCommand(String destination, int numkeys, String[] keys, double[] weights,
        AggregateType aggregateType) {
        this(destination, numkeys, keys, weights, aggregateType, null, null);
    }

    public ZUnionStoreCommand(String destination, int numkeys, String[] keys, double[] weights,
        AggregateType aggregateType, byte[] rawDestination, byte[][] rawKeys) {
        this.destination = destination;
        this.numkeys = numkeys;
        this.keys = keys;
        this.weights = weights;
        this.aggregateType = aggregateType;
        this.rawDestination = rawDestination;
        this.rawKeys = rawKeys;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getNumkeys() {
        return numkeys;
    }

    public void setNumkeys(int numkeys) {
        this.numkeys = numkeys;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public AggregateType getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(AggregateType aggregateType) {
        this.aggregateType = aggregateType;
    }

    public byte[] getRawDestination() {
        return rawDestination;
    }

    public void setRawDestination(byte[] rawDestination) {
        this.rawDestination = rawDestination;
    }

    public byte[][] getRawKeys() {
        return rawKeys;
    }

    public void setRawKeys(byte[][] rawKeys) {
        this.rawKeys = rawKeys;
    }

    @Override
    public String toString() {
        return "ZUnionStoreCommand{" +
            "destination='" + destination + '\'' +
            ", numkeys=" + numkeys +
            ", keys=" + Arrays.toString(keys) +
            ", weights=" + Arrays.toString(weights) +
            ", aggregateType=" + aggregateType +
            '}';
    }

}
