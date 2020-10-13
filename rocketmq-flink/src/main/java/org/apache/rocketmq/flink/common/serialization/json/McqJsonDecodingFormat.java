package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;

/**
 * @Author: gaobo07
 * @Date: 2020/9/28 5:57 下午
 */
public class McqJsonDecodingFormat implements DecodingFormat<DeserializationSchema<RowData>> {

    private final boolean ignoreParseErrors;

    private final TimestampFormat timestampFormatOption;

    public McqJsonDecodingFormat(boolean ignoreParseErrors, TimestampFormat timestampFormatOption){
        this.ignoreParseErrors = ignoreParseErrors;
        this.timestampFormatOption = timestampFormatOption;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeserializationSchema<RowData> createRuntimeDecoder(DynamicTableSource.Context context, DataType producedDataType) {
        // create type information for the DeserializationSchema
        final TypeInformation<RowData> producedTypeInfo = (TypeInformation<RowData>) context.createTypeInformation(producedDataType);

        // most of the code in DeserializationSchema will not work on internal data structures
        // create a converter for conversion at the end
        final DynamicTableSource.DataStructureConverter converter = context.createDataStructureConverter(producedDataType);

        RowType rowType = (RowType) producedDataType.getLogicalType();

        // create runtime class
        return new McqJsonDeserializer(rowType, producedTypeInfo, ignoreParseErrors, timestampFormatOption);
    }

    @Override
    public ChangelogMode getChangelogMode() {
        // define that this format can produce INSERT and DELETE rows
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.DELETE)
                .build();
    }

}
