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

package org.apache.rocketmq.replicator.redis.cmd.impl;

import org.apache.rocketmq.replicator.redis.cmd.Command;

import java.util.Arrays;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ZUnionStoreCommand implements Command {
    private String destination;
    private int numkeys;
    private String[] keys;
    private double[] weights;
    private AggregateType aggregateType;

    public ZUnionStoreCommand() {
    }

    public ZUnionStoreCommand(String destination, int numkeys, String[] keys, double[] weights, AggregateType aggregateType) {
        this.destination = destination;
        this.numkeys = numkeys;
        this.keys = keys;
        this.weights = weights;
        this.aggregateType = aggregateType;
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
