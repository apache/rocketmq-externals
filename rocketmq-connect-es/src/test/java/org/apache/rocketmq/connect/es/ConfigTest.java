package org.apache.rocketmq.connect.es;

import org.apache.rocketmq.connect.es.config.ConfigManage;
import org.junit.Before;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;

public class ConfigTest {

	private KeyValue keyValue;
	
	private Config config;
	
	private ConfigManage configManage;
	
	@Before
	public void init(){
		config = new Config(configManage);
	}
	
	@Test
	public void testConfig() {
		keyValue = new DefaultKeyValue();
		keyValue.put("topicNames", "testTopic,testTopic2");
		/*for(int i = 0 ; i < 2 ; i++) {
			String head = "sinkclient[" + i +"]";
			keyValue.put(head+".name", "client-" + i);
			keyValue.put(head+".serverAddress", "serverAddress-" + i);
		}*/
		
		for(int i = 0 ; i < 1 ; i++) {
			String head = "relation[" + i + "]";
			String mainHead = head+"." +"mainMapperConfig";
			keyValue.put(mainHead+".mapperName", "client-" + i);
			keyValue.put(mainHead+".tableName", "client-" + i);
			keyValue.put(mainHead+".clientName", "client-" + i);
			keyValue.put(mainHead+".mapperName", "client-" + i);
			keyValue.put(mainHead+".mapperName", "client-" + i);
			
			String mainFieldHead = mainHead+"."+"fieldAndKeyMapper";
			keyValue.put(mainFieldHead+".id", "id-" + i);
			keyValue.put(mainFieldHead+".name", "name-" + i);
			
			
		}
		
		config.load(keyValue);
	}
	
	

	
}
