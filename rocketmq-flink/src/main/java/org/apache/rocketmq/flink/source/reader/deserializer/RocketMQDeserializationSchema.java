package org.apache.rocketmq.flink.source.reader.deserializer;

import org.apache.rocketmq.common.message.MessageExt;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.serialization.DeserializationSchema.InitializationContext;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.util.List;

/** An interface for the deserialization of RocketMQ records. */
public interface RocketMQDeserializationSchema<T>
        extends DeserializationSchema<List<MessageExt>, T> {

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
    default void open(InitializationContext context) {}

    /**
     * Deserializes the byte message.
     *
     * <p>Can output multiple records through the {@link Collector}. Note that number and size of
     * the produced records should be relatively small. Depending on the source implementation
     * records can be buffered in memory or collecting records might delay emitting checkpoint
     * barrier.
     *
     * @param record The MessageExts to deserialize.
     * @param out The collector to put the resulting messages.
     */
    @PublicEvolving
    void deserialize(List<MessageExt> record, Collector<T> out) throws IOException;
}
