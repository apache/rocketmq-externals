package main

import "testing"
import (
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
)

func TestAdda(t *testing.T) {
	var clienConfig = &rocketmq_api_model.ClientConfig{}
	clienConfig.SetNameServerAddress("127.0.0.1:9876")
	testa := rocketmq_api.InitRocketMQController(clienConfig)
	fmt.Print(testa)
}
