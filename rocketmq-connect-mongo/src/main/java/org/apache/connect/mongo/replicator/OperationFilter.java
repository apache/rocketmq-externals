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

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.SourceTaskConfig;
import org.apache.connect.mongo.initsync.CollectionMeta;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;

public class OperationFilter {

    private Function<CollectionMeta, Boolean> dbAndCollectionFilter;
    private Map<String, List<String>> interestMap = new HashMap<>();
    private Function<OperationType, Boolean> notNoopFilter;

    public OperationFilter(SourceTaskConfig sourceTaskConfig) {

        String interestDbAndCollection = sourceTaskConfig.getInterestDbAndCollection();

        if (StringUtils.isNotBlank(interestDbAndCollection)) {
            JSONObject jsonObject = JSONObject.parseObject(interestDbAndCollection);
            for (String db : jsonObject.keySet()) {
                List<String> collections = jsonObject.getObject(db, new TypeReference<List<String>>() {
                });
                interestMap.put(db, collections);
            }

        }

        dbAndCollectionFilter = (collectionMeta) -> {
            if (interestMap.size() == 0) {
                return true;
            }
            List<String> collections = interestMap.get(collectionMeta.getDatabaseName());

            if (collections == null || collections.size() == 0) {
                return false;
            }

            if (collections.contains("*") || collections.contains(collectionMeta.getCollectionName())) {
                return true;
            }

            return false;
        };

        notNoopFilter = (operationType) -> !operationType.equals(OperationType.NOOP);
    }

    public boolean filterMeta(CollectionMeta collectionMeta) {
        return dbAndCollectionFilter.apply(collectionMeta);
    }

    public boolean filterEvent(ReplicationEvent event) {
        return dbAndCollectionFilter.apply(new CollectionMeta(event.getDatabaseName(), event.getCollectionName()))
            && notNoopFilter.apply(event.getOperationType());
    }
}
