package org.apache.rocketmq.flink.source.reader.deserializer;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.serialization.DeserializationSchema.InitializationContext;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.io.Serializable;

/** An interface for the deserialization of records. */
public interface DeserializationSchema<IN, OUT> extends Serializable, ResultTypeQueryable<OUT> {

    /**
     * Initialization method for the schema. It is called before the actual working methods {@link
     * #deserialize} and thus suitable for one time setup work.
     *
     * <p>The provided {@link InitializationContext} can be used to access additional features such
     * as e.g. registering user metrics.
     *
     * @param context Contextual information that can be used during initialization.
     */
    @PublicEvolving
    default void open(InitializationContext context) throws Exception {}

    /**
     * Deserializes the byte message.
     *
     * <p>Can output multiple records through the {@link Collector}. Note that number and size of
     * the produced records should be relatively small. Depending on the source implementation
     * records can be buffered in memory or collecting records might delay emitting checkpoint
     * barrier.
     *
     * @param record The record to deserialize.
     * @param out The collector to put the resulting messages.
     */
    @PublicEvolving
    void deserialize(IN record, Collector<OUT> out) throws IOException;
}
