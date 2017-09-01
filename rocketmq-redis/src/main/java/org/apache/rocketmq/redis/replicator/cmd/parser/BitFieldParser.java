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

package org.apache.rocketmq.redis.replicator.cmd.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.BitFieldCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.GetTypeOffset;
import org.apache.rocketmq.redis.replicator.cmd.impl.IncrByTypeOffsetIncrement;
import org.apache.rocketmq.redis.replicator.cmd.impl.OverFlow;
import org.apache.rocketmq.redis.replicator.cmd.impl.OverFlowType;
import org.apache.rocketmq.redis.replicator.cmd.impl.SetTypeOffsetValue;
import org.apache.rocketmq.redis.replicator.cmd.impl.Statement;

public class BitFieldParser implements CommandParser<BitFieldCommand> {

    @Override
    public BitFieldCommand parse(Object[] command) {
        int idx = 1;
        String key = CommandParsers.objToString(command[idx]);
        byte[] rawKey = CommandParsers.objToBytes(command[idx]);
        idx++;
        List<Statement> list = new ArrayList<>();
        if (idx < command.length) {
            String token;
            do {
                idx = parseStatement(idx, command, list);
                if (idx >= command.length)
                    break;
                token = CommandParsers.objToString(command[idx]);
            }
            while (token != null && (token.equalsIgnoreCase("GET") || token.equalsIgnoreCase("SET") || token.equalsIgnoreCase("INCRBY")));
        }
        List<OverFlow> overFlowList = null;
        if (idx < command.length) {
            overFlowList = new ArrayList<>();
            do {
                OverFlow overFlow = new OverFlow();
                idx = parseOverFlow(idx, command, overFlow);
                overFlowList.add(overFlow);
                if (idx >= command.length)
                    break;
            }
            while ("OVERFLOW".equalsIgnoreCase(CommandParsers.objToString(command[idx])));
        }

        return new BitFieldCommand(key, list, overFlowList, rawKey);
    }

    private int parseOverFlow(int i, Object[] params, OverFlow overFlow) {
        int idx = i;
        accept(CommandParsers.objToString(params[idx++]), "OVERFLOW");
        OverFlowType overFlowType;
        String keyWord = CommandParsers.objToString(params[idx++]);
        if ("WRAP".equalsIgnoreCase(keyWord)) {
            overFlowType = OverFlowType.WRAP;
        } else if ("SAT".equalsIgnoreCase(keyWord)) {
            overFlowType = OverFlowType.SAT;
        } else if ("FAIL".equalsIgnoreCase(keyWord)) {
            overFlowType = OverFlowType.FAIL;
        } else {
            throw new AssertionError("parse [BITFIELD] command error." + keyWord);
        }
        List<Statement> list = new ArrayList<>();
        if (idx < params.length) {
            String token;
            do {
                idx = parseStatement(idx, params, list);
                if (idx >= params.length)
                    break;
                token = CommandParsers.objToString(params[idx]);
            }
            while (token != null && (token.equalsIgnoreCase("GET") || token.equalsIgnoreCase("SET") || token.equalsIgnoreCase("INCRBY")));
        }
        overFlow.setOverFlowType(overFlowType);
        overFlow.setStatements(list);
        return idx;
    }

    private int parseStatement(int i, Object[] params, List<Statement> list) {
        int idx = i;
        String keyWord = CommandParsers.objToString(params[idx++]);
        Statement statement;
        if ("GET".equalsIgnoreCase(keyWord)) {
            GetTypeOffset getTypeOffset = new GetTypeOffset();
            idx = parseGet(idx - 1, params, getTypeOffset);
            statement = getTypeOffset;
        } else if ("SET".equalsIgnoreCase(keyWord)) {
            SetTypeOffsetValue setTypeOffsetValue = new SetTypeOffsetValue();
            idx = parseSet(idx - 1, params, setTypeOffsetValue);
            statement = setTypeOffsetValue;
        } else if ("INCRBY".equalsIgnoreCase(keyWord)) {
            IncrByTypeOffsetIncrement incrByTypeOffsetIncrement = new IncrByTypeOffsetIncrement();
            idx = parseIncrBy(idx - 1, params, incrByTypeOffsetIncrement);
            statement = incrByTypeOffsetIncrement;
        } else {
            return i;
        }
        list.add(statement);
        return idx;
    }

    private int parseIncrBy(int i, Object[] params, IncrByTypeOffsetIncrement incrByTypeOffsetIncrement) {
        int idx = i;
        accept(CommandParsers.objToString(params[idx++]), "INCRBY");
        String type = CommandParsers.objToString(params[idx]);
        byte[] rawType = CommandParsers.objToBytes(params[idx]);
        idx++;
        String offset = CommandParsers.objToString(params[idx]);
        byte[] rawOffset = CommandParsers.objToBytes(params[idx]);
        idx++;
        long increment = new BigDecimal(CommandParsers.objToString(params[idx++])).longValueExact();
        incrByTypeOffsetIncrement.setType(type);
        incrByTypeOffsetIncrement.setOffset(offset);
        incrByTypeOffsetIncrement.setIncrement(increment);
        incrByTypeOffsetIncrement.setRawType(rawType);
        incrByTypeOffsetIncrement.setRawOffset(rawOffset);
        return idx;
    }

    private int parseSet(int i, Object[] params, SetTypeOffsetValue setTypeOffsetValue) {
        int idx = i;
        accept(CommandParsers.objToString(params[idx++]), "SET");
        String type = CommandParsers.objToString(params[idx]);
        byte[] rawType = CommandParsers.objToBytes(params[idx]);
        idx++;
        String offset = CommandParsers.objToString(params[idx]);
        byte[] rawOffset = CommandParsers.objToBytes(params[idx]);
        idx++;
        long value = new BigDecimal(CommandParsers.objToString(params[idx++])).longValueExact();
        setTypeOffsetValue.setType(type);
        setTypeOffsetValue.setOffset(offset);
        setTypeOffsetValue.setValue(value);
        setTypeOffsetValue.setRawType(rawType);
        setTypeOffsetValue.setRawOffset(rawOffset);
        return idx;
    }

    private int parseGet(int i, Object[] params, GetTypeOffset getTypeOffset) {
        int idx = i;
        accept(CommandParsers.objToString(params[idx++]), "GET");
        String type = CommandParsers.objToString(params[idx]);
        byte[] rawType = CommandParsers.objToBytes(params[idx]);
        idx++;
        String offset = CommandParsers.objToString(params[idx]);
        byte[] rawOffset = CommandParsers.objToBytes(params[idx]);
        idx++;
        getTypeOffset.setType(type);
        getTypeOffset.setOffset(offset);
        getTypeOffset.setRawType(rawType);
        getTypeOffset.setRawOffset(rawOffset);
        return idx;
    }

    private void accept(String actual, String expect) {
        if (actual.equalsIgnoreCase(expect))
            return;
        throw new AssertionError("expect " + expect + " but actual " + actual);
    }

}
