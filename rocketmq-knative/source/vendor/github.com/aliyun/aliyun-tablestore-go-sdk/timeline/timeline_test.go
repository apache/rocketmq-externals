package timeline

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/writer"
	"math/rand"
	"os"
	"testing"
	"time"
)

func BenchmarkTmLine_BatchStore_Concurrent(b *testing.B) {
	option := initStoreOptionFromEnv(b.Name())
	store, err := NewDefaultStore(*option)
	if err != nil {
		b.Fatal(err)
	}
	defer store.Close()
	defer deleteTable(store.(*DefaultStore), b.Name())

	var messageLength = 200
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			msg := randomMessage(messageLength)
			id := randomString(8)
			tmLine, err := NewTmLine(id, DefaultStreamAdapter, store)
			if err != nil {
				b.Error(err)
			}
			future, err := tmLine.BatchStore(msg)
			if err != nil {
				b.Error(err)
			}
			_, err = future.Get()
			if err != nil {
				b.Error(err)
			}
		}
	})
}

func BenchmarkTmLine_BatchStore_WriteSpread(b *testing.B) {
	option := initStoreOptionFromEnv(b.Name())
	store, err := NewDefaultStore(*option)
	if err != nil {
		b.Fatal(err)
	}
	defer store.Close()
	defer deleteTable(store.(*DefaultStore), b.Name())

	for i := 0; i < b.N; i++ {
		message := randomMessage(200)
		numOfWrite := 10000
		futures := make([]*promise.Future, numOfWrite)
		for i := 0; i < numOfWrite; i++ {
			tmLine, err := NewTmLine(fmt.Sprintf("%d", i), DefaultStreamAdapter, store)
			if err != nil {
				b.Error(err)
			}
			f, err := tmLine.BatchStore(message)
			if err != nil {
				b.Error(err)
			}
			futures[i] = f
		}
		fanFuture := promise.FanIn(futures...)
		fails, err := fanFuture.FanInGet()
		if err != nil {
			b.Error(err)
		}
		for _, ret := range fails {
			if ret.Err != nil {
				b.Error(err)
			}
		}
	}
}

func BenchmarkTmLine_BatchStore_WriteSpread_IgnoreMessageLost(b *testing.B) {
	option := initStoreOptionFromEnv(b.Name())
	store, err := NewDefaultStore(*option)
	if err != nil {
		b.Fatal(err)
	}
	defer store.Close()
	defer deleteTable(store.(*DefaultStore), b.Name())

	for i := 0; i < b.N; i++ {
		message := randomMessage(200)
		numOfWrite := 10000
		for i := 0; i < numOfWrite; i++ {
			tmLine, err := NewTmLine(fmt.Sprintf("%d", i), DefaultStreamAdapter, store)
			if err != nil {
				b.Error(err)
			}
			_, err = tmLine.BatchStore(message)
			if err != nil {
				b.Error(err)
			}
		}
	}
}

func initStoreOptionFromEnv(table string) *StoreOption {
	return &StoreOption{
		Endpoint:  os.Getenv("OTS_TEST_ENDPOINT"),
		Instance:  os.Getenv("OTS_TEST_INSTANCENAME"),
		TableName: table,
		AkId:      os.Getenv("OTS_TEST_KEYID"),
		AkSecret:  os.Getenv("OTS_TEST_SECRET"),
		WriterConfig: &writer.Config{
			Concurrent:    300,
			FlushInterval: 20 * time.Millisecond,
			RetryTimeout:  5 * time.Second,
		},
	}
}

func deleteTable(store *DefaultStore, name string) error {
	_, err := store.api.DeleteTable(&tablestore.DeleteTableRequest{TableName: name})
	return err
}

func randomMessage(messageLenght int) Message {
	return &StreamMessage{
		Id:        randomString(8),
		Content:   randomString(messageLenght),
		Timestamp: time.Now().UnixNano(),
	}
}

const letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

func randomString(length int) string {
	b := make([]byte, length)
	for i := range b {
		b[i] = letterBytes[rand.Int63()%int64(len(letterBytes))]
	}
	return string(b)
}
