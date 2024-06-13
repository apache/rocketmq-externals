package org.apache.rocketmq.connect.es.config;

public enum NamingMethod {
    // 驼峰映射
	HUMP, 
    // 关系映射
    MAPPER,
    // 字段名映射
    FIELDNAME;
}
