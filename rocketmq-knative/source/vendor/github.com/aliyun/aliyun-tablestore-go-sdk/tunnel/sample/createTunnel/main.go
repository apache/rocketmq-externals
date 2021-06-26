package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel"
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

	//create base and stream tunnel
	tunnelName := "exampleTunnel"
	req := &tunnel.CreateTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
		Type:       tunnel.TunnelTypeBaseStream,
	}
	resp, err := tunnelClient.CreateTunnel(req)
	if err != nil {
		log.Fatal("create test tunnel failed", err)
	}
	log.Println("tunnel id is", resp.TunnelId)

	//start consume tunnel
	workConfig := &tunnel.TunnelWorkerConfig{
		ProcessorFactory: &tunnel.SimpleProcessFactory{
			CustomValue: "user defined interface{} value",
			ProcessFunc: exampleConsumeFunction,
			ShutdownFunc: func(ctx *tunnel.ChannelContext) {
				fmt.Println("shutdown hook")
			},
		},
	}

	daemon := tunnel.NewTunnelDaemon(tunnelClient, resp.TunnelId, workConfig)
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

func exampleConsumeFunction(ctx *tunnel.ChannelContext, records []*tunnel.Record) error {
	fmt.Println("user-defined information", ctx.CustomValue)
	for _, rec := range records {
		fmt.Println("tunnel record detail:", rec.String())
	}
	fmt.Println("a round of records consumption finished")
	return nil
}
