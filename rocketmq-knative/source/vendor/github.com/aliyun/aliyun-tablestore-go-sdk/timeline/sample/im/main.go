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

	im, err := NewIm(storeBuilder, syncBuilder, timeline.DefaultStreamAdapter)
	if err != nil {
		log.Fatal("init im failed: ", err)
	}

	// user_A send a message to group_1 {"user_A", "user_B", "user_C"}
	fmt.Printf("****user_A send message to group_1****\n")
	groupName := "group_1"
	groupMembers := []string{"user_A", "user_B", "user_C"}
	msg := &timeline.StreamMessage{
		Id:        "client@1",
		Content:   "阿里云的NoSQL数据库是哪个?",
		Timestamp: time.Now().UnixNano(),
		Attr: map[string]interface{}{
			"From": "user_A",
			"AtId": "user_B",
		},
	}
	faileds, err := im.SendGroup(groupName, groupMembers, msg)
	if err != nil {
		log.Fatal("user_A send message to group failed: ", err)
	}
	if len(faileds) != 0 {
		log.Fatal("user_A send message to some user failed, failed user list:", faileds)
	}
	fmt.Printf("****user_A send message to group_1 succeed****\n\n\n")

	// user_A send a message to user_B
	fmt.Printf("****user_A send message to user_B****\n")
	msg = &timeline.StreamMessage{
		Id:        "client@2",
		Content:   "user_B看下group_1群里的问题",
		Timestamp: time.Now().UnixNano(),
		Attr: map[string]interface{}{
			"From": "user_A",
		},
	}
	err = im.Send("user_A", "user_B", msg)
	if err != nil {
		log.Fatal("user_A send message to user_B: ", err)
	}
	fmt.Printf("****user_A send message to user_B succeed****\n\n\n")

	// user_B check sync timeline form last read sequence 0
	fmt.Printf("****user_B read sync messages from lastRead 0****\n")
	lastRead := int64(0)
	entries, err := im.GetSyncMessage("user_B", lastRead)
	if err != nil {
		log.Fatal("user_B get sync messages failed: ", err)
	}
	for _, entry := range entries {
		fmt.Printf("Sync message: sequence %d\n", entry.Sequence)
		smsg := entry.Message.(*timeline.StreamMessage)
		fmt.Printf("From: %s\nMessage detail: %s\nTime: %v\n", smsg.Attr["From"],
			smsg.Content, time.Unix(0, smsg.Timestamp))
		fmt.Println()
	}
	fmt.Println("Now new lastRead sequence:", entries[len(entries)-1].Sequence)
	fmt.Printf("****user_B read sync messages from lastRead 0 succeed****\n\n\n")

	// user_B check group_1 history message
	fmt.Printf("****user_B read group_1 history messages****\n")
	entries, err = im.GetHistoryMessage("group_1", 10)
	if err != nil {
		log.Fatal("user_B get history messages failed: ", err)
	}
	for _, entry := range entries {
		fmt.Printf("History message: sequence %d\n", entry.Sequence)
		smsg := entry.Message.(*timeline.StreamMessage)
		fmt.Printf("From: %s\nMessage detail: %s\nTime: %v\n@: %s\n", smsg.Attr["From"],
			smsg.Content, time.Unix(0, smsg.Timestamp), smsg.Attr["AtId"])
		fmt.Println()
	}
	fmt.Printf("****user_B read group_1 history messages succeed****\n\n\n")

	// user_B check chat with user_A history message
	fmt.Printf("****user_B read chat with user_A history messages****\n")
	entries, err = im.GetHistoryMessage(singChatStoreName("user_B", "user_A"), 10)
	if err != nil {
		log.Fatal("user_B get history messages failed: ", err)
	}
	for _, entry := range entries {
		fmt.Printf("History message: sequence %d\n", entry.Sequence)
		smsg := entry.Message.(*timeline.StreamMessage)
		fmt.Printf("From: %s\nMessage detail: %s\nTime: %v\n", smsg.Attr["From"],
			smsg.Content, time.Unix(0, smsg.Timestamp))
		fmt.Println()
	}
	fmt.Printf("****user_B read chat with user_A history messages succeed****\n\n\n")

	//close im store to avoid async writer goroutine leak
	im.Close()
}
