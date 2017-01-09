package org.apache.rocketmq.console.config;

import com.alibaba.rocketmq.common.MixAll;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tangjie on 2016/11/17.
 */
public class ConfigureInitializer {
    private Logger logger = LoggerFactory.getLogger(ConfigureInitializer.class);
    
    private String nameSrvAddr;

    public String getNameSrvAddr() {
        return nameSrvAddr;
    }

    public void setNameSrvAddr(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    public void init() {
        if(StringUtils.isNotBlank(nameSrvAddr)){
            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, nameSrvAddr);
            logger.info("setNameSrvAddrByProperty nameSrvAddr={}", nameSrvAddr);
        }
    }
}
