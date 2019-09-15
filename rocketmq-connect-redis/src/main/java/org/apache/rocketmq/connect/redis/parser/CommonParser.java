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

package org.apache.rocketmq.connect.redis.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.common.Options;

public class CommonParser {

    public static KVEntry commonEvalParser(KVEntry builder, String[] args) {
        int numberKeys = Integer.parseInt(args[0]);
        if (numberKeys > 0) {
            List<List<String>> values = new ArrayList<>();

            String[] keys_arr = new String[numberKeys];
            System.arraycopy(args, 1, keys_arr, 0, numberKeys);
            List<String> keysList = new ArrayList<>(numberKeys);
            keysList.addAll(Arrays.asList(keys_arr));
            values.add(keysList);

            if (numberKeys + 1 < args.length) {
                String[] args_arr = new String[args.length - numberKeys - 1];
                System.arraycopy(args, numberKeys + 1, args_arr, 0, args.length - numberKeys - 1);
                List<String> argsList = new ArrayList<>(numberKeys);
                argsList.addAll(Arrays.asList(args_arr));
                values.add(argsList);
            }

            builder.value(values);
        }

        return builder;
    }

    public static KVEntry commonMapParser(KVEntry builder, String[] args) {
        Map<String, String> kvMap = new HashMap<>(args.length / 2);
        for (int i = 0, j = i + 1; j < args.length; i++, i++, j++, j++) {
            kvMap.put(args[i], args[j]);
        }
        builder.value(kvMap);
        return builder;
    }

    public static KVEntry noCommandKeyMapParser(KVEntry builder, String[] args) {
        String firKey = builder.getKey();
        Map<String, String> kvMap = new HashMap<>((args.length + 1) / 2);
        kvMap.put(firKey, args[0]);
        for (int i = 1, j = i + 1; j < args.length; i++, i++, j++, j++) {
            kvMap.put(args[i], args[j]);
        }
        builder.value(kvMap);
        return builder;
    }

    public static KVEntry noCommandKeyArrayParser(KVEntry builder, String[] args) {
        List<String> keys = new ArrayList<>(args.length + 1);
        keys.add(builder.getKey());
        keys.addAll(Arrays.stream(args).collect(Collectors.toCollection(ArrayList::new)));
        return builder.value(keys);
    }

    public static KVEntry commonZStoreHandler(KVEntry builder, String[] args) {
        int numberKey = Integer.parseInt(args[0]);
        Map<String, String> map = new HashMap<>();
        String[] keys = new String[numberKey];
        System.arraycopy(args, 1, keys, 0, numberKey);
        int idx = numberKey + 1;
        if (idx < args.length) {
            if (Options.REDIS_WEIGHTS.name().equals(args[idx].toUpperCase())) {
                String[] values = new String[numberKey];
                System.arraycopy(args, idx + 1, values, 0, numberKey);
                for (int i = 0; i < numberKey; i++) {
                    map.put(keys[i], values[i]);
                }
                builder.value(map);
                idx = idx + numberKey + 1;
            } else {
                for (int i = 0; i < numberKey; i++) {
                    map.put(keys[i], Integer.toString(1));
                }
                builder.value(map);
            }
            if (idx < args.length && Options.REDIS_AGGREGATE.name().equals(args[idx].toUpperCase())) {
                builder.param(Options.REDIS_AGGREGATE, args[idx + 1]);
            }
        } else {
            for (int i = 0; i < numberKey; i++) {
                map.put(keys[i], Integer.toString(1));
            }
            builder.value(map);
        }
        return builder;
    }

}
