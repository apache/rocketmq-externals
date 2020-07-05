package org.apache.rocketmq.connect.cassandra.schema.column;

import io.openmessaging.connector.api.data.FieldType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ColumnParser {

    // TODO we haven't checked all the
    public static ColumnParser getColumnParser(String dataType, String colType, String charset) {

        switch (dataType) {
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "varint":
            case "int":
                return new IntColumnParser(dataType, colType);
            case "bigint":
                return new BigIntColumnParser(colType);
            case "tinytext":
            case "text":
            case "mediumtext":
            case "longtext":
            case "varchar":
            case "ascii":
            case "char":
                return new StringColumnParser(charset);
            case "date":
            case "datetime":
            case "timestamp":
                return new DateTimeColumnParser();
            case "time":
                return new TimeColumnParser();
            case "year":
                return new YearColumnParser();
            case "enum":
                return new EnumColumnParser(colType);
            case "set":
                return new SetColumnParser(colType);
            case "boolean":
                return new BooleanColumnParser();
            default:
                return new DefaultColumnParser();
        }
    }

    public static FieldType mapConnectorFieldType(String dataType) {

        switch (dataType) {
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
                return FieldType.INT32;
            case "bigint":
                return FieldType.BIG_INTEGER;
            case "tinytext":
            case "text":
            case "mediumtext":
            case "longtext":
            case "varchar":
            case "char":
                return FieldType.STRING;
            case "date":
            case "datetime":
            case "timestamp":
            case "time":
            case "year":
                return FieldType.DATETIME;
            case "enum":
                return null;
            case "set":
                return null;
            default:
                return FieldType.BYTES;
        }
    }

    public static String[] extractEnumValues(String colType) {
        String[] enumValues = {};
        Matcher matcher = Pattern.compile("(enum|set)\\((.*)\\)").matcher(colType);
        if (matcher.matches()) {
            enumValues = matcher.group(2).replace("'", "").split(",");
        }

        return enumValues;
    }

    public abstract Object getValue(Object value);

}
