package main

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel"
	"log"
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

	//open existing tunnel for scale or failover
	tunnelName := "exampleBaseStreamTunnel"
	req := &tunnel.DescribeTunnelRequest{
		TableName:  testConfig.TableName,
		TunnelName: tunnelName,
	}

	tunnelId, channels, err := describeTunnel(req, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//suspend all open channel
	err = SuspendAllOpenChannel(tunnelId, channels, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//check tunnel channels again, some in CLOSING state, some in WAIT state
	_, channels, err = describeTunnel(req, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//open all wait channel
	err = ScheduleAllWaitingChannel(tunnelId, channels, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//check tunnel channels again, some in CLOSING state, some in OPEN state
	_, channels, err = describeTunnel(req, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//resume all suspended channels
	err = ResumeAllSuspendedChannel(tunnelId, channels, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}

	//check tunnel channels finally, all channels are in OPEN state
	_, channels, err = describeTunnel(req, tunnelClient)
	if err != nil {
		log.Fatal(err)
	}
}

func describeTunnel(req *tunnel.DescribeTunnelRequest, client tunnel.TunnelMetaApi) (string, []*tunnel.ChannelInfo, error) {
	resp, err := client.DescribeTunnel(req)
	if err != nil {
		return "", nil, err
	}
	for _, ch := range resp.Channels {
		fmt.Println("ChannelDetail:", ch)
	}
	return resp.Tunnel.TunnelId, resp.Channels, nil
}

func SuspendAllOpenChannel(tunnelId string, channels []*tunnel.ChannelInfo, client tunnel.TunnelMetaApi) error {
	channelsToSuspend := make([]*tunnel.ScheduleChannel, 0)
	for _, ch := range channels {
		if ch.ChannelStatus == "OPEN" {
			channelsToSuspend = append(channelsToSuspend, tunnel.SuspendChannel(ch.ChannelId))
		}
	}
	_, err := client.Schedule(&tunnel.ScheduleRequest{
		TunnelId: tunnelId,
		Channels: channelsToSuspend,
	})
	return err
}

func ScheduleAllWaitingChannel(tunnelId string, channels []*tunnel.ChannelInfo, client tunnel.TunnelMetaApi) error {
	channelsToOpen := make([]*tunnel.ScheduleChannel, 0)
	for _, ch := range channels {
		if ch.ChannelStatus == "WAIT" {
			channelsToOpen = append(channelsToOpen, tunnel.OpenChannel(ch.ChannelId))
		}
	}
	_, err := client.Schedule(&tunnel.ScheduleRequest{
		TunnelId: tunnelId,
		Channels: channelsToOpen,
	})
	return err
}

func ResumeAllSuspendedChannel(tunnelId string, channels []*tunnel.ChannelInfo, client tunnel.TunnelMetaApi) error {
	channelsToResume := make([]*tunnel.ScheduleChannel, 0)
	for _, ch := range channels {
		if ch.ChannelStatus == "CLOSING" {
			channelsToResume = append(channelsToResume, tunnel.ResumeChannel(ch.ChannelId))
		}
	}
	_, err := client.Schedule(&tunnel.ScheduleRequest{
		TunnelId: tunnelId,
		Channels: channelsToResume,
	})
	return err
}
