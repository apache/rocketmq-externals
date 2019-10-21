package org.apache.rocketmq.connect.jdbc.source;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.connect.jdbc.config.Config;
import org.apache.rocketmq.connect.jdbc.schema.Database;
import org.apache.rocketmq.connect.jdbc.schema.Schema;
import org.apache.rocketmq.connect.jdbc.schema.Table;
import org.apache.rocketmq.connect.jdbc.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Querier {

    private final Logger log = LoggerFactory.getLogger(Querier.class); // use concrete subclass
    protected String topicPrefix;
    protected String jdbcUrl;
    private final Queue<Connection> connections = new ConcurrentLinkedQueue<>();
    private Config config;
    private Connection connection;
    private List<Table> list = new LinkedList<>();
    private String mode;
    private Schema schema;

    public Querier(){

    }

    public Querier(Config config, Connection connection) {
        this.config = config;
        this.connection = connection;
        this.schema = new Schema(connection);
    }

    /**
     * @return the config
     */
    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

//    public String getMode() {
//        return mode;
//    }
//
//    public void setMode(String mode) {
//        this.mode = mode;
//    }

    public List<Table> getList() {
        return list;
    }

//    public void setList(List<Table> list) {
//        this.list = list;
//    }

    protected PreparedStatement createDBPreparedStatement(Connection db) throws SQLException {

        String SQL = "select table_name,column_name,data_type,column_type,character_set_name "
                + "from information_schema.columns " + "where table_schema = jdbc_db order by ORDINAL_POSITION";

        log.trace("Creating a PreparedStatement '{}'", SQL);
        PreparedStatement stmt = db.prepareStatement(SQL);
        return stmt;

    }

    protected PreparedStatement createPreparedStatement(Connection db, String string) throws SQLException {
        String query = "select * from " + string;
        log.trace("Creating a PreparedStatement '{}'", query);
        PreparedStatement stmt = db.prepareStatement(query);
        return stmt;

    }

    protected ResultSet executeQuery(PreparedStatement stmt) throws SQLException {
        return stmt.executeQuery();
    }

    public void poll()  {
        try {
            PreparedStatement stmt;
            String query = "select * from ";
            LinkedList<Table> tableLinkedList = new LinkedList<>();
            for (Map.Entry<String, Database> entry : schema.getDbMap().entrySet()) {
                String db = entry.getKey();
                Iterator<Map.Entry<String, Table>> iterator = entry.getValue().getTableMap().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Table> tableEntry = iterator.next();
                    String tb = tableEntry.getKey();
                    stmt = connection.prepareStatement(query + db + "." + tb);
                    ResultSet rs;
                    rs = stmt.executeQuery();
                    List<String> colList = tableEntry.getValue().getColList();
                    List<String> DataTypeList = tableEntry.getValue().getRawDataTypeList();
                    List<ColumnParser> ParserList = tableEntry.getValue().getParserList();

                    while (rs.next()) {
                        Table table = new Table(db, tb);
                        //System.out.print("|");
                        table.setColList(colList);
                        table.setRawDataTypeList(DataTypeList);
                        table.setParserList(ParserList);

                        for (String string : colList) {
                            table.getDataList().add(rs.getObject(string));
                            //System.out.print(string + " : " + rs.getObject(string) + "|");
                        }
                        tableLinkedList.add(table);
                    }
                    rs.close();
                    stmt.close();
                }
            }
            list = tableLinkedList;
        } catch (SQLException e) {
            log.error("fail to poll data, {}", e);
        }

    }

    public void start() throws Exception {
        String whiteDataBases = config.getWhiteDataBase();
        String whiteTables = config.getWhiteTable();

        if (!StringUtils.isEmpty(whiteDataBases)) {
            Arrays.asList(whiteDataBases.trim().split(",")).forEach(whiteDataBase -> {
                Collections.addAll(schema.dataBaseWhiteList, whiteDataBase);
            });
        }

        if (!StringUtils.isEmpty(whiteTables)) {
            Arrays.asList(whiteTables.trim().split(",")).forEach(whiteTable -> {
                Collections.addAll(schema.tableWhiteList, whiteTable);
            });
        }
        schema.load();
        log.info("load schema success");
    }

}
