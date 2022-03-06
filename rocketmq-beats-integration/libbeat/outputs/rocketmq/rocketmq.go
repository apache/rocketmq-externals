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

package rocketmq

import (
	"github.com/elastic/beats/v7/libbeat/beat"
	"github.com/elastic/beats/v7/libbeat/common"
	"github.com/elastic/beats/v7/libbeat/logp"
	"github.com/elastic/beats/v7/libbeat/outputs"
	"github.com/elastic/beats/v7/libbeat/outputs/codec"
)

const (
	logSelector = "rocketmq"
)

func init() {
	outputs.RegisterType("rocketmq", makeRocketmq)
}

func makeRocketmq(
	_ outputs.IndexManager,
	beat beat.Info,
	observer outputs.Observer,
	cfg *common.Config,
) (outputs.Group, error) {
	log := logp.NewLogger(logSelector)

	config, err := readConfig(cfg)
	if err != nil {
		return outputs.Fail(err)
	}

	index := beat.Beat

	codec, err := codec.CreateEncoder(beat, config.Codec)
	if err != nil {
		return outputs.Fail(err)
	}

	client := &client{log: log,
		observer:    observer,
		index:       index,
		codec:       codec,
		namesrvAddr: config.Nameservers,
		topic:       config.Topic,
		group:       config.Group,
		timeout:     config.SendTimeout,
		maxRetries:  config.MaxRetries}

	//retry in producer, so set it 0 in the following function
	return outputs.Success(config.BatchSize, 0, client)
}
