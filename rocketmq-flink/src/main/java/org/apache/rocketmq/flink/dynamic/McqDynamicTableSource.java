package org.apache.rocketmq.flink.dynamic;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.rocketmq.flink.RocketMQConfig;
import org.apache.rocketmq.flink.RocketMQSource;

import java.util.Properties;

/**
 * @Author: gaobo07
 * @Date: 2020/9/27 10:11 上午
 */
public class McqDynamicTableSource implements ScanTableSource {

    private final String nameServerAddress;
    private final String group;
    private final String topic;
    private final String tag;
    private final String offsetResetTo;
    private final DecodingFormat<DeserializationSchema<RowData>> decodingFormat;
    private final DataType producedDataType;

    public McqDynamicTableSource(
            String nameServerAddress,
            String topic,
            String group,
            String tag,
            String offsetResetTo,
            DataType producedDataType,
            DecodingFormat<DeserializationSchema<RowData>> decodingFormat) {
        this.nameServerAddress = nameServerAddress;
        this.group = group;
        this.topic = topic;
        this.tag = tag;
        this.offsetResetTo = offsetResetTo;
        this.producedDataType = producedDataType;
        this.decodingFormat = decodingFormat;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        // in our example the format decides about the changelog mode
        // but it could also be the source itself
        return decodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {

        // create runtime classes that are shipped to the cluster
        DeserializationSchema<RowData> deserializer = decodingFormat.createRuntimeDecoder(runtimeProviderContext, producedDataType);

        Properties consumerProps = new Properties();
        consumerProps.setProperty(RocketMQConfig.NAME_SERVER_ADDR, nameServerAddress);
        consumerProps.setProperty(RocketMQConfig.CONSUMER_GROUP, group);
        consumerProps.setProperty(RocketMQConfig.CONSUMER_TOPIC, topic);
        if(StringUtils.isNotBlank(tag)){
            consumerProps.setProperty(RocketMQConfig.CONSUMER_TAG, tag);
        }
        if(StringUtils.isNotBlank(offsetResetTo)){
            consumerProps.setProperty(RocketMQConfig.CONSUMER_OFFSET_RESET_TO, offsetResetTo);
        }

        RocketMQSource<RowData> rocketMQSource = new RocketMQSource(deserializer, consumerProps);

        return SourceFunctionProvider.of(rocketMQSource, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new McqDynamicTableSource(
                nameServerAddress,
                group,
                topic,
                tag,
                offsetResetTo,
                producedDataType,
                decodingFormat);
    }

    @Override
    public String asSummaryString() {
        return "mcq table source";
    }

}
