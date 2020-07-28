package org.apache.rocketmq.connect.es;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.connect.es.config.ConfigManage;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.model.Model;
import org.apache.rocketmq.connect.es.model.ModelProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;

public class EsSinkTask extends SinkTask {

	private static final Map<FieldType, Class<?>> FIELDTYPE_CLASS = new HashMap<FieldType, Class<?>>();

	 private static final Logger log = LoggerFactory.getLogger("");
	
	static {
		FIELDTYPE_CLASS.put(FieldType.INT32, Integer.class);
		FIELDTYPE_CLASS.put(FieldType.INT64, Long.class);
		FIELDTYPE_CLASS.put(FieldType.BIG_INTEGER, BigInteger.class);
		FIELDTYPE_CLASS.put(FieldType.FLOAT32, Float.class);
		FIELDTYPE_CLASS.put(FieldType.FLOAT64, Double.class);
		FIELDTYPE_CLASS.put(FieldType.INT32, Integer.class);
	}

	private static Pattern linePattern = Pattern.compile("_(\\w)");

	public static String lineToHump(String str) {
		str = str.toLowerCase();
		Matcher matcher = linePattern.matcher(str);
		StringBuffer sb = new StringBuffer();
		StringUtils.split("_");
		while (matcher.find()) {
			matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private Model model = new ModelProxy();

	private ConfigManage configManage = new ConfigManage();

	@Override
	public void start(KeyValue config) {
		// 读取配置
	}

	@Override
	public void stop() {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void put(Collection<SinkDataEntry> sinkDataEntries) {

		for (SinkDataEntry sinkDataEntry : sinkDataEntries) {
			Schema schema = sinkDataEntry.getSchema();
			// 获得表对应的配置
			MapperConfig mapperConfig = configManage.getMapperConfig(schema.getName());
			if(Objects.isNull(mapperConfig)) {
				continue;
			}
			List<Field> fields = schema.getFields();
			Object[] payload = sinkDataEntry.getPayload();
			JSONObject rowBeforeUpdateData = new JSONObject();
			JSONObject rowData = new JSONObject();
			Map<String, String> mapper = mapperConfig.getMapper();
			for (Field field : fields) {
				Class<?> typeClazz = FIELDTYPE_CLASS.get(field.getType());
				// 1. 映射关系，2. 字段名，3. 驼峰命名
				String keyName = Objects.isNull(mapper) ? field.getName() : mapper.get(field.getName());
				JSONArray jsonArray = JSON.parseArray((String) payload[field.getIndex()]);
				rowData.put(keyName, jsonArray.getObject(field.getIndex(), typeClazz));
				rowBeforeUpdateData.put(keyName, jsonArray.getObject(field.getIndex(), typeClazz));
			}

			SyncMetadata syncMetadata = new SyncMetadata();
			syncMetadata.setRowData(rowData);
			syncMetadata.setRowBeforeUpdateData(rowBeforeUpdateData);
			syncMetadata.setSinkDataEntry(sinkDataEntry);
			syncMetadata.setMapperConfig(mapperConfig);

			if (Objects.equals(EntryType.CREATE, sinkDataEntry.getEntryType())) {
				model.create(syncMetadata);
			} else if (Objects.equals(EntryType.DELETE, sinkDataEntry.getEntryType())) {
				model.delete(syncMetadata);
			} else if (Objects.equals(EntryType.UPDATE, sinkDataEntry.getEntryType())) {
				model.update(syncMetadata);
			}
		}
	}

	@Override
	public void commit(Map<QueueMetaData, Long> offsets) {
		// TODO Auto-generated method stub

	}

}
