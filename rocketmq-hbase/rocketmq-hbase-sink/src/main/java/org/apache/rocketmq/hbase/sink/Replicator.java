/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.hbase.sink;

import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.replication.BaseReplicationEndpoint;
import org.apache.hadoop.hbase.wal.WAL;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class functions as an HBase replication endpoint, that runs at each region server,
 * and replicates data from specified HBase tables to a RocketMQ topic.
 */
public class Replicator extends BaseReplicationEndpoint {

    private static final String ROCKETMQ_NAMESRV_ADDR_PARAM = "rocketmq.namesrv.addr";

    private static final String ROCKETMQ_TOPIC_PARAM = "rocketmq.topic";

    private static final String ROCKETMQ_HBASE_TABLES_PARAM = "rocketmq.hbase.tables";

    private static final String ROCKETMQ_TRANSACTION_ROWS_PARAM = "rocketmq.transaction.max.rows";

    private static final int ROCKETMQ_TRANSACTION_ROWS_DEFAULT = 100;

    private static final Logger logger = LoggerFactory.getLogger(Replicator.class);

    private RocketMQProducer producer;

    private Set<String> tables = Sets.newHashSet();

    private int maxTransactionRows;

    /**
     * Constructor.
     */
    public Replicator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Context context) throws IOException {
        super.init(context);
        logger.info("HBaseEndpoint initialized");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStart() {
        final Configuration config = ctx.getConfiguration();
        final String namesrvAddr = config.get(ROCKETMQ_NAMESRV_ADDR_PARAM);
        if (namesrvAddr == null) {
            logger.error("Configuration property not set: " + ROCKETMQ_NAMESRV_ADDR_PARAM);
            return;
        }

        final String topic = config.get(ROCKETMQ_TOPIC_PARAM);
        if (topic == null) {
            logger.error("Configuration property not set: " + ROCKETMQ_TOPIC_PARAM);
            return;
        }

        final String tablesParam = config.get(ROCKETMQ_HBASE_TABLES_PARAM);
        if (tablesParam == null) {
            logger.error("Configuration property not set: " + ROCKETMQ_HBASE_TABLES_PARAM);
            return;
        }
        tables = new HashSet<>(Arrays.asList(tablesParam.split(",")));

        maxTransactionRows = config.getInt(ROCKETMQ_TRANSACTION_ROWS_PARAM, ROCKETMQ_TRANSACTION_ROWS_DEFAULT);

        try {
            producer = new RocketMQProducer(namesrvAddr, topic);
            producer.start();

            logger.info("HBase replication to RocketMQ started");
            notifyStarted();
        } catch (MQClientException e) {
            logger.error("Failed to start RocketMQ producer.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStop() {
        producer.stop();

        logger.info("HBase replication to RocketMQ stopped.");
        notifyStopped();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getPeerUUID() {
        return UUID.randomUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replicate(ReplicateContext context) {
        final List<WAL.Entry> entries = context.getEntries();

        final Map<String, List<WAL.Entry>> entriesByTable = entries.stream()
                .filter(entry -> tables.contains(entry.getKey().getTablename().getNameAsString()))
                .collect(groupingBy(entry -> entry.getKey().getTablename().getNameAsString()));

        // replicate data to rocketmq
        Transaction transaction = new Transaction(maxTransactionRows);
        try {
            for (Map.Entry<String, List<WAL.Entry>> entry : entriesByTable.entrySet()) {
                final String tableName = entry.getKey();
                final List<WAL.Entry> tableEntries = entry.getValue();

                for (WAL.Entry tableEntry : tableEntries) {
                    List<Cell> cells = tableEntry.getEdit().getCells();

                    // group entries by the row key
                    Map<byte[], List<Cell>> columnsByRow = cells.stream().collect(groupingBy(CellUtil::cloneRow));

                    for (Map.Entry<byte[], List<Cell>> rowCols : columnsByRow.entrySet()) {
                        final byte[] row = rowCols.getKey();
                        final List<Cell> columns = rowCols.getValue();

                        if (!transaction.addRow(tableName, row, columns)) {
                            producer.push(transaction.toJson());
                            transaction = new Transaction(maxTransactionRows);
                        }
                    }
                }
            }

            // replicate remaining transaction
            producer.push(transaction.toJson());
        } catch (Exception e) {
            logger.error("Error while sending message to RocketMQ.", e);
            return false;
        }

        return true;
    }
}
