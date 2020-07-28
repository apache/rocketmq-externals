package org.apache.rocketmq.connect.es.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;

public class ConfigManage {

	private Map<String, MapperConfig>  mapperConfigMap = new ConcurrentHashMap<String, MapperConfig>();
	
	private Map<String ,String> likeTableNameMap = new ConcurrentHashMap<String, String>();
	
	
	private Map<String , RestHighLevelClient> restHighLevelClient = new HashMap<>();
	
	
	public MapperConfig getMapperConfig(String tableName) {
		
		MapperConfig mapperConfig  = mapperConfigMap.get(tableName);
		if(Objects.isNull(mapperConfig)) {
			synchronized(this) {
				for(Map.Entry<String, String>  entry :  likeTableNameMap.entrySet()) {
					if( tableName.startsWith(entry.getValue())) {
						mapperConfig  = mapperConfigMap.get(tableName);
						mapperConfigMap.put(entry.getValue(), mapperConfig);
					}
				}
			}
		}
		return mapperConfig;
	}
	
	public void setMapperConfig(MapperConfig mapperConfig) {
		
	}
	
	public RestHighLevelClient getRestHighLevelClient(String clietName) {
		return restHighLevelClient.get(clietName);
	}
	
	public void setElasticSearchConfig(ElasticSearchConfig elasticSearchConfig) {
		String serverAddress = elasticSearchConfig.getServerAddress();
		
		int index = serverAddress.indexOf("//");
		if(index == -1) {
			
		}
		String protocol = serverAddress.substring(0 , index);
		String[] serverAddressArray = StringUtils.split(serverAddress.substring(index+2 ),',');
		HttpHost[] httpHosts = new HttpHost[serverAddressArray.length];
		for(int i = 0 ; i < serverAddressArray.length ; i++) {
			String address  = serverAddressArray[i];
			String[] addressArray = StringUtils.split(address,":");
			httpHosts[i] = new HttpHost(addressArray[0],Integer.valueOf(addressArray[1]),protocol);
		}
		
		
		RestClientBuilder builder = RestClient.builder(httpHosts);
		
		builder.setRequestConfigCallback(new RequestConfigCallback() {
			
			@Override
			public Builder customizeRequestConfig(Builder requestConfigBuilder) {
				
				return null;
			}
		});
		
		restHighLevelClient.put(elasticSearchConfig.getName(), new RestHighLevelClient(builder));
	}
}
