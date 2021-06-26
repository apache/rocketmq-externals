package exactlyOnce

import (
	"encoding/json"
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel"
	"log"
	"os"
	"os/signal"
	"syscall"
)

type userCheckpointer interface {
	//指定主键和列名，返回该行之前的channelId, sequenceInfo, 属性列map
	getCheckpoint(id string, colNameToGet []string) (channelId string, sequenceInfo *tunnel.SequenceInfo,
		valueMap map[string]interface{}, err error)
	//更新指定列的channelId，sequenceInfo，condition是乐观锁的条件期望，valueMap是属性列map
	updateCheckpoint(id, channelId string, sequenceInfo *tunnel.SequenceInfo,
		condition *tablestore.RowCondition, valueMap map[string]interface{}) error
}

type Config struct {
	Endpoint  string
	Instance  string
	TableName string
	AkId      string
	AkSecret  string
}

var testConfig = Config{
	Endpoint:  "<Your instance endpoint>",
	Instance:  "<Your instance name>",
	TableName: "<Your table name>",
	AkId:      "<Your ak id>",
	AkSecret:  "<Your ak secret>",
}

func main() {
	tunnelClient := tunnel.NewTunnelClient(testConfig.Endpoint, testConfig.Instance,
		testConfig.AkId, testConfig.AkSecret)

	//open existing tunnel for scale or failover
	tunnelName := "exampleStreamTunnel"
	req := &tunnel.DescribeTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
	}
	resp, err := tunnelClient.DescribeTunnel(req)
	if err != nil {
		log.Fatal("create test tunnel failed", err)
	}
	log.Println("tunnel id is", resp.Tunnel.TunnelId)

	//基于ots的userCheckpointer接口
	var checkpointer userCheckpointer //todo implementation

	//start consume tunnel
	workConfig := &tunnel.TunnelWorkerConfig{
		ProcessorFactory: &tunnel.SimpleProcessFactory{
			CustomValue: checkpointer,
			ProcessFunc: exactlyOnceIngestionFinalState,
			ShutdownFunc: func(ctx *tunnel.ChannelContext) {
				fmt.Println("shutdown hook")
			},
		},
	}

	daemon := tunnel.NewTunnelDaemon(tunnelClient, resp.Tunnel.TunnelId, workConfig)
	go func() {
		err = daemon.Run()
		if err != nil {
			log.Fatal("tunnel worker fatal error: ", err)
		}
	}()

	{
		stop := make(chan os.Signal, 1)
		signal.Notify(stop, syscall.SIGTERM, syscall.SIGHUP, syscall.SIGINT)
		<-stop
		daemon.Close()
	}
}

func exactlyOnceIngestionFinalState(ctx *tunnel.ChannelContext, records []*tunnel.Record) error {
	checkpointer := ctx.CustomValue.(userCheckpointer)

	for _, rec := range records {
		if rec.SequenceInfo == nil { //增量数据才有SequenceInfo
			//unexpected base data record
			continue
		}
		id := rec.PrimaryKey.PrimaryKeys[0].Value.(string)
		_, instateSeq, valueMap, err := checkpointer.getCheckpoint(id, nil)
		if err != nil {
			return err
		}
		duplicated, condition := checkRecordFinalState(rec.SequenceInfo, instateSeq)
		if duplicated {
			continue //skip
		}
		//todo do something with valueMap
		fmt.Println("map size", len(valueMap))
		err = checkpointer.updateCheckpoint(id, "", rec.SequenceInfo, condition, valueMap)
		if err != nil {
			return err
		}
	}
	fmt.Println("a round of records consumption finished")
	return nil
}

func checkRecordFinalState(incomingSeq, instateSeq *tunnel.SequenceInfo) (duplicated bool, condition *tablestore.RowCondition) {
	condition = new(tablestore.RowCondition)
	if instateSeq == nil { //数据行不存在
		condition.RowExistenceExpectation = tablestore.RowExistenceExpectation_EXPECT_NOT_EXIST
		return
	}
	if !tunnel.StreamRecordSequenceLess(instateSeq, incomingSeq) {
		duplicated = true
		return
	}
	condition.RowExistenceExpectation = tablestore.RowExistenceExpectation_EXPECT_EXIST
	seqBuf, _ := json.Marshal(instateSeq)
	condition.ColumnCondition = tablestore.NewSingleColumnCondition("SequenceInfo", tablestore.CT_EQUAL, seqBuf)
	return
}
