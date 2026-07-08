package org.apache.rocketmq.connect.es.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Assert;
import org.junit.Test;

public class ConfigManageTest {

	ConfigManage configManage = new ConfigManage();
	
	@Test
	public void setDefaultElasticSearchConfigTest() {
		ElasticSearchConfig elasticSearchConfig = new ElasticSearchConfig();
		elasticSearchConfig.setName("test");
		elasticSearchConfig.setServerAddress("http://27.0.0.1:9200");
		configManage.setDefaultElasticSearchConfig(elasticSearchConfig);
		RestHighLevelClient defaultElastic = configManage.getDefaultElasticSearchConfig();
		Assert.assertNotNull(defaultElastic);
	}
}
