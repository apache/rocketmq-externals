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
	"fmt"
	"time"
	"strings"
	cloudevents "github.com/cloudevents/sdk-go"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/kncloudevents"
	 "k8s.io/client-go/kubernetes"
	"golang.org/x/net/context"
    "github.com/apache/rocketmq-client-go"
    "github.com/apache/rocketmq-client-go/consumer"
	"github.com/apache/rocketmq-client-go/primitive"
)

type Adapter struct {
	K8sClient kubernetes.Interface
	Namespace string
	SecretName string
	SecretKey string
	Topic string
	NamesrvAddr  string
	SubscriptionID string
	SinkURI string
	AdCode string
    pushConsumer rocketmq.PushConsumer
	ceClient          cloudevents.Client
	transformer       bool
	transformerClient cloudevents.Client
}

func (a *Adapter) Start(ctx context.Context) error {

	var err error
	if a.ceClient == nil {
		if a.ceClient, err = kncloudevents.NewDefaultClient(a.SinkURI); err != nil {
			return fmt.Errorf("failed to create cloudevent client: %s", err.Error())
		}
	}

	namesrvAddrs := strings.Fields(a.NamesrvAddr)
	a.pushConsumer, _ = rocketmq.NewPushConsumer(
		consumer.WithGroupName("consumerGroup"),
		consumer.WithNameServer(namesrvAddrs),
	)

	return a.consumerStart()
}

func (a *Adapter) consumerStart() error  {

	err := a.pushConsumer.Subscribe(a.Topic,consumer.MessageSelector{},a.receiveMsg)
	if err != nil {
		fmt.Println(err.Error())
	}

	// Note: start after subscribe
	err = a.pushConsumer.Start()
	if err != nil {
		fmt.Println(err.Error())
		return nil
	}
        
	for true {
		time.Sleep(time.Hour)
	}
	err = a.pushConsumer.Shutdown()
	if err != nil {
		fmt.Printf("shutdown Consumer error: %s", err.Error())
		return nil
	}
	return err;
}

func (a *Adapter)  receiveMsg(ctx context.Context, msgs ...*primitive.MessageExt) (consumer.ConsumeResult, error) {
	for i := range msgs {
		fmt.Printf("subscribe callback: %v \n", msgs[i])
		event := cloudevents.NewEvent(cloudevents.VersionV02)
		event.SetID(msgs[i].MsgId)
		event.SetTime(time.Now())
		event.SetDataContentType(*cloudevents.StringOfApplicationJSON())
		event.SetSource("rocketmq")
		event.SetData(msgs[i])
		event.SetType("RocketMQEventType")

		_, err := a.ceClient.Send(ctx, event)
		if err != nil {
			fmt.Println("error Send cloud event %s", err.Error())
		}
	}
	return consumer.ConsumeSuccess, nil
}




