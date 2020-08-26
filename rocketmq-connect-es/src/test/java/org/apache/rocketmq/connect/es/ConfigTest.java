package org.apache.rocketmq.connect.es;

import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;

public class ConfigTest {

	private KeyValue keyValue;
	
	private Config config = new Config();
	
	@Test
	public void testConfig() {
		keyValue = new DefaultKeyValue();
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
