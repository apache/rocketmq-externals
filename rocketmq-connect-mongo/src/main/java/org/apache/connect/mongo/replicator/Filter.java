package org.apache.connect.mongo.replicator;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.MongoReplicatorConfig;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;

import java.util.function.Function;

public class Filter {

    private Function<String, Boolean> dbFilter;
    private Function<String, Boolean> collectionFilter;
    private Function<OperationType, Boolean> noopFilter;


    public Filter(MongoReplicatorConfig mongoReplicatorConfig) {
        if (StringUtils.isNotBlank(mongoReplicatorConfig.getInterestDB())) {
            dbFilter = (dataBaseName) -> {
                if (StringUtils.isBlank(dataBaseName)) {
                    return true;
                }
                String interestDB = mongoReplicatorConfig.getInterestDB();
                String[] db = StringUtils.split(interestDB, ",");
                if (ArrayUtils.contains(db, dataBaseName)) {
                    return true;
                }

                return false;
            };
        } else {
            dbFilter = (dataBaseName) -> true;
        }


        if (StringUtils.isNotBlank(mongoReplicatorConfig.getInterestCollection())) {
            collectionFilter = (collectionName) -> {
                if (StringUtils.isBlank(collectionName)) {
                    return true;
                }

                String interestCollection = mongoReplicatorConfig.getInterestCollection();
                String[] coll = StringUtils.split(interestCollection, ",");
                if (ArrayUtils.contains(coll, collectionName)) {
                    return true;
                }
                return false;
            };
        } else {
            collectionFilter = (collectionName) -> true;
        }

        noopFilter = (opeartionType) -> opeartionType.ordinal() != OperationType.NOOP.ordinal();
    }


    public boolean filterDatabaseName(String dataBaseName) {
        return dbFilter.apply(dataBaseName);
    }


    public boolean filterCollectionName(String collectionName) {
        return collectionFilter.apply(collectionName);
    }

    public boolean filterEvent(ReplicationEvent event) {
        return dbFilter.apply(event.getDatabaseName())
                && collectionFilter.apply(event.getCollectionName())
                && noopFilter.apply(event.getOperationType());
    }

}
