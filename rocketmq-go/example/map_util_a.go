package main

import (
	"encoding/json"
	"fmt"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util/structs"
)

func main() {
	config := rocketmqm.NewRocketMqConsumerConfig()
	print(structs.Map(config))
	print(util.Struct2Map(config))
}
func print(m map[string]interface{}) {
	bb, _ := json.Marshal(m)
	fmt.Println(string(bb))
}
