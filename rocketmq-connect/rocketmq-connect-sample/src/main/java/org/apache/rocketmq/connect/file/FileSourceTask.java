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
package org.apache.rocketmq.connect.file;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.exception.ConnectException;
import io.openmessaging.connector.api.source.SourceTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.connect.file.FileConstants.LINE;

public class FileSourceTask extends SourceTask {

    private Logger log = LoggerFactory.getLogger(LoggerName.FILE_CONNECTOR);

    private FileConfig fileConfig;

    private InputStream stream;
    private BufferedReader reader = null;
    private char[] buffer = new char[1024];
    private int offset = 0;
    private int batchSize = FileSourceConnector.DEFAULT_TASK_BATCH_SIZE;

    private Long streamOffset;

    @Override public Collection<SourceDataEntry> poll() {
        if (stream == null) {
            try {
                stream = Files.newInputStream(Paths.get(fileConfig.getFilename()));
                ByteBuffer positionInfo;
                positionInfo = this.context.positionStorageReader().getPosition(ByteBuffer.wrap(FileConstants.getPartition(fileConfig.getFilename()).getBytes(Charset.defaultCharset())));
                if (positionInfo != null) {
                    String positionJson = new String(positionInfo.array(), Charset.defaultCharset());
                    JSONObject jsonObject = JSONObject.parseObject(positionJson);
                    Object lastRecordedOffset = jsonObject.getLong(FileConstants.NEXT_POSITION);
                    if (lastRecordedOffset != null && !(lastRecordedOffset instanceof Long))
                        throw new ConnectException(-1, "Offset position is the incorrect type");
                    if (lastRecordedOffset != null) {
                        log.debug("Found previous offset, trying to skip to file offset {}", lastRecordedOffset);
                        long skipLeft = (Long) lastRecordedOffset;
                        while (skipLeft > 0) {
                            try {
                                long skipped = stream.skip(skipLeft);
                                skipLeft -= skipped;
                            } catch (IOException e) {
                                log.error("Error while trying to seek to previous offset in file {}: ", fileConfig.getFilename(), e);
                                throw new ConnectException(-1, e);
                            }
                        }
                        log.debug("Skipped to offset {}", lastRecordedOffset);
                    }
                    streamOffset = (lastRecordedOffset != null) ? (Long) lastRecordedOffset : 0L;
                } else {
                    streamOffset = 0L;
                }
                reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                log.debug("Opened {} for reading", logFilename());
            } catch (NoSuchFileException e) {
                log.warn("Couldn't find file {} for FileStreamSourceTask, sleeping to wait for it to be created", logFilename());
                synchronized (this) {
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e1) {
                        log.error("Interrupt error .", e1);
                    }
                }
                return null;
            } catch (IOException e) {
                log.error("Error while trying to open file {}: ", fileConfig.getFilename(), e);
                throw new ConnectException(-1, e);
            }
        }

        try {
            final BufferedReader readerCopy;
            synchronized (this) {
                readerCopy = reader;
            }
            if (readerCopy == null) {
                return null;
            }

            Collection<SourceDataEntry> records = null;

            int nread = 0;
            while (readerCopy.ready()) {
                nread = readerCopy.read(buffer, offset, buffer.length - offset);
                log.trace("Read {} bytes from {}", nread, logFilename());

                if (nread > 0) {
                    offset += nread;
                    if (offset == buffer.length) {
                        char[] newbuf = new char[buffer.length * 2];
                        System.arraycopy(buffer, 0, newbuf, 0, buffer.length);
                        buffer = newbuf;
                    }

                    String line;
                    do {
                        line = extractLine();
                        if (line != null) {
                            log.trace("Read a line from {}", logFilename());
                            if (records == null) {
                                records = new ArrayList<>();
                            }
                            Schema schema = new Schema();
                            schema.setDataSource(fileConfig.getFilename());
                            schema.setName(fileConfig.getFilename() + LINE);
                            final Field field = new Field(0, FileConstants.FILE_LINE_CONTENT, FieldType.STRING);
                            List<Field> fields = new ArrayList<Field>() {
                                {
                                    add(field);
                                }
                            };
                            schema.setFields(fields);
                            DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema)
                                .entryType(EntryType.CREATE)
                                .queue(fileConfig.getTopic())
                                .timestamp(System.currentTimeMillis())
                                .putFiled(FileConstants.FILE_LINE_CONTENT, line);
                            final SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(offsetKey(FileConstants.getPartition(fileConfig.getFilename())), offsetValue(streamOffset));
                            records.add(sourceDataEntry);
                            if (records.size() >= batchSize) {
                                return records;
                            }
                        }
                    }
                    while (line != null);
                }
            }

            if (nread <= 0) {
                synchronized (this) {
                    this.wait(1000);
                }
            }

            return records;
        } catch (IOException e) {
        } catch (InterruptedException e) {
            log.error("Interrupt error .", e);
        }
        return null;
    }

    private String extractLine() {
        int until = -1, newStart = -1;
        for (int i = 0; i < offset; i++) {
            if (buffer[i] == '\n') {
                until = i;
                newStart = i + 1;
                break;
            } else if (buffer[i] == '\r') {
                // We need to check for \r\n, so we must skip this if we can't check the next char
                if (i + 1 >= offset)
                    return null;

                until = i;
                newStart = (buffer[i + 1] == '\n') ? i + 2 : i + 1;
                break;
            }
        }

        if (until != -1) {
            String result = new String(buffer, 0, until);
            System.arraycopy(buffer, newStart, buffer, 0, buffer.length - newStart);
            offset = offset - newStart;
            if (streamOffset != null)
                streamOffset += newStart;
            return result;
        } else {
            return null;
        }
    }

    public void commitRecord(SourceDataEntry sourceDataEntry, SinkDataEntry sinkDataEntry) {
        log.info("commit sink queueOffset: {} ", sinkDataEntry.getQueueOffset());
    }

    @Override public void start(KeyValue props) {
        fileConfig = new FileConfig();
        fileConfig.load(props);
        if (fileConfig.getFilename() == null || fileConfig.getFilename().isEmpty()) {
            stream = System.in;
            streamOffset = null;
            reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    @Override public void stop() {
        log.trace("Stopping");
        synchronized (this) {
            try {
                if (stream != null && stream != System.in) {
                    stream.close();
                    log.trace("Closed input stream");
                }
            } catch (IOException e) {
                log.error("Failed to close FileStreamSourceTask stream: ", e);
            }
            this.notify();
        }
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    private String logFilename() {
        return fileConfig.getFilename() == null ? "stdin" : fileConfig.getFilename();
    }

    private ByteBuffer offsetKey(String filename) {
        return ByteBuffer.wrap(filename.getBytes(Charset.defaultCharset()));
    }

    private ByteBuffer offsetValue(Long pos) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FileConstants.NEXT_POSITION, pos);
        return ByteBuffer.wrap(jsonObject.toJSONString().getBytes(Charset.defaultCharset()));
    }

}
