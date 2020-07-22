package org.apache.rocketmq.connect.runtime.rest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URLEncoder;

public class RestSender {
    public String sendHttpRequest(String baseUrl, String configs){
        try {
            CloseableHttpClient client = null;
            CloseableHttpResponse response = null;
            try {
                String encoded_configs = URLEncoder.encode(configs,"utf-8");
                HttpGet httpGet = new HttpGet(baseUrl + encoded_configs);
                client = HttpClients.createDefault();
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                System.out.println(result);
                return result;
            } finally {
                if (response != null) {
                    response.close();
                }
                if (client != null) {
                    client.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
