package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.EncodingFormatFactory;

/**
 * @Author: gaobo07
 * @Date: 2020/10/9 5:36 下午
 */
@PublicEvolving
public interface McqSerializationFormatFactory extends EncodingFormatFactory<McqSerializationSchema<RowData>> {

}
