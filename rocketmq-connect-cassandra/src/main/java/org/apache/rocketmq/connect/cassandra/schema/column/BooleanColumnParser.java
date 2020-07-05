package org.apache.rocketmq.connect.cassandra.schema.column;

public class BooleanColumnParser extends ColumnParser{
    @Override
    public Object getValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return value;
        }

        return value;
    }
}
