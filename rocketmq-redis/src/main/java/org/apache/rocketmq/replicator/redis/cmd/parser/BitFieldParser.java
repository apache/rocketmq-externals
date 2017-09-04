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

package org.apache.rocketmq.replicator.redis.cmd.parser;

import org.apache.rocketmq.replicator.redis.cmd.CommandParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.replicator.redis.cmd.impl.BitFieldCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.GetTypeOffset;
import org.apache.rocketmq.replicator.redis.cmd.impl.IncrByTypeOffsetIncrement;
import org.apache.rocketmq.replicator.redis.cmd.impl.OverFlow;
import org.apache.rocketmq.replicator.redis.cmd.impl.OverFlowType;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetTypeOffsetValue;
import org.apache.rocketmq.replicator.redis.cmd.impl.Statement;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class BitFieldParser implements CommandParser<BitFieldCommand> {

    @Override
    public BitFieldCommand parse(Object[] command) {
        int idx = 1;
        String key = (String) command[idx++];
        List<Statement> list = new ArrayList<>();
        if (idx < command.length) {
            String token;
            do {
                idx = parseStatement(idx, command, list);
                if (idx >= command.length) break;
                token = (String) command[idx];
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
                if (idx >= command.length) break;
            } while (((String) command[idx]).equalsIgnoreCase("OVERFLOW"));
        }

        return new BitFieldCommand(key, list, overFlowList);
    }

    private int parseOverFlow(int i, Object[] params, OverFlow overFlow) {
        int idx = i;
        accept((String) params[idx++], "OVERFLOW");
        OverFlowType overFlowType = null;
        String keyWord = (String) params[idx++];
        if (keyWord.equalsIgnoreCase("WRAP")) {
            overFlowType = OverFlowType.WRAP;
        } else if (keyWord.equalsIgnoreCase("SAT")) {
            overFlowType = OverFlowType.SAT;
        } else if (keyWord.equalsIgnoreCase("FAIL")) {
            overFlowType = OverFlowType.FAIL;
        } else {
            throw new AssertionError("parse [BITFIELD] command error." + keyWord);
        }
        List<Statement> list = new ArrayList<>();
        if (idx < params.length) {
            String token;
            do {
                idx = parseStatement(idx, params, list);
                if (idx >= params.length) break;
                token = (String) params[idx];
            }
            while (token != null && (token.equalsIgnoreCase("GET") || token.equalsIgnoreCase("SET") || token.equalsIgnoreCase("INCRBY")));
        }
        overFlow.setOverFlowType(overFlowType);
        overFlow.setStatements(list);
        return idx;
    }

    private int parseStatement(int i, Object[] params, List<Statement> list) {
        int idx = i;
        String keyWord = (String) params[idx++];
        Statement statement = null;
        if (keyWord.equalsIgnoreCase("GET")) {
            GetTypeOffset getTypeOffset = new GetTypeOffset();
            idx = parseGet(idx - 1, params, getTypeOffset);
            statement = getTypeOffset;
        } else if (keyWord.equalsIgnoreCase("SET")) {
            SetTypeOffsetValue setTypeOffsetValue = new SetTypeOffsetValue();
            idx = parseSet(idx - 1, params, setTypeOffsetValue);
            statement = setTypeOffsetValue;
        } else if (keyWord.equalsIgnoreCase("INCRBY")) {
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
        accept((String) params[idx++], "INCRBY");
        String type = (String) params[idx++];
        String offset = (String) params[idx++];
        long increment = new BigDecimal((String) params[idx++]).longValueExact();
        incrByTypeOffsetIncrement.setType(type);
        incrByTypeOffsetIncrement.setOffset(offset);
        incrByTypeOffsetIncrement.setIncrement(increment);
        return idx;
    }

    private int parseSet(int i, Object[] params, SetTypeOffsetValue setTypeOffsetValue) {
        int idx = i;
        accept((String) params[idx++], "SET");
        String type = (String) params[idx++];
        String offset = (String) params[idx++];
        long value = new BigDecimal((String) params[idx++]).longValueExact();
        setTypeOffsetValue.setType(type);
        setTypeOffsetValue.setOffset(offset);
        setTypeOffsetValue.setValue(value);
        return idx;
    }

    private int parseGet(int i, Object[] params, GetTypeOffset getTypeOffset) {
        int idx = i;
        accept((String) params[idx++], "GET");
        String type = (String) params[idx++];
        String offset = (String) params[idx++];
        getTypeOffset.setType(type);
        getTypeOffset.setOffset(offset);
        return idx;
    }

    private void accept(String actual, String expect) {
        if (actual.equalsIgnoreCase(expect)) return;
        throw new AssertionError("expect " + expect + " but actual " + actual);
    }

}
