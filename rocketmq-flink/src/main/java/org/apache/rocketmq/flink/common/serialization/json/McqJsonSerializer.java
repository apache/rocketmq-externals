package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.formats.json.JsonRowDataSerializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.rocketmq.common.message.Message;

import java.util.Objects;

/**
 * @Author: gaobo07
 * @Date: 2020/10/7 3:12 下午
 */
public class McqJsonSerializer implements McqSerializationSchema<RowData> {

    private final JsonRowDataSerializationSchema jsonSerializer;

    private final TimestampFormat timestampFormat;

    private final Integer sinkKeyPos;

    public McqJsonSerializer(RowType rowType, TimestampFormat timestampFormat, Integer sinkKeyPos){
        this.timestampFormat = timestampFormat;
        this.jsonSerializer = new JsonRowDataSerializationSchema(rowType, timestampFormat);
        this.sinkKeyPos = sinkKeyPos;
    }

    @Override
    public Message serialize(RowData element) {
        String key = String.valueOf(((GenericRowData) element).getField(sinkKeyPos));
        Message message = new Message("", "", key, jsonSerializer.serialize(element));
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        McqJsonSerializer that = (McqJsonSerializer) o;
        return Objects.equals(timestampFormat, that.timestampFormat) &&
                Objects.equals(jsonSerializer, that.jsonSerializer) &&
                sinkKeyPos.equals(that.sinkKeyPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonSerializer, timestampFormat, sinkKeyPos);
    }

}
