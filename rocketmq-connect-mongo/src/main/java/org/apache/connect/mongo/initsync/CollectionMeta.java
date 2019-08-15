package org.apache.connect.mongo.initsync;

public class CollectionMeta {

    private String databaseName;
    private String collectionName;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public CollectionMeta(String databaseName, String collectionName) {

        this.databaseName = databaseName;
        this.collectionName = collectionName;
    }

    public String getNameSpace() {
        return databaseName + "." + collectionName;
    }

    @Override
    public String toString() {
        return "CollectionMeta{" +
            "databaseName='" + databaseName + '\'' +
            ", collectionName='" + collectionName + '\'' +
            '}';
    }
}
