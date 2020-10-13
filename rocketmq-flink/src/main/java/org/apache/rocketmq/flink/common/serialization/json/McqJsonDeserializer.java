package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.JsonRowDataDeserializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @Author: gaobo07
 * @Date: 2020/9/28 5:58 下午
 */
public class McqJsonDeserializer implements DeserializationSchema<RowData> {

    private static final Logger log = LoggerFactory.getLogger(McqJsonDeserializer.class);

    private JsonRowDataDeserializationSchema jsonDeserializationSchema;
    private final TypeInformation<RowData> producedTypeInfo;
    private final boolean ignoreParseErrors;

    public McqJsonDeserializer(
            RowType rowType,
            TypeInformation<RowData> producedTypeInfo,
            boolean ignoreParseErrors,
            TimestampFormat timestampFormatOption) {
        this.producedTypeInfo = producedTypeInfo;
        this.ignoreParseErrors = ignoreParseErrors;
        this.jsonDeserializationSchema = new JsonRowDataDeserializationSchema(
                rowType,
                // the result type is never used, so it's fine to pass in Canal's result type
                producedTypeInfo,
                // ignoreParseErrors already contains the functionality of failOnMissingField
                false,
                ignoreParseErrors,
                timestampFormatOption);
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        // return the type information required by Flink's core interfaces
        return producedTypeInfo;
    }

    @Override
    public RowData deserialize(byte[] message) throws IOException {
        try {
            RowData row = jsonDeserializationSchema.deserialize(message);
            //因为是rmq，因此这里只有新增场景存在，若是mysql等，则需考虑修改，删除场景
            RowKind kind = RowKind.INSERT;
            row.setRowKind(kind);
            // convert to internal data structure
            return row;
        } catch (Exception e){
            if(!ignoreParseErrors){
                throw new IOException(String.format("deserialize json message error, the json message is '%s'", message));
            }
            log.error("deserialize json message error:{}", e);
        }
        return null;
    }

    @Override
    public boolean isEndOfStream(RowData nextElement) {
        return false;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        McqJsonDeserializer that = (McqJsonDeserializer) o;
        return ignoreParseErrors == that.ignoreParseErrors &&
                Objects.equals(jsonDeserializationSchema, that.jsonDeserializationSchema) &&
                Objects.equals(producedTypeInfo, that.producedTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonDeserializationSchema, producedTypeInfo, ignoreParseErrors);
    }

}
