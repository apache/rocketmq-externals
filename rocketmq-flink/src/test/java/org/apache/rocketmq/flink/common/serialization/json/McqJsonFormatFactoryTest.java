package org.apache.rocketmq.flink.common.serialization.json;


import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CatalogTableImpl;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.TestDynamicTableFactory;
import org.apache.flink.table.runtime.connector.sink.SinkRuntimeProviderContext;
import org.apache.flink.table.runtime.connector.source.ScanRuntimeProviderContext;
import org.apache.flink.table.runtime.typeutils.RowDataTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.TestLogger;
import org.apache.rocketmq.flink.dynamic.McqDynamicTableSink;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * @Author: gaobo07
 * @Date: 2020/10/12 11:47 上午
 */
public class McqJsonFormatFactoryTest extends TestLogger {

    private Integer sinkKeyPosition = 0;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final TableSchema SCHEMA = TableSchema.builder()
            .field("id", DataTypes.INT())
            .field("name", DataTypes.STRING())
            .field("description", DataTypes.STRING())
            .field("weight", DataTypes.DOUBLE())
            .build();

    private static final RowType ROW_TYPE = (RowType) SCHEMA.toRowDataType().getLogicalType();

    @Test
    public void testSeDeSchema() {
        final McqJsonDeserializer expectedDeser = new McqJsonDeserializer(
                ROW_TYPE,
                new RowDataTypeInfo(ROW_TYPE),
                true,
                TimestampFormat.ISO_8601);

        final McqJsonSerializer expectedSer = new McqJsonSerializer(
                ROW_TYPE,
                TimestampFormat.ISO_8601,
                sinkKeyPosition);

        Map<String, String> options = getAllOptions();

        final DynamicTableSource actualSource = createTableSource(options);
        assert actualSource instanceof TestDynamicTableFactory.DynamicTableSourceMock;
        TestDynamicTableFactory.DynamicTableSourceMock scanSourceMock =
                (TestDynamicTableFactory.DynamicTableSourceMock) actualSource;

        DeserializationSchema<RowData> actualDeser = scanSourceMock.valueFormat
                .createRuntimeDecoder(
                        ScanRuntimeProviderContext.INSTANCE,
                        SCHEMA.toRowDataType());

        assertEquals(expectedDeser, actualDeser);

        options = getSinkOptions();
        final DynamicTableSink actualSink = createTableSink(options);

        McqSerializationSchema<RowData> actualSer = ((McqDynamicTableSink)actualSink).encodingFormat.createRuntimeEncoder(
                new SinkRuntimeProviderContext(false),
                SCHEMA.toRowDataType());

        assertEquals(expectedSer, actualSer);
    }

    @Test
    public void testInvalidIgnoreParseError() {
        thrown.expect(CoreMatchers.containsCause(new IllegalArgumentException(
                "Unrecognized option for boolean: abc. Expected either true or false(case insensitive)")));

        final Map<String, String> options =
                getModifiedOptions(opts -> opts.put("mcq-json.ignore-parse-errors", "abc"));

        createTableSource(options);
    }

    // ------------------------------------------------------------------------
    //  Utilities
    // ------------------------------------------------------------------------

    /**
     * Returns the full options modified by the given consumer {@code optionModifier}.
     *
     * @param optionModifier Consumer to modify the options
     */
    private Map<String, String> getModifiedOptions(Consumer<Map<String, String>> optionModifier) {
        Map<String, String> options = getAllOptions();
        optionModifier.accept(options);
        return options;
    }

    private Map<String, String> getAllOptions() {
        final Map<String, String> options = new HashMap<>();
        options.put("connector", TestDynamicTableFactory.IDENTIFIER);
        options.put("target", "MyTarget");
        options.put("buffer-size", "1000");

        options.put("format", "mcq-json");
        options.put("mcq-json.ignore-parse-errors", "true");
        options.put("mcq-json.timestamp-format.standard", "ISO-8601");
        return options;
    }

    private Map<String, String> getSinkOptions() {
        final Map<String, String> options = new HashMap<>();
        options.put("connector", "mcq-flink");
        options.put("topic", "myTopic");
        options.put("nameserver.address", "dummy");
        options.put("group", "dummy");
        options.put("tag", "dummy");

        options.put("format", "mcq-json");
        options.put("mcq-json.key.position", sinkKeyPosition.toString());
        options.put("mcq-json.ignore-parse-errors", "true");
        options.put("mcq-json.timestamp-format.standard", "ISO-8601");
        return options;
    }

    private static DynamicTableSource createTableSource(Map<String, String> options) {
        return FactoryUtil.createTableSource(
                null,
                ObjectIdentifier.of("default", "default", "t1"),
                new CatalogTableImpl(SCHEMA, options, "mock source"),
                new Configuration(),
                McqJsonFormatFactoryTest.class.getClassLoader());
    }

    private static DynamicTableSink createTableSink(Map<String, String> options) {

        ObjectIdentifier objectIdentifier = ObjectIdentifier.of("default", "default", "t1");
        CatalogTable catalogTable = new CatalogTableImpl(SCHEMA, options, "full sink");
        ReadableConfig configuration = new Configuration();

        return FactoryUtil.createTableSink(null,
                objectIdentifier,
                catalogTable,
                configuration,
                Thread.currentThread().getContextClassLoader());
    }
}