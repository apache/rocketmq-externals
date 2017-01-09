package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.service.ClusterService;
import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * Created by tangjie on 2016/11/17.
 */
@Controller
@RequestMapping("/cluster")
public class ClusterController {

    @Resource
    private ClusterService clusterService;

    @RequestMapping(value = "/list.query", method = RequestMethod.GET)
    @JsonBody
    public Object list() {
        return clusterService.list();
    }

    @RequestMapping(value = "/brokerConfig.query", method = RequestMethod.GET)
    @JsonBody
    public Object brokerConfig(@RequestParam String brokerAddr) {
        return clusterService.getBrokerConfig(brokerAddr);
    }
}
