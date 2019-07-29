package org.apache.rocketmq.connect.jdbc.source;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sql.DataSource;

import org.apache.rocketmq.connect.jdbc.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSourceFactory;


import org.apache.rocketmq.connect.jdbc.schema.*;
import org.apache.rocketmq.connect.jdbc.schema.column.ColumnParser;

public class Querier {
	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	private final Logger log = LoggerFactory.getLogger(getClass()); // use concrete subclass
	protected String topicPrefix;
	protected String jdbcUrl;
	private final Queue<Connection> connections = new ConcurrentLinkedQueue<>();
	private Config config = new Config();
	private DataSource dataSource;
	private List<Table> list=new LinkedList<>();

	
	
	public List<Table> getList() {
		return list;
	}

	public void setList(List<Table> list) {
		this.list = list;
	}

	public Connection getConnection() throws SQLException {

		// These config names are the same for both source and sink configs ...
		String username = config.jdbcUsername;
		String dbPassword = config.jdbcPassword;
		jdbcUrl = config.jdbcUrl;
		Properties properties = new Properties();
		if (username != null) {
			properties.setProperty("user", username);
		}
		if (dbPassword != null) {
			properties.setProperty("password", dbPassword);
		}
		Connection connection = DriverManager.getConnection(jdbcUrl, properties);

		connections.add(connection);
		return connection;
	}

	public void close() {
		Connection conn;
		while ((conn = connections.poll()) != null) {
			try {
				conn.close();
			} catch (Throwable e) {
				log.warn("Error while closing connection to {}", "jdbc", e);
			}
		}
	}

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

	public static void main(String[] args) throws Exception {
		Querier querier = new Querier();
		try {
			querier.start();
			querier.poll();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Schema schema;

	private Map<Long, Table> tableMap = new HashMap<>();

	public void poll() {
		try {

			PreparedStatement stmt;
			String query = "select * from ";
			Connection conn = dataSource.getConnection();

			for (Map.Entry<String, Database> entry : schema.dbMap.entrySet()) {
				String db = entry.getKey();
				if(!db.contains("jdbc_db"))
					continue;
				Iterator<Map.Entry<String, Table>> iterator = entry.getValue().tableMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Table> tableEntry = iterator.next();
					String tb=tableEntry.getKey();
					stmt = conn.prepareStatement(query+db + "." +tb);
					ResultSet rs;
					rs = stmt.executeQuery();
				    List<String> colList = tableEntry.getValue().getColList();
				    List<String> DataTypeList = tableEntry.getValue().getRawDataTypeList();
				    List<ColumnParser> ParserList = tableEntry.getValue().getParserList();

				    while(rs.next()) {
					    Table table=new Table(db, tb);
					    System.out.print("|");
			    		table.setColList(colList);
			    		table.setRawDataTypeList(DataTypeList);
			    		table.setParserList(ParserList);
			    		
				    	for (String string : colList) {
				    		table.getDataList().add(rs.getObject(string));
				    		System.out.print(string+" : "+rs.getObject(string)+"|");
					}
				    	list.add(table);
				    	System.out.println();
				    }
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void start() throws Exception {
		initDataSource();
		schema = new Schema(dataSource);
		schema.load();
	}

	private void initDataSource() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
		map.put("url",
				"jdbc:mysql://" + config.jdbcUrl + "?useSSL=true&verifyServerCertificate=false&serverTimezone=GMT%2B8");
		map.put("username", config.jdbcUsername);
		map.put("password", config.jdbcPassword);
		map.put("initialSize", "2");
		map.put("maxActive", "2");
		map.put("maxWait", "60000");
		map.put("timeBetweenEvictionRunsMillis", "60000");
		map.put("minEvictableIdleTimeMillis", "300000");
		map.put("validationQuery", "SELECT 1 FROM DUAL");
		map.put("testWhileIdle", "true");
		dataSource = DruidDataSourceFactory.createDataSource(map);
	}

}
