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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.file;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.exception.ConnectException;
import io.openmessaging.connector.api.sink.SinkTask;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSinkTask extends SinkTask {

    private Logger log = LoggerFactory.getLogger(LoggerName.FILE_CONNECTOR);

    private FileConfig fileConfig;

    private PrintStream outputStream;

    @Override public void put(Collection<SinkDataEntry> sinkDataEntries) {
        for (SinkDataEntry record : sinkDataEntries) {
            Object[] payloads = record.getPayload();
            log.trace("Writing line to {}: {}", logFilename(), payloads);
            Schema schema = record.getSchema();
            List<Field> fields = schema.getFields();
            for (Field field : fields) {
                FieldType type = field.getType();
                if (type.equals(FieldType.STRING)) {
                    log.info("Writing line to {}: {}", logFilename(), payloads[field.getIndex()]);
                    outputStream.println(String.valueOf(payloads[field.getIndex()]));
                }
            }
        }

    }

    @Override public void commit(Map<QueueMetaData, Long> map) {
        log.trace("Flushing output stream for {}", logFilename());
        outputStream.flush();
    }

    @Override public void start(KeyValue props) {
        fileConfig = new FileConfig();
        fileConfig.load(props);
        if (fileConfig.getFilename() == null || fileConfig.getFilename().isEmpty()) {
            outputStream = System.out;
        } else {
            try {
                outputStream = new PrintStream(
                    Files.newOutputStream(Paths.get(fileConfig.getFilename()), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    false,
                    StandardCharsets.UTF_8.name());
            } catch (IOException e) {
                throw new ConnectException(-1, "Couldn't find or create file '" + fileConfig.getFilename() + "' for FileStreamSinkTask", e);
            }
        }
    }

    @Override public void stop() {
        if (outputStream != null && outputStream != System.out) {
            outputStream.close();
        }
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    private String logFilename() {
        return fileConfig.getFilename() == null ? "stdout" : fileConfig.getFilename();
    }

}
