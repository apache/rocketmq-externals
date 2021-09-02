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

package org.apache.rocketmq.cloudevent;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.message.StructuredMessageReader;
import io.cloudevents.core.mock.CSVFormat;
import io.cloudevents.core.test.Data;
import io.cloudevents.core.v03.CloudEventV03;
import io.cloudevents.core.v1.CloudEventV1;
import io.cloudevents.types.Time;
import org.apache.rocketmq.cloudevent.impl.RocketMQHeaders;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class RocketMQMessageWriterTest {

    private static final String PREFIX_TEMPLATE = RocketMQHeaders.CE_PREFIX + "%s";
    private static final String DATACONTENTTYPE_NULL = null;
    private static final byte[] DATAPAYLOAD_NULL = null;


    @ParameterizedTest
    @MethodSource("io.cloudevents.core.test.Data#allEventsWithoutExtensions")
    void testRequestWithStructured(CloudEvent event) {
        String expectedContentType = CSVFormat.INSTANCE.serializedContentType();
        byte[] expectedBuffer = CSVFormat.INSTANCE.serialize(event);

        String topic = "test";
        String keys = "keys";
        String tags = "tags";

        Message message = StructuredMessageReader
                .from(event, CSVFormat.INSTANCE)
                .read(RocketMQMessageFactory.createWriter(topic, keys, tags));

        assertThat(message.getTopic())
                .isEqualTo(topic);
        assertThat(message.getKeys())
                .isEqualTo(keys);
        assertThat(message.getTags())
                .isEqualTo(tags);
        assertThat(message.getBody())
                .isEqualTo(expectedBuffer);
    }

    @ParameterizedTest
    @MethodSource("binaryTestArguments")
    void testRequestWithBinary(CloudEvent event, Map<String, String> expectedHeaders, byte[] expectedBody) {

        String topic = "test";
        String keys = "keys";
        String tags = "tags";

        Message message = RocketMQMessageFactory
                .createWriter(topic, keys, tags)
                .writeBinary(event);

        assertThat(message.getTopic())
                .isEqualTo(topic);
        assertThat(message.getKeys())
                .isEqualTo(keys);
        assertThat(message.getTags())
                .isEqualTo(tags);
        assertThat(message.getBody())
                .isEqualTo(expectedBody);
        assertThat(message.getProperties()
                .keySet().containsAll(expectedHeaders.keySet()));
        assertThat(message.getProperties()
                .values().containsAll(expectedHeaders.values()));
    }

    private static Stream<Arguments> binaryTestArguments() {

        return Stream.of(
                // V03
                Arguments.of(
                        Data.V03_MIN,
                        properties(
                                property(CloudEventV03.SPECVERSION, SpecVersion.V03.toString()),
                                property(CloudEventV03.ID, Data.ID),
                                property(CloudEventV03.TYPE, Data.TYPE),
                                property(CloudEventV03.SOURCE, Data.SOURCE.toString()),
                                property("ignored", "ignore")
                        ),
                        DATAPAYLOAD_NULL
                ),
                Arguments.of(
                        Data.V03_WITH_JSON_DATA,
                        properties(
                                property(CloudEventV03.SPECVERSION, SpecVersion.V03.toString()),
                                property(CloudEventV03.ID, Data.ID),
                                property(CloudEventV03.TYPE, Data.TYPE),
                                property(CloudEventV03.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV03.SCHEMAURL, Data.DATASCHEMA.toString()),
                                property(CloudEventV03.SUBJECT, Data.SUBJECT),
                                property(CloudEventV03.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignore")
                        ),
                        Data.DATA_JSON_SERIALIZED

                ),
                Arguments.of(
                        Data.V03_WITH_JSON_DATA_WITH_EXT_STRING,
                        properties(
                                property(CloudEventV03.SPECVERSION, SpecVersion.V03.toString()),
                                property(CloudEventV03.ID, Data.ID),
                                property(CloudEventV03.TYPE, Data.TYPE),
                                property(CloudEventV03.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV03.SCHEMAURL, Data.DATASCHEMA.toString()),
                                property(CloudEventV03.SUBJECT, Data.SUBJECT),
                                property(CloudEventV03.TIME, Time.writeTime(Data.TIME)),
                                property("astring", "aaa"),
                                property("aboolean", "true"),
                                property("anumber", "10"),
                                property("ignored", "ignored")
                        ),
                        Data.DATA_JSON_SERIALIZED

                ),
                Arguments.of(
                        Data.V03_WITH_XML_DATA,
                        properties(
                                property(CloudEventV03.SPECVERSION, SpecVersion.V03.toString()),
                                property(CloudEventV03.ID, Data.ID),
                                property(CloudEventV03.TYPE, Data.TYPE),
                                property(CloudEventV03.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV03.SUBJECT, Data.SUBJECT),
                                property(CloudEventV03.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_XML_SERIALIZED

                ),
                Arguments.of(
                        Data.V03_WITH_TEXT_DATA,
                        properties(
                                property(CloudEventV03.SPECVERSION, SpecVersion.V03.toString()),
                                property(CloudEventV03.ID, Data.ID),
                                property(CloudEventV03.TYPE, Data.TYPE),
                                property(CloudEventV03.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV03.SUBJECT, Data.SUBJECT),
                                property(CloudEventV03.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_TEXT_SERIALIZED

                ),
                // V1
                Arguments.of(
                        Data.V1_MIN,
                        properties(
                                property(CloudEventV1.SPECVERSION, SpecVersion.V1.toString()),
                                property(CloudEventV1.ID, Data.ID),
                                property(CloudEventV1.TYPE, Data.TYPE),
                                property(CloudEventV1.SOURCE, Data.SOURCE.toString()),
                                property("ignored", "ignored")
                        ),

                        DATAPAYLOAD_NULL

                ),
                Arguments.of(
                        Data.V1_WITH_JSON_DATA,
                        properties(
                                property(CloudEventV1.SPECVERSION, SpecVersion.V1.toString()),
                                property(CloudEventV1.ID, Data.ID),
                                property(CloudEventV1.TYPE, Data.TYPE),
                                property(CloudEventV1.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV1.DATASCHEMA, Data.DATASCHEMA.toString()),
                                property(CloudEventV1.SUBJECT, Data.SUBJECT),
                                property(CloudEventV1.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_JSON_SERIALIZED

                ),
                Arguments.of(
                        Data.V1_WITH_JSON_DATA_WITH_EXT_STRING,
                        properties(
                                property(CloudEventV1.SPECVERSION, SpecVersion.V1.toString()),
                                property(CloudEventV1.ID, Data.ID),
                                property(CloudEventV1.TYPE, Data.TYPE),
                                property(CloudEventV1.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV1.DATASCHEMA, Data.DATASCHEMA.toString()),
                                property(CloudEventV1.SUBJECT, Data.SUBJECT),
                                property(CloudEventV1.TIME, Time.writeTime(Data.TIME)),
                                property("astring", "aaa"),
                                property("aboolean", "true"),
                                property("anumber", "10"),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_JSON_SERIALIZED

                ),
                Arguments.of(
                        Data.V1_WITH_XML_DATA,
                        properties(
                                property(CloudEventV1.SPECVERSION, SpecVersion.V1.toString()),
                                property(CloudEventV1.ID, Data.ID),
                                property(CloudEventV1.TYPE, Data.TYPE),
                                property(CloudEventV1.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV1.SUBJECT, Data.SUBJECT),
                                property(CloudEventV1.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_XML_SERIALIZED

                ),
                Arguments.of(
                        Data.V1_WITH_TEXT_DATA,
                        properties(
                                property(CloudEventV1.SPECVERSION, SpecVersion.V1.toString()),
                                property(CloudEventV1.ID, Data.ID),
                                property(CloudEventV1.TYPE, Data.TYPE),
                                property(CloudEventV1.SOURCE, Data.SOURCE.toString()),
                                property(CloudEventV1.SUBJECT, Data.SUBJECT),
                                property(CloudEventV1.TIME, Time.writeTime(Data.TIME)),
                                property("ignored", "ignored")
                        ),

                        Data.DATA_TEXT_SERIALIZED

                )
        );
    }

    private static final AbstractMap.SimpleEntry<String, String> property(final String name, final String value) {
        return name.equalsIgnoreCase("ignored") ?
                new AbstractMap.SimpleEntry<>(name, value) :
                new AbstractMap.SimpleEntry<>(String.format(PREFIX_TEMPLATE, name), value);
    }

    @SafeVarargs
    private static final Map<String, String> properties(final AbstractMap.SimpleEntry<String, String>... entries) {
        return Stream.of(entries)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    }
}
