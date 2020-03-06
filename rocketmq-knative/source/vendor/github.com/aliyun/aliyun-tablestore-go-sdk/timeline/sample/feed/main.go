package main

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline"
	"log"
	"time"
)

func main() {
	//set builder config
	storeBuilder := timeline.StoreOption{
		Endpoint:  "<Your instance endpoint>",
		Instance:  "<Your instance name>",
		TableName: "<Your table name>",
		AkId:      "<Your ak id>",
		AkSecret:  "<Your ak secret>",
		TTL:       365 * 24 * 3600, // Data time to alive, eg: almost one year
	}
	syncBuilder := timeline.StoreOption{
		Endpoint:  "<Your instance endpoint>",
		Instance:  "<Your instance name>",
		TableName: "<Your table name>",
		AkId:      "<Your ak id>",
		AkSecret:  "<Your ak secret>",
		TTL:       30 * 24 * 3600, // Data time to alive, eg: almost one month
	}

	// create a feed for user_A
	feedA, err := NewFeed("user_A", storeBuilder, syncBuilder, timeline.DefaultStreamAdapter)
	if err != nil {
		log.Fatal("init feed failed: ", err)
	}

	// create a feed for user_B
	feedB, err := NewFeed("user_B", storeBuilder, syncBuilder, timeline.DefaultStreamAdapter)
	if err != nil {
		log.Fatal("init feed failed: ", err)
	}

	// user_A followed user_B
	feedB.followers = append(feedB.followers, "user_A")

	// user_B post an activity stream
	activity := &timeline.StreamMessage{
		Id:        "client@1",
		Content:   "TableStore is fantastic!",
		Timestamp: time.Now().UnixNano(),
		Attr: map[string]interface{}{
			"Position": "shanghai-ali center",
		},
	}
	failedFollows, err := feedB.Post(activity)
	if err != nil {
		log.Fatal("user_B post new activity failed: ", err)
	}
	if len(failedFollows) != 0 {
		log.Fatal("user_B post new activity to some follower failed, failed user list:", failedFollows)
	}

	// user_A refresh and see user_B's new activity
	lastRead := int64(0)
	entries, err := feedA.Refresh(lastRead)
	if err != nil {
		log.Fatal("user_A refresh activities failed: ", err)
	}
	for _, entry := range entries {
		fmt.Printf("New activity: sequence %d\n", entry.Sequence)
		act := entry.Message.(*timeline.StreamMessage)
		fmt.Printf("Detail: %s\nTime: %v\nPosition: %s\n", act.Content,
			time.Unix(0, act.Timestamp), act.Attr["Position"])
		fmt.Println()
	}

	// user_B check his own history activities
	entries, err = feedB.OwnHistory(100)
	if err != nil {
		log.Fatal("user_B get hisotry activities failed: ", err)
	}
	for _, entry := range entries {
		fmt.Printf("History activity: sequence %d\n", entry.Sequence)
		act := entry.Message.(*timeline.StreamMessage)
		fmt.Printf("Detail: %s\nTime: %v\nPosition: %s\n", act.Content,
			time.Unix(0, act.Timestamp), act.Attr["Position"])
		fmt.Println()
	}
}
