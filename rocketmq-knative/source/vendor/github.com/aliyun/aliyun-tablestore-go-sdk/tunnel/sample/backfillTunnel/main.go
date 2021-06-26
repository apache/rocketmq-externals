package main

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
)

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

	//create a stream backfill tunnel, from 2019.07.10, 15:00 to 2019.07.10, 16:00
	tunnelName := "exampleTunnel"
	from := time.Date(2019, time.July, 10, 15, 0, 0, 0, time.UTC)
	to := time.Date(2019, time.July, 10, 16, 0, 0, 0, time.UTC)
	req := &tunnel.CreateTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
		Type:       tunnel.TunnelTypeStream,
		StreamTunnelConfig: &tunnel.StreamTunnelConfig{
			StartOffset: uint64(from.UnixNano()),
			EndOffset:   uint64(to.UnixNano()),
		},
	}
	resp, err := tunnelClient.CreateTunnel(req)
	if err != nil {
		log.Fatal("create test tunnel failed", err)
	}
	log.Println("tunnel id is", resp.TunnelId)

	//create a stream backfill tunnel, from now to 2019.07.10, 16:00
	to = time.Date(2019, time.July, 10, 16, 0, 0, 0, time.UTC)
	req = &tunnel.CreateTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
		Type:       tunnel.TunnelTypeStream,
		StreamTunnelConfig: &tunnel.StreamTunnelConfig{
			Flag:      protocol.StartOffsetFlag_LATEST,
			EndOffset: uint64(to.UnixNano()),
		},
	}
	resp, err = tunnelClient.CreateTunnel(req)
	if err != nil {
		log.Fatal("create test tunnel failed", err)
	}
	log.Println("tunnel id is", resp.TunnelId)

	//create a stream backfill tunnel, from earliest log to 2019.07.10, 16:00
	to = time.Date(2019, time.July, 10, 16, 0, 0, 0, time.UTC)
	req = &tunnel.CreateTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
		Type:       tunnel.TunnelTypeStream,
		StreamTunnelConfig: &tunnel.StreamTunnelConfig{
			Flag:      protocol.StartOffsetFlag_EARLIEST,
			EndOffset: uint64(to.UnixNano()),
		},
	}
	resp, err = tunnelClient.CreateTunnel(req)
	if err != nil {
		log.Fatal("create test tunnel failed", err)
	}
	log.Println("tunnel id is", resp.TunnelId)

	{
		stop := make(chan os.Signal, 1)
		signal.Notify(stop, syscall.SIGTERM, syscall.SIGHUP, syscall.SIGINT)
		<-stop
	}
}

func exampleConsumeFunction(ctx *tunnel.ChannelContext, records []*tunnel.Record) error {
	fmt.Println("user-defined information", ctx.CustomValue)
	for _, rec := range records {
		fmt.Println("tunnel record detail:", rec.String())
	}
	fmt.Println("a round of records consumption finished")
	return nil
}
