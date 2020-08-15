package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"

	"github.com/gogap/logs"
	"github.com/aliyun/aliyun-mns-go-sdk"
)

type appConf struct {
	Url             string `json:"url"`
	AccessKeyId     string `json:"access_key_id"`
	AccessKeySecret string `json:"access_key_secret"`
}

func main() {
	conf := appConf{}

	if bFile, e := ioutil.ReadFile("app.conf"); e != nil {
		panic(e)
	} else {
		if e := json.Unmarshal(bFile, &conf); e != nil {
			panic(e)
		}
	}

	client := ali_mns.NewAliMNSClient(conf.Url,
		conf.AccessKeyId,
		conf.AccessKeySecret)

	// 1. create a queue for receiving pushed messages
	queueManager := ali_mns.NewMNSQueueManager(client)
	err := queueManager.CreateSimpleQueue("testQueue")
	if err != nil && !ali_mns.ERR_MNS_QUEUE_ALREADY_EXIST_AND_HAVE_SAME_ATTR.IsEqual(err) {
		fmt.Println(err)
		return
	}

	// 2. create the topic
	topicManager := ali_mns.NewMNSTopicManager(client)
	// topicManager.DeleteTopic("testTopic")
	err = topicManager.CreateSimpleTopic("testTopic")
	if err != nil && !ali_mns.ERR_MNS_TOPIC_ALREADY_EXIST_AND_HAVE_SAME_ATTR.IsEqual(err) {
		fmt.Println(err)
		return
	}

	// 3. subscribe to topic, the endpoint is set to be a queue in this sample
	topic := ali_mns.NewMNSTopic("testTopic", client)
	sub := ali_mns.MessageSubsribeRequest{
		Endpoint:            topic.GenerateQueueEndpoint("testQueue"),
		NotifyContentFormat: ali_mns.SIMPLIFIED,
	}

	// topic.Unsubscribe("SubscriptionNameA")
	err = topic.Subscribe("SubscriptionNameA", sub)
	if err != nil && !ali_mns.ERR_MNS_SUBSCRIPTION_ALREADY_EXIST_AND_HAVE_SAME_ATTR.IsEqual(err) {
		fmt.Println(err)
		return
	}

	/*
		Please refer to
		https://help.aliyun.com/document_detail/27434.html
		before using mail push

		sub = ali_mns.MessageSubsribeRequest{
	        Endpoint:  topic.GenerateMailEndpoint("a@b.com"),
	        NotifyContentFormat: ali_mns.SIMPLIFIED,
	    }
	    err = topic.Subscribe("SubscriptionNameB", sub)
	    if (err != nil && !ali_mns.ERR_MNS_SUBSCRIPTION_ALREADY_EXIST_AND_HAVE_SAME_ATTR.IsEqual(err)) {
	        fmt.Println(err)
	        return
	    }
	*/

	// 4. now publish message
	msg := ali_mns.MessagePublishRequest{
		MessageBody: "hello topic <\"aliyun-mns-go-sdk\">",
		MessageAttributes: &ali_mns.MessageAttributes{
			MailAttributes: &ali_mns.MailAttributes{
				Subject:     "AAA中文",
				AccountName: "BBB",
			},
		},
	}
	_, err = topic.PublishMessage(msg)
	if err != nil {
		fmt.Println(err)
		return
	}

	// 5. receive the message from queue
	queue := ali_mns.NewMNSQueue("testQueue", client)

	endChan := make(chan int)
	respChan := make(chan ali_mns.MessageReceiveResponse)
	errChan := make(chan error)
	go func() {
		select {
		case resp := <-respChan:
			{
				logs.Pretty("response:", resp)
				fmt.Println("change the visibility: ", resp.ReceiptHandle)
				if ret, e := queue.ChangeMessageVisibility(resp.ReceiptHandle, 5); e != nil {
					fmt.Println(e)
				} else {
					logs.Pretty("visibility changed", ret)
					fmt.Println("delete it now: ", ret.ReceiptHandle)
					if e := queue.DeleteMessage(ret.ReceiptHandle); e != nil {
						fmt.Println(e)
					}
					endChan <- 1
				}
			}
		case err := <-errChan:
			{
				fmt.Println(err)
				endChan <- 1
			}
		}
	}()

	queue.ReceiveMessage(respChan, errChan, 30)
	<-endChan
}
