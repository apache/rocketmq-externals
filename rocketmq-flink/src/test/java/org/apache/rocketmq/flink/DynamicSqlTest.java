package org.apache.rocketmq.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @Author: gaobo07
 * @Date: 2020/10/10 11:29 上午
 */
public class DynamicSqlTest {

    private static final String nameServer = "localhost:9876";

    private static final String topic = "flink_test";


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        EnvironmentSettings settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);

        String ddl = "CREATE TABLE datagen ( \n" +
                "  f_sequence INT, \n" +
                "  f_random INT, \n" +
                "  f_random_str STRING, \n" +
                "  ts STRING \n" +
                ") WITH ( \n" +
                "  'connector' = 'mcq-flink',\n" +
                "  'topic' = '" + topic + "', \n" +
                "  'nameserver.address' = '" + nameServer + "', \n" +
                "  'group' = 'connector', \n" +
                "  'format' = 'mcq-json' \n" +
                ")";
        tEnv.executeSql(ddl);

        String ddl2 = "CREATE TABLE datagenoutput ( \n" +
                "  f_sequence INT, \n" +
                "  f_random INT, \n" +
                "  f_random_str STRING, \n" +
                "  ts STRING \n" +
                ") WITH ( \n" +
                "  'connector' = 'mcq-flink',\n" +
                "  'topic' = 'flink_output', \n" +
                "  'nameserver.address' = '" + nameServer + "', \n" +
                "  'group' = 'connector', \n" +
                "  'tag' = 'test', \n" +
                "  'format' = 'mcq-json', \n" +
                "  'mcq-json.key.position' = '1', \n" +
                "  'mcq-json.ignore-parse-errors' = 'true' \n" +
                ")";
        tEnv.executeSql(ddl2);

//        Table result = tEnv.sqlQuery("SELECT f_sequence, f_random, f_random_str  FROM datagen");
//
//        // print the result to the console
//        tEnv.toRetractStream(result, Row.class).print();

        String insertSql = "INSERT INTO datagenoutput SELECT *  FROM datagen WHERE f_sequence > 10";
        TableResult tableResult = tEnv.executeSql(insertSql);

        tableResult.print();

//        env.execute("hello user define source");

    }

}
