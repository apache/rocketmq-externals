package org.apache.rocketmq.connect.runtime.service;

import io.openmessaging.MessagingAccessPoint;
import io.openmessaging.OMS;
import java.util.HashMap;
import java.util.Map;

/**
 * The wrapper of MessagingAccessPoint, manage all MessagingAccessPoint.
 */
public class MessagingAccessWrapper {

    private Map<String, MessagingAccessPoint> accessPointMap;

    public MessagingAccessWrapper(){
        this.accessPointMap = new HashMap<>();
    }

    /**
     * Get a MessagingAccessPoint instance. If it not contained in the memory, create it.
     * @param omsDriverUrl
     * @return
     */
    public MessagingAccessPoint getMessageAccessPoint(String omsDriverUrl){

        if(!accessPointMap.containsKey(omsDriverUrl)){
            MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint(omsDriverUrl);
            messagingAccessPoint.startup();
            accessPointMap.put(omsDriverUrl, messagingAccessPoint);
        }
        return accessPointMap.get(omsDriverUrl);
    }

    /**
     * Remove all MessagingAccessPoint instance.
     */
    public void removeAllMessageAccessPoint(){

        for(String omsDriverUrl : accessPointMap.keySet()){
            MessagingAccessPoint messagingAccessPoint = accessPointMap.get(omsDriverUrl);
            messagingAccessPoint.shutdown();
            accessPointMap.remove(omsDriverUrl);
        }
    }
}
