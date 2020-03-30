package org.apache.rocketmq.connect.jdbc.source;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.rocketmq.connect.jdbc.config.Config;
import org.apache.rocketmq.connect.jdbc.common.DBUtils;
import org.apache.rocketmq.connect.jdbc.schema.Database;
import org.apache.rocketmq.connect.jdbc.schema.Schema;
import org.apache.rocketmq.connect.jdbc.schema.Table;
import org.apache.rocketmq.connect.jdbc.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.druid.pool.DruidDataSourceFactory;


public class TimestampIncrementingQuerier extends Querier {
    protected PreparedStatement stmt;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    protected String jdbcUrl;

    private Config config;
    private DataSource dataSource;
    private static final Logger log = LoggerFactory.getLogger(TimestampIncrementingQuerier.class);
    private List<Table> list = new LinkedList<>();
    private HashMap<String, Long> incrementingMap;
    private HashMap<String, Timestamp> timestampMap;
    private static final Calendar UTC_CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("UTC+8"));
    private String timestampColumn = "";
    static final String INCREMENTING_FIELD = "incrementing";
    static final String TIMESTAMP_FIELD = "timestamp";
    private Map<String, Long> offset;
    private Long timestampOffset;
    private String incrementingColumn = "";
    private Map<String, String> partition;
    private Schema schema;

    public String getTimestampColumn() {
        return timestampColumn;
    }

    public void setTimestampColumn(String timestampColumn) {
        this.timestampColumn = timestampColumn;
    }

    public String getIncrementingColumn() {
        return incrementingColumn;
    }

    public void setIncrementingColumn(String incrementingColumn) {
        this.incrementingColumn = incrementingColumn;
    }

    private Long incrementingOffset = null;
    private String name;

    public void extractRecord(String name) throws SQLException {
        if (incrementingColumn != null) {
            log.info("{}", name);
            incrementingMap.put(name, incrementingOffset);
        }
        if (timestampColumn != null) {
            timestampMap.put(name, new Timestamp(timestampOffset));
        }
    }

    @SuppressWarnings("deprecation")
    public void storeRecord(String name) throws SQLException {
        offset = new HashMap<>();
        if (incrementingColumn != null) {
            Long id = 0L;

            if (incrementingMap.containsKey(name)) {
                id = incrementingMap.get(name);
                System.out.println("read incrementingMap" + id);
            }
            assert (incrementingOffset == null || id > incrementingOffset) || timestampColumn != null;
            incrementingOffset = id;
            offset.put(INCREMENTING_FIELD, id);
        }
        if (timestampColumn != null) {
            Timestamp timestamp = new Timestamp(0);
            if (timestampMap.containsKey(name))
                timestamp = timestampMap.get(name);

            System.out.println("read timestampColumn" + timestamp.toString());
            timestampOffset = timestamp.getTimezoneOffset() + timestamp.getTime();
            System.out.println("read" + new Timestamp(timestampOffset));
            offset.put(TIMESTAMP_FIELD, timestampOffset);
        }
        log.info("{}store", new Timestamp(timestampOffset));
        partition = Collections.singletonMap("table", name);
    }

    protected void createPreparedStatement(Connection conn) throws SQLException {
        // Default when unspecified uses an autoincrementing column
        if (incrementingColumn != null && incrementingColumn.isEmpty()) {
            incrementingColumn = DBUtils.getAutoincrementColumn(conn, name);
        }

        String quoteString = conn.getMetaData().getIdentifierQuoteString();
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM ");
        builder.append(name);

        quoteString = quoteString == null ? "" : quoteString;

        if (incrementingColumn != null && timestampColumn != null) {
            // This version combines two possible conditions. The first checks timestamp ==
            // last
            // timestamp and incrementing > last incrementing. The timestamp alone would
            // include
            // duplicates, but adding the incrementing condition ensures no duplicates, e.g.
            // you would
            // get only the row with id = 23:
            // timestamp 1234, id 22 <- last
            // timestamp 1234, id 23
            // The second check only uses the timestamp >= last timestamp. This covers
            // everything new,
            // even if it is an update of the existing row. If we previously had:
            // timestamp 1234, id 22 <- last
            // and then these rows were written:
            // timestamp 1235, id 22
            // timestamp 1236, id 23
            // We should capture both id = 22 (an update) and id = 23 (a new row)
            String timeString = quoteString + timestampColumn + quoteString;
            String incrString = quoteString + incrementingColumn + quoteString;
            builder.append(" WHERE ");
            builder.append(timeString);
            builder.append(" < CURRENT_TIMESTAMP AND ((");
            builder.append(timeString);
            builder.append(" = ? AND ");
            builder.append(incrString);
            builder.append(" > ?");
            builder.append(") OR ");
            builder.append(timeString);
            builder.append(" > ?)");
            builder.append(" ORDER BY ");
            builder.append(timeString);
            builder.append(",");
            builder.append(incrString);
            builder.append(" ASC");

        } else if (incrementingColumn != null) {
            String incrString = quoteString + incrementingColumn + quoteString;
            builder.append(" WHERE ");
            builder.append(incrString);
            builder.append(" > ?");
            builder.append(" ORDER BY ");
            builder.append(incrString);
            builder.append(" ASC");
        } else if (timestampColumn != null) {
            String timeString = quoteString + timestampColumn + quoteString;
            builder.append(" WHERE ");
            builder.append(timeString);
            builder.append(" > ? AND ");
            builder.append(timeString);
            builder.append(" < CURRENT_TIMESTAMP ORDER BY ");
            builder.append(timeString);
            builder.append(" ASC");
        }
        String queryString = builder.toString();
        stmt = conn.prepareStatement(queryString);
        log.info(queryString);
    }

    public static void main(String[] args) throws Exception {
        TimestampIncrementingQuerier querier = new TimestampIncrementingQuerier();
        try {
            querier.start();
            querier.poll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected ResultSet executeQuery() throws SQLException {
        if (incrementingColumn != null && timestampColumn != null) {
            Timestamp ts = new Timestamp(timestampOffset == null ? 0 : timestampOffset);
            stmt.setTimestamp(1, ts);
            stmt.setLong(2, (incrementingOffset == null ? -1 : incrementingOffset));
            stmt.setTimestamp(3, ts);
        } else if (incrementingColumn != null) {
            stmt.setLong(1, (incrementingOffset == null ? -1 : incrementingOffset));
        } else if (timestampColumn != null) {
            Timestamp ts = new Timestamp(timestampOffset == null ? 0 : timestampOffset);
            stmt.setTimestamp(1, ts);
        }
        log.info("{}·", stmt);
        log.info("{},{}", incrementingOffset, timestampOffset);
        return stmt.executeQuery();
    }                       

    public List<Table> getList() {
        return list;
    }

    public void setList(List<Table> list) {
        this.list = list;
    }

    public void poll() {
        try {
            list.clear();
            Connection conn = dataSource.getConnection();
            for (Map.Entry<String, Database> entry : schema.getDbMap().entrySet()) {
                String db = entry.getKey();
                log.info("{} database is loading", db);
                Iterator<Map.Entry<String, Table>> iterator = entry.getValue().getTableMap().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Table> tableEntry = iterator.next();
                    String tb = tableEntry.getKey();
                    log.info("{} table is loading", tb);
                    name = db + "." + tb;
                    storeRecord(name);
                    createPreparedStatement(conn);
                    ResultSet rs;
                    rs = executeQuery();
                    List<String> colList = tableEntry.getValue().getColList();
                    List<String> DataTypeList = tableEntry.getValue().getRawDataTypeList();
                    List<ColumnParser> ParserList = tableEntry.getValue().getParserList();

                    while (rs.next()) {
                        Table table = new Table(db, tb);
                        System.out.print("|");
                        table.setColList(colList);
                        table.setRawDataTypeList(DataTypeList);
                        table.setParserList(ParserList);
                        for (String string : colList) {
                            table.getDataList().add(rs.getObject(string));
                            System.out.print(string + " : " + rs.getObject(string) + "|");
                        }
                        incrementingOffset = incrementingOffset > rs.getInt(incrementingColumn) ? incrementingOffset
                                : rs.getInt(incrementingColumn);
                        timestampOffset = timestampOffset > rs.getTimestamp(timestampColumn).getTime() ? timestampOffset
                                : rs.getTimestamp(timestampColumn).getTime();
                        System.out.println(timestampOffset);
                        list.add(table);
                        System.out.println();
                    }
                    extractRecord(name);
                    incrementingOffset = 0L;
                    timestampOffset = 0L;
                }
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        try {
            initDataSource();
            if (incrementingColumn != null && timestampColumn != null) {
                incrementingMap = new HashMap<>();
                timestampMap = new HashMap<>();
            } else if (incrementingColumn != null) {
                incrementingMap = new HashMap<>();
            } else if (timestampColumn != null) {
                timestampMap = new HashMap<>();
            }
        } catch (Throwable exception) {
            log.info("error,{}", exception);
        }
        schema = new Schema(dataSource.getConnection());
        schema.load();
    }

    public void initDataSource() throws Exception {
        Map<String, String> map = new HashMap<>();
        config = super.getConfig();
        timestampColumn = config.getTimestampColmnName();
        incrementingColumn = config.getIncrementingColumnName();
        map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        map.put("url",
                "jdbc:mysql://" + config.getDbUrl() + ":" + config.getDbPort() +"?useSSL=true&verifyServerCertificate=false&serverTimezone=GMT%2B8");
        map.put("username", config.getDbUsername());
        map.put("password", config.getDbPassword());
        map.put("initialSize", "2");
        map.put("maxActive", "2");
        map.put("maxWait", "60000");
        map.put("timeBetweenEvictionRunsMillis", "60000");
        map.put("minEvictableIdleTimeMillis", "300000");
        map.put("validationQuery", "SELECT 1 FROM DUAL");
        map.put("testWhileIdle", "true");
        log.info("{}config read successfully", map);
        try {
            dataSource = DruidDataSourceFactory.createDataSource(map);
        } catch (Exception exception) {
            log.info("exeception,{}", exception);
        } catch (Error error) {
            log.info("error,{}", error);
        }
        log.info("datasorce success");
    }
}
