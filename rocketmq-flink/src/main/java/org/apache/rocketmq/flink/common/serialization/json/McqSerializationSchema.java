package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.rocketmq.common.message.Message;

import java.io.Serializable;

/**
 * @Author: gaobo07
 * @Date: 2020/10/9 5:43 下午
 */
@PublicEvolving
public interface McqSerializationSchema<T> extends Serializable {


    /**
     * Initialization method for the schema. It is called before the actual working methods
     * {@link #serialize(Object)}} and thus suitable for one time setup work.
     *
     * <p>The provided {@link SerializationSchema.InitializationContext} can be used to access additional
     * features such as e.g. registering user metrics.
     *
     * @param context Contextual information that can be used during initialization.
     */
    @PublicEvolving
    default void open(SerializationSchema.InitializationContext context) throws Exception {
    }

    /**
     * Serializes given element and returns it as a {@link Message}.
     * Message topic and tag is null
     * @param element element to be serialized
     * @return Mcq {@link Message}
     */
    Message serialize(T element);

}
