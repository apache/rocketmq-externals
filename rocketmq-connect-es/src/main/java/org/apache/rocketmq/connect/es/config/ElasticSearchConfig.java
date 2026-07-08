package org.apache.rocketmq.connect.es.config;

public class ElasticSearchConfig {

	private String name;
	
	private String serverAddress;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}
	
}
