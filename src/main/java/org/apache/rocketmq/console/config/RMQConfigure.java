package org.apache.rocketmq.console.config;

import com.alibaba.rocketmq.common.MixAll;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by songyongzhong on 2017/1/17.
 */
@Configuration
@ConfigurationProperties(prefix = "rocketmq.namesrv")
public class RMQConfigure {

    private Logger logger = LoggerFactory.getLogger(RMQConfigure.class);

    private String addr;

    private String consoleCollectData;

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
        if (StringUtils.isNotBlank(addr)) {
            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, addr);
            logger.info("setNameSrvAddrByProperty nameSrvAddr={}", addr);
        }
    }

    public String getConsoleCollectData() {
        if (!Strings.isNullOrEmpty(consoleCollectData)){
            return consoleCollectData.trim();
        }
        return consoleCollectData;
    }

    public void setConsoleCollectData(String consoleCollectData) {
        this.consoleCollectData = consoleCollectData;
        if (!Strings.isNullOrEmpty(consoleCollectData)) {
            logger.info("setConsoleCollectData consoleCollectData={}", consoleCollectData);
        }
    }
}
