package org.apache.rocketmq.flink.dynamic;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;
import org.apache.rocketmq.flink.common.serialization.json.McqSerializationFormatFactory;
import org.apache.rocketmq.flink.common.serialization.json.McqSerializationSchema;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author: gaobo07
 * @Date: 2020/9/27 10:11 上午
 */
public class McqDynamicTableFactory implements DynamicTableSourceFactory, DynamicTableSinkFactory {

    private final static String IDENTIFIER = "mcq-flink";

    public static final ConfigOption<String> TOPIC = ConfigOptions
            .key("topic")
            .stringType()
            .noDefaultValue()
            .withDescription("Required topic name from which the table is read");

    public static final ConfigOption<String> NAMESERVER_ADDRESS = ConfigOptions
            .key("nameserver.address")
            .stringType()
            .noDefaultValue()
            .withDescription("Required mcq server connection string");

    public static final ConfigOption<String> GROUP = ConfigOptions
            .key("group")
            .stringType()
            .noDefaultValue()
            .withDescription("Required group in mcq producer and consumer");


    public static final ConfigOption<String> TAG = ConfigOptions
            .key("tag")
            .stringType()
            .noDefaultValue()
            .withDescription("Required tag in mcq consumer, no need for mcq producer");

    // --------------------------------------------------------------------------------------------
    // Scan specific options
    // --------------------------------------------------------------------------------------------

    public static final ConfigOption<String> CONSUMER_OFFSET_RESET_TO = ConfigOptions
            .key("consumer.offset.reset.to")
            .stringType()
            .defaultValue("latest")
            .withDescription("Optional startup mode for mcq consumer, valid enumerations are "
                    + "\"latest\", \"earliest\"");

    @Override
    public String factoryIdentifier() {
        // used for matching to `connector = '...'`
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(NAMESERVER_ADDRESS);
        options.add(TOPIC);
        options.add(GROUP);
        options.add(FactoryUtil.FORMAT);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(TAG);
        options.add(CONSUMER_OFFSET_RESET_TO);
        return options;
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        // either implement your custom validation logic here ...
        // or use the provided helper utility
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

        DecodingFormat<DeserializationSchema<RowData>> decodingFormat = helper.discoverDecodingFormat(
                DeserializationFormatFactory.class,
                FactoryUtil.FORMAT);

        // validate all options
        helper.validate();

        // derive the produced data type (excluding computed columns) from the catalog table
        final DataType consumerDataType = context.getCatalogTable().getSchema().toPhysicalRowDataType();

        ReadableConfig options = helper.getOptions();

        // create and return dynamic table source
        return new McqDynamicTableSource(options.get(NAMESERVER_ADDRESS),
                options.get(TOPIC),
                options.get(GROUP),
                options.get(TAG),
                options.get(CONSUMER_OFFSET_RESET_TO),
                consumerDataType,
                decodingFormat);
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

        EncodingFormat<McqSerializationSchema<RowData>> encodingFormat = helper.discoverEncodingFormat(
                McqSerializationFormatFactory.class,
                FactoryUtil.FORMAT);
        // validate all options
        helper.validate();

        DataType produceDataType = context.getCatalogTable().getSchema().toPhysicalRowDataType();
        ReadableConfig options = helper.getOptions();

        return new McqDynamicTableSink(
                produceDataType,
                options.get(TOPIC),
                options.get(NAMESERVER_ADDRESS),
                options.get(GROUP),
                options.get(TAG),
                encodingFormat);
    }

}
