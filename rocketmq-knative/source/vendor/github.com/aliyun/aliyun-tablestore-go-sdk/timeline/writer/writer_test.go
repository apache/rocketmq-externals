package writer

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
	"math/rand"
	"os"
	"strings"
	"testing"
	"time"
)

var testTableStoreApi = initClientFromEnv()

const (
	firstPk = "PK1"
	sendPk  = "PK2"
	attrCol = "Col"
)

func BenchmarkBatchWriter_BatchAdd_Concurrent(b *testing.B) {
	testTable := b.Name()
	err := prepareAutoIncTable(testTable)
	if err != nil {
		if !strings.Contains(err.Error(), "OTSObjectAlreadyExist") {
			b.Fatal(err)
		}
	}
	defer deleteTable(testTable)

	writer := NewBatchWriter(testTableStoreApi, &Config{Concurrent: 200, FlushInterval: 30 * time.Millisecond, RetryTimeout: time.Second})
	defer writer.Close()
	b.RunParallel(func(pb *testing.PB) {
		change := randomAutoIncPutChange(testTable, 200)
		for pb.Next() {
			f := promise.NewFuture()
			ctx := NewBatchAdd("id", change, f)
			err := writer.BatchAdd(ctx)
			if err != nil {
				b.Error(err)
			}
			_, err = f.Get()
			if err != nil {
				b.Error(err)
			}
		}
	})
}

func BenchmarkBatchWriter_BatchAdd_WriteSpread(b *testing.B) {
	testTable := b.Name()
	err := prepareAutoIncTable(testTable)
	if err != nil {
		if !strings.Contains(err.Error(), "OTSObjectAlreadyExist") {
			b.Fatal(err)
		}
	}
	defer deleteTable(testTable)

	writer := NewBatchWriter(testTableStoreApi, &Config{Concurrent: 200, FlushInterval: 30 * time.Millisecond, RetryTimeout: time.Second})
	defer writer.Close()

	for i := 0; i < b.N; i++ {
		change := randomAutoIncPutChange(testTable, 200)
		f := promise.NewFuture()
		ctx := NewBatchAdd("id", change, f)
		numOfWrite := 10000
		futures := make([]*promise.Future, numOfWrite)
		for i := 0; i < numOfWrite; i++ {
			err := writer.BatchAdd(ctx)
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
				b.Error(ret.Err)
			}
		}
	}
}

func initClientFromEnv() tablestore.TableStoreApi {
	endpoint := os.Getenv("OTS_TEST_ENDPOINT")
	instanceName := os.Getenv("OTS_TEST_INSTANCENAME")
	accessKeyId := os.Getenv("OTS_TEST_KEYID")
	accessKeySecret := os.Getenv("OTS_TEST_SECRET")
	return tablestore.NewClient(endpoint, instanceName, accessKeyId, accessKeySecret)
}

func deleteTable(name string) error {
	_, err := testTableStoreApi.DeleteTable(&tablestore.DeleteTableRequest{TableName: name})
	return err
}

func prepareAutoIncTable(name string) error {
	meta := &tablestore.TableMeta{
		TableName: name,
	}
	meta.AddPrimaryKeyColumn(firstPk, tablestore.PrimaryKeyType_STRING)
	meta.AddPrimaryKeyColumnOption(sendPk, tablestore.PrimaryKeyType_INTEGER, tablestore.AUTO_INCREMENT)
	option := &tablestore.TableOption{
		TimeToAlive: -1,
		MaxVersion:  1,
	}
	req := &tablestore.CreateTableRequest{
		TableMeta:          meta,
		TableOption:        option,
		ReservedThroughput: new(tablestore.ReservedThroughput),
	}
	_, err := testTableStoreApi.CreateTable(req)
	if err != nil {
		return err
	}
	time.Sleep(2 * time.Second)
	return nil
}

func randomAutoIncPutChange(table string, msgLength int) *tablestore.PutRowChange {
	pk := new(tablestore.PrimaryKey)
	pk.AddPrimaryKeyColumn(firstPk, randomString(8))
	pk.AddPrimaryKeyColumnWithAutoIncrement(sendPk)
	change := &tablestore.PutRowChange{
		TableName:  table,
		PrimaryKey: pk,
		Condition:  &tablestore.RowCondition{RowExistenceExpectation: tablestore.RowExistenceExpectation_IGNORE},
	}
	change.AddColumn(attrCol, randomString(msgLength))
	return change
}

const letterBytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

func randomString(length int) string {
	b := make([]byte, length)
	for i := range b {
		b[i] = letterBytes[rand.Int63()%int64(len(letterBytes))]
	}
	return string(b)
}
