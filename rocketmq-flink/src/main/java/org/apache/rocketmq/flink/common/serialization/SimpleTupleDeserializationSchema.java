package org.apache.rocketmq.flink.common.serialization;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;

import java.nio.charset.StandardCharsets;

public class SimpleTupleDeserializationSchema implements KeyValueDeserializationSchema<Tuple2<String, String>> {

    @Override
    public Tuple2<String, String> deserializeKeyAndValue(byte[] key, byte[] value) {
        String keyString = key != null ? new String(key, StandardCharsets.UTF_8) : null;
        String valueString = value != null ? new String(value, StandardCharsets.UTF_8) : null;
        return new Tuple2<>(keyString, valueString);
    }

    @Override
    public TypeInformation<Tuple2<String, String>> getProducedType() {
        return TypeInformation.of(new TypeHint<Tuple2<String,String>>(){});
    }
}
