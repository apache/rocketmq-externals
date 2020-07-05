package org.apache.rocketmq.connect.cassandra.schema;

import org.apache.rocketmq.connect.cassandra.schema.column.ColumnParser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Table {

    private String database;
    private String name;
    private List<String> colList = new LinkedList<>();
    private List<ColumnParser> parserList = new LinkedList<>();
    private List<String> rawDataTypeList = new LinkedList<>();
    private List<Object> dataList = new LinkedList<>();
    private Map<String, String> filterMap = new HashMap<>();

    public Table(String database, String table) {
        this.database = database;
        this.name = table;
    }

    public void addCol(String column) {
        colList.add(column);
    }

    public void setParserList(List<ColumnParser> parserList) {
        this.parserList = parserList;
    }

    public void setRawDataTypeList(List<String> rawDataTypeList) {
        this.rawDataTypeList = rawDataTypeList;
    }

    public void addParser(ColumnParser columnParser) {
        parserList.add(columnParser);
    }

    public void addRawDataType(String rawDataType) {
        this.rawDataTypeList.add(rawDataType);
    }

    public List<String> getColList() {
        return colList;
    }

    public List<String> getRawDataTypeList() {
        return rawDataTypeList;
    }

    public String getDatabase() {
        return database;
    }

    public String getName() {
        return name;
    }

    public List<ColumnParser> getParserList() {
        return parserList;
    }

    public List<Object> getDataList() {
        return dataList;
    }

    public void setDataList(List<Object> dataList) {
        this.dataList = dataList;
    }

    public void setColList(List<String> colList) {
        this.colList = colList;
    }

    public Map<String, String> getFilterMap() {
        return filterMap;
    }

    public void setFilterMap(Map<String, String> filterMap) {
        this.filterMap = filterMap;
    }
}