package org.apache.rocketmq.console.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author 莫那·鲁道
 * @date 2019-05-02-21:17
 */
@Component
public class AclEnableHelper {

    @Autowired
    private RMQConfigure rmqConfigure;

    public boolean isAclEnable() {
        String aclEnable = rmqConfigure.getAclEnable();
        if (StringUtils.isNotEmpty(aclEnable) && Boolean.valueOf(aclEnable)) {
            return true;
        }

        return false;
    }

}
