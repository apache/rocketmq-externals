package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.service.ProducerService;
import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 */
@Controller
@RequestMapping("/producer")
public class ProducerController  {

    @Resource
    private ProducerService producerService;

    @RequestMapping(value = "/producerConnection.query", method = {RequestMethod.GET})
    @JsonBody
    public Object producerConnection(@RequestParam String producerGroup, @RequestParam String topic) {
        return producerService.getProducerConnection(producerGroup, topic);
    }
}
