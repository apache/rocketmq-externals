/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.console.web;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class WebStaticApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("<body ng-controller");
	}

	//@Test
	public void testCss() throws Exception {
		ResponseEntity<String> entity = this.restTemplate.getForEntity(
				"/vendor/bootstrap/css/bootstrap.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("body");
		assertThat(entity.getHeaders().getContentType())
				.isEqualTo(MediaType.valueOf("text/css;charset=UTF-8"));
	}

	@Test
	public void testResources() throws Exception {
		for(Map.Entry<String,String> entry : resourcesMap().entrySet()){
			resources(entry.getValue(),entry.getKey());
		}
	}

	private Map<String,String> resourcesMap(){
		Map<String,String> map = Maps.newHashMap();
		map.put("text/css;charset=UTF-8","/vendor/bootstrap/css/bootstrap.css");
		map.put("text/css;charset=UTF-8","/vendor/bootstrap/css/bootstrap-theme.css");
		map.put("text/css;charset=UTF-8","/vendor/bootstrap-material-design/css/bootstrap-material-design.css");
		map.put("text/css;charset=UTF-8","/vendor/bootstrap-material-design/css/ripples.css");
		return map;
	}

	private void resources(String path,String type){
		ResponseEntity<String> entity = this.restTemplate.getForEntity(path, String.class);
		assertThat(entity.getHeaders().getContentType())
			.isEqualTo(MediaType.valueOf(type));
	}

}
