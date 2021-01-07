package org.apache.rocketmq.connect.es.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManage {
	
	private static final Logger log = LoggerFactory.getLogger(ConfigManage.class);
	

	private Map<String, MapperConfig> tableNameTomapperConfigMap = new ConcurrentHashMap<String, MapperConfig>();

	private Map<String, MapperConfig> mapperNameTomapperConfigMap = new ConcurrentHashMap<String, MapperConfig>();
	
	private Map<String, String> likeTableNameMap = new ConcurrentHashMap<String, String>();

	private Map<String, RestHighLevelClient> restHighLevelClient = new HashMap<>();
	
	private RestHighLevelClient defaultClient;

	public MapperConfig getMapperConfig(String tableName) {

		MapperConfig mapperConfig = tableNameTomapperConfigMap.get(tableName);
		if (Objects.isNull(mapperConfig)) {
			synchronized (this) {
				for (Map.Entry<String, String> entry : likeTableNameMap.entrySet()) {
					if (tableName.startsWith(entry.getValue())) {
						mapperConfig = tableNameTomapperConfigMap.get(entry.getValue());
						tableNameTomapperConfigMap.put(tableName, mapperConfig);
						break;
					}
				}
			}
		}
		return mapperConfig;
	}

	public  MapperConfig getMapperConfigByMapperName(String mapperName) {
		return mapperNameTomapperConfigMap.get(mapperName);
	}
	
	public void setMapperConfig(MapperConfig mapperConfig) {
		mapperNameTomapperConfigMap.put(mapperConfig.getMapperName(), mapperConfig);
		tableNameTomapperConfigMap.put(mapperConfig.getTableName(), mapperConfig);
		likeTableNameMap.put(mapperConfig.getTableName(), mapperConfig.getTableName());
	}

	public RestHighLevelClient getRestHighLevelClient(String clietName) {
		return restHighLevelClient.get(clietName);
	}
	
	public RestHighLevelClient getDefaultElasticSearchConfig() {
		return defaultClient;
	}

	public void setDefaultElasticSearchConfig(ElasticSearchConfig elasticSearchConfig) {
		this.defaultClient = createRestHighLevelClient(elasticSearchConfig);
	}
	
	public void setElasticSearchConfig(List<ElasticSearchConfig> elasticSearchConfigList) {
		if(Objects.isNull(elasticSearchConfigList)) {
			return;
		}
		for(ElasticSearchConfig elasticSearchConfig : elasticSearchConfigList) {
			restHighLevelClient.put(elasticSearchConfig.getName(), createRestHighLevelClient(elasticSearchConfig));
		}
	}
	
	private RestHighLevelClient createRestHighLevelClient(ElasticSearchConfig elasticSearchConfig) {
		String serverAddress = elasticSearchConfig.getServerAddress();

		int index = serverAddress.indexOf("://");
		if (index == -1) {

		}
		String protocol = serverAddress.substring(0, index);
		String[] serverAddressArray = StringUtils.split(serverAddress.substring(index + 3), ',');
		HttpHost[] httpHosts = new HttpHost[serverAddressArray.length];
		for (int i = 0; i < serverAddressArray.length; i++) {
			String address = serverAddressArray[i];
			String[] addressArray = StringUtils.split(address, ":");
			httpHosts[i] = new HttpHost(addressArray[0], Integer.valueOf(addressArray[1]), protocol);
		}

		RestClientBuilder builder = RestClient.builder(httpHosts);

		builder.setRequestConfigCallback(new RequestConfigCallback() {

			@Override
			public Builder customizeRequestConfig(Builder requestConfigBuilder) {

				return requestConfigBuilder;
			}
		});
		return new RestHighLevelClient(builder);
	}
	
	public void close() {
		for(Entry<String, RestHighLevelClient> entry : restHighLevelClient.entrySet()) {
			try {
				entry.getValue().close();
			} catch (IOException e) {
				log.error("RestHighLevelClient close error " ,e);
			}
		}
	}
}
