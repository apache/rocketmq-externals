package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;

/**
 * @Author: gaobo07
 * @Date: 2020/10/7 3:12 下午
 */
public class McqJsonEncodeingFormat implements EncodingFormat<McqSerializationSchema<RowData>> {

    private final TimestampFormat timestampFormat;
    private final Integer sinkKeyPos;

    public McqJsonEncodeingFormat(TimestampFormat timestampFormat, Integer sinkKeyPos){
        this.timestampFormat = timestampFormat;
        this.sinkKeyPos = sinkKeyPos;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.DELETE)
                .build();
    }

    @Override
    public McqSerializationSchema<RowData> createRuntimeEncoder(
            DynamicTableSink.Context context, DataType consumedDataType) {
        final RowType rowType = (RowType) consumedDataType.getLogicalType();
        return new McqJsonSerializer(rowType, timestampFormat, sinkKeyPos);
    }

}
