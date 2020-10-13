package org.apache.rocketmq.flink.dynamic;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.util.Preconditions;
import org.apache.rocketmq.flink.RocketMQConfig;
import org.apache.rocketmq.flink.RocketMQSink;
import org.apache.rocketmq.flink.common.selector.DefaultTopicSelector;
import org.apache.rocketmq.flink.common.serialization.json.McqSerializationSchema;

import java.util.Objects;
import java.util.Properties;

/**
 * @Author: gaobo07
 * @Date: 2020/9/27 10:17 上午
 */
public class McqDynamicTableSink implements DynamicTableSink {

    /** Consumed data type of the table. */
    protected final DataType producerDataType;

    /** The mcq topic to write to. */
    protected final String topic;

    private final String nameServerAddress;

    private final String group;

    private final String tag;

    /** Sink format for encoding records to mcq. */
    public final EncodingFormat<McqSerializationSchema<RowData>> encodingFormat;

    protected McqDynamicTableSink(
            DataType producerDataType,
            String topic,
            String nameServerAddress,
            String group,
            String tag,
            EncodingFormat<McqSerializationSchema<RowData>> encodingFormat) {
        this.producerDataType = Preconditions.checkNotNull(producerDataType, "Producer data type must not be null.");
        this.topic = Preconditions.checkNotNull(topic, "Topic must not be null.");
        this.nameServerAddress = Preconditions.checkNotNull(nameServerAddress, "NameServerAddress must not be null.");
        this.group = Preconditions.checkNotNull(group, "Group must not be null.");
        this.encodingFormat = Preconditions.checkNotNull(encodingFormat, "Encoding format must not be null.");
        this.tag = tag;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return this.encodingFormat.getChangelogMode();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        McqSerializationSchema<RowData> serializationSchema =
                this.encodingFormat.createRuntimeEncoder(context, this.producerDataType);

        Properties properties = new Properties();
        properties.setProperty(RocketMQConfig.NAME_SERVER_ADDR, nameServerAddress);
        properties.setProperty(RocketMQConfig.PRODUCER_GROUP, group);
        properties.setProperty(RocketMQConfig.CONSUMER_TOPIC, topic);
        properties.setProperty(RocketMQConfig.CONSUMER_TAG, tag);

        final SinkFunction<RowData> mcqProducer = createMcqProducer(
                topic,
                properties,
                serializationSchema);

        return SinkFunctionProvider.of(mcqProducer);
    }

    /**
     * Returns the version-specific MCQ producer.
     *
     * @param topic               mcq topic to produce to.
     * @param properties          Properties for the mcq producer.
     * @param serializationSchema Serialization schema to use to create mcq records.
     * @return The version-specific mcq producer
     */
    protected SinkFunction<RowData> createMcqProducer(
            String topic,
            Properties properties,
            McqSerializationSchema<RowData> serializationSchema){

        return new RocketMQSink<RowData>(
                    serializationSchema,
                    new DefaultTopicSelector(topic),
                    properties);
    }

    @Override
    public DynamicTableSink copy() {
        return new McqDynamicTableSink(
                this.producerDataType,
                this.topic,
                this.nameServerAddress,
                this.group,
                this.tag,
                this.encodingFormat);
    }

    @Override
    public String asSummaryString() {
        return "mcq table sink";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final McqDynamicTableSink that = (McqDynamicTableSink) o;
        return Objects.equals(producerDataType, that.producerDataType) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(nameServerAddress, that.nameServerAddress) &&
                Objects.equals(group, that.group) &&
                Objects.equals(encodingFormat, that.encodingFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                producerDataType,
                topic,
                nameServerAddress,
                group,
                encodingFormat);
    }

}
