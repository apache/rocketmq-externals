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
	"time"

	"github.com/elastic/beats/v7/libbeat/common"
	"github.com/elastic/beats/v7/libbeat/outputs/codec"
)

type rocketmqConfig struct {
	Nameservers []string `config:"nameservers"               validate:"required"`
	Topic       string   `config:"topic" validate:"required"`
	//Tag         string   `config:"tag"`
	Group string `config:"group"`

	SendTimeout time.Duration `config:"send_timeout"             `
	MaxRetries  int           `config:"max_retries"`

	Codec codec.Config `config:"codec"`

	// Max number of events in a batch to send to a single client
	BatchSize int `config:"batch_size" validate:"min=1"`
}

func defaultConfig() rocketmqConfig {
	return rocketmqConfig{
		Nameservers: nil,
		Topic:       "topic-beats",
		SendTimeout: 3 * time.Second,
		MaxRetries:  2,
		BatchSize:   1,
	}
}

func readConfig(cfg *common.Config) (*rocketmqConfig, error) {
	c := defaultConfig()
	if err := cfg.Unpack(&c); err != nil {
		return nil, err
	}
	return &c, nil
}
