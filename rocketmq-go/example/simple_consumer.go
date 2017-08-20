/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main

import (
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/api/model"
	//"github.com/golang/glog"
	//"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/message"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/util"
	"github.com/golang/glog"
)

func main() {
	//ch := make(chan string, 10)
	//for i := 0; i < 11; i++ {
	//	ch<-"2345"
	//}
	mmp := util.New()
	go func() {
		i := 1
		for true {
			i *= 3
			mmp.Set(util.IntToString(i), "2345")
		}
	}()
	go func() {
		i := 1
		for true {
			i *= 4
			mmp.Set(util.IntToString(i), "2345")
		}
	}()

	go func() {
		for true {
			glog.Info(len(mmp.Keys()))
		}
	}()
	select {}

}
