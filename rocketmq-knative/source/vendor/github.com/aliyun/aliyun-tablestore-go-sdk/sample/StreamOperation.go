package sample

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"strconv"
	"time"
	"github.com/golang/protobuf/proto"
)

func GetStreamRecordWithTimestampSample(client *tablestore.TableStoreClient, tableName string) {
	resp, err := client.ListStream(&tablestore.ListStreamRequest{TableName: &tableName})
	if err!= nil {
		fmt.Println("failed to list Stream:", err)
		return
	}

	fmt.Printf("%#v\n", resp)

	streamId := resp.Streams[0].Id

	resp2, err := client.DescribeStream(&tablestore.DescribeStreamRequest{StreamId: streamId})
	fmt.Printf("DescribeStreamResponse: %#v\n", resp)
	fmt.Printf("StreamShard: %#v\n", resp2.Shards[0])
	shardId := resp2.Shards[0].SelfShard

	time1:= time.Now().UnixNano() / 1000  - 1000 * 1000 * 3600 * 24

	fmt.Println(time1)
	resp3, err := client.GetShardIterator(&tablestore.GetShardIteratorRequest{
		StreamId: streamId,
		ShardId:  shardId,
		Timestamp: proto.Int64(time1),
	})
	if err != nil {
		fmt.Println("hit err:", err)
		return
	}

	iter := resp3.ShardIterator
	if resp3.Token != nil {
		fmt.Println("token is", resp3.Token)
	} else {
		iter = resp3.ShardIterator
		fmt.Println("iterator is", *iter)
	}

	records := make([]*tablestore.StreamRecord, 0)
	for {
		resp, err := client.GetStreamRecord(&tablestore.GetStreamRecordRequest{
			ShardIterator: iter})
		if err != nil {
			fmt.Println(err)
			return
		}
		fmt.Printf("#records: %d\n", len(resp.Records))
		for i, rec := range resp.Records {
			fmt.Printf("record %d: %s\n", i, rec)
		}
		for _, rec := range resp.Records {
			records = append(records, rec)
		}
		nextIter := resp.NextShardIterator
		if nextIter == nil {
			fmt.Printf("next iterator: %#v\n", nextIter)
			break
		} else {
			fmt.Printf("next iterator: %#v\n", *nextIter)
		}
		if *iter == *nextIter {
			break
		}
		iter = nextIter
	}
}

func GetStreamRecordSample(client *tablestore.TableStoreClient, tableName string) {
	createtableRequest := new(tablestore.CreateTableRequest)

	//client.DeleteTable(&tablestore.DeleteTableRequest{TableName: tableName})

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk2", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk3", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk4", tablestore.PrimaryKeyType_INTEGER)
	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 3
	reservedThroughput := new(tablestore.ReservedThroughput)
	reservedThroughput.Readcap = 0
	reservedThroughput.Writecap = 0
	createtableRequest.TableMeta = tableMeta
	createtableRequest.TableOption = tableOption
	createtableRequest.ReservedThroughput = reservedThroughput
	createtableRequest.StreamSpec = &tablestore.StreamSpecification{EnableStream: true, ExpirationTime: 24}

	_, err := client.CreateTable(createtableRequest)
	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("Create table finished")
	}

	time.Sleep(time.Millisecond * 20)

	for j := 0; j < 300; j++ {

		go func() {
			for i := 0; i < 1000; i++ {
				req := tablestore.PutRowRequest{}
				rowChange := tablestore.PutRowChange{}
				rowChange.TableName = tableName
				pk := tablestore.PrimaryKey{}
				pk.AddPrimaryKeyColumn("pk1", "01f3")
				pk.AddPrimaryKeyColumn("pk2", "000001")
				pk.AddPrimaryKeyColumn("pk3", "001")
				val := 1495246210 + i*100
				pk.AddPrimaryKeyColumn("pk4", int64(val))

				rowChange.PrimaryKey = &pk

				val1 := float64(120.1516525097) + float64(0.0000000001)*float64(i)

				rowChange.AddColumn("longitude", strconv.FormatFloat(val1, 'g', 1, 64))
				rowChange.AddColumn("latitude", "30.2583277934")
				rowChange.AddColumn("brand", "BMW")

				rowChange.AddColumn("speed", "25")
				rowChange.AddColumn("wind_speed", "2")
				rowChange.AddColumn("temperature", "20")
				distance := 8000 + i
				rowChange.AddColumn("distance", strconv.Itoa(distance))

				rowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
				req.PutRowChange = &rowChange
				_, err = client.PutRow(&req)

				if err != nil {
					fmt.Print(err)
				}
			}
		}()

	}

	resp, err := client.ListStream(&tablestore.ListStreamRequest{TableName: &tableName})

	fmt.Printf("%#v\n", resp)

	streamId := resp.Streams[0].Id

	resp2, err := client.DescribeStream(&tablestore.DescribeStreamRequest{StreamId: streamId})
	fmt.Printf("DescribeStreamResponse: %#v\n", resp)
	fmt.Printf("StreamShard: %#v\n", resp2.Shards[0])
	shardId := resp2.Shards[0].SelfShard

	resp3, err := client.GetShardIterator(&tablestore.GetShardIteratorRequest{
		StreamId: streamId,
		ShardId:  shardId})

	iter := resp3.ShardIterator

	records := make([]*tablestore.StreamRecord, 0)
	for {
		resp, err := client.GetStreamRecord(&tablestore.GetStreamRecordRequest{
			ShardIterator: iter})
		if err != nil {
			fmt.Println(err)
			return
		}
		fmt.Printf("#records: %d\n", len(resp.Records))
		for i, rec := range resp.Records {
			fmt.Printf("record %d: %s\n", i, rec)
		}
		for _, rec := range resp.Records {
			records = append(records, rec)
		}
		nextIter := resp.NextShardIterator
		if nextIter == nil {
			fmt.Printf("next iterator: %#v\n", nextIter)
			break
		} else {
			fmt.Printf("next iterator: %#v\n", *nextIter)
		}
		if *iter == *nextIter {
			break
		}
		iter = nextIter
	}

}
