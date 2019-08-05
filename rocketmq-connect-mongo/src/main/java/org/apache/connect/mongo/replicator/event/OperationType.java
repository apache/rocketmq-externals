package org.apache.connect.mongo.replicator.event;

public enum OperationType {

    INSERT("i"),
    UPDATE("u"),
    DELETE("d"),
    NOOP("n"),
    DBCOMMAND("c"),
    CREATED("created"),
    UNKNOWN("unknown");

    private final String operationStr;

    OperationType(String operationStr) {
        this.operationStr = operationStr;
    }

    public static OperationType getOperationType(String operationStr) {
        for (OperationType operationType : OperationType.values()) {
            if (operationType.operationStr.equals(operationStr)) {
                return operationType;
            }
        }
        return UNKNOWN;
    }

}
