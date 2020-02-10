/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package rocketmq

import (
	"context"

	"fmt"

	"os"
	"os/signal"
	"syscall"

	"strings"

	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel"
	"github.com/apache/rocketmq-externals/rocketmq-knative/source/pkg/apis/sources/v1alpha1"
	"golang.org/x/sync/errgroup"
)

// TunnelClientCreator creates a TunnelClient.
type TunnelClientCreator func(ctx context.Context, instance string, creds *v1alpha1.Credentials) (TunnelClient, error)

// AliTunnelClientCreator creates a real tablestore tunnel client. It should always be used, except during
// unit tests.
func AliTunnelClientCreator(ctx context.Context, instance string, creds *v1alpha1.Credentials) (TunnelClient, error) {
	tunnelClient := tunnel.NewTunnelClient(creds.Url, instance,
		creds.AccessKeyId, creds.AccessKeySecret)
	return &realAliTunnelClient{Ctx: ctx, client: tunnelClient}, nil
}

// TunnelClient is the set of methods we use on tunnel.Client. See tunnel.Client for documentation
// of the functions.
type TunnelClient interface {
	CreateSubscription(ctx context.Context, tableName, subscriptionName string) error
	DeleteSubscription(ctx context.Context, tableName, subscriptionName string) error
	SubscriptionExists(ctx context.Context, tableName, subscriptionName string) (bool, error)
	SubscriptionReceiveMessage(ctx context.Context, tableName, subscriptionName string, f func(tunnelCtx *tunnel.ChannelContext, records []*tunnel.Record) error) error
}

// realAliTunnelClient is the client that will be used everywhere except unit tests. Verify that it
// satisfies the interface.
var _ TunnelClient = &realAliTunnelClient{}

// realAliTunnelClient wraps a real tunnel client, so that it matches the TunnelClient
// interface. It is needed because the real SubscriptionInProject returns a struct and does not
// implicitly match TunnelClient, which returns an interface.
type realAliTunnelClient struct {
	Ctx    context.Context
	client tunnel.TunnelClient
}

func (c *realAliTunnelClient) SubscriptionReceiveMessage(ctx context.Context, tableName, subscriptionName string, f func(tunnelCtx *tunnel.ChannelContext, records []*tunnel.Record) error) error {
	group, gctx := errgroup.WithContext(ctx)
	group.Go(func() error {
		return c.subscriptionReceiver(gctx, tableName, subscriptionName, f)
	})
	return group.Wait()
}

func (c *realAliTunnelClient) subscriptionReceiver(ctx context.Context, tableName, subscriptionName string, f func(tunnelCtx *tunnel.ChannelContext, records []*tunnel.Record) error) error {
	_, cancel := context.WithCancel(ctx)
	defer cancel()
	//start consume tunnel
	workConfig := &tunnel.TunnelWorkerConfig{
		ProcessorFactory: &tunnel.SimpleProcessFactory{
			CustomValue: "user defined interface{} value",
			ProcessFunc: f,
			ShutdownFunc: func(ctx *tunnel.ChannelContext) {
				fmt.Println("shutdown hook")
			},
		},
	}
	req := &tunnel.DescribeTunnelRequest{
		TableName:  tableName,
		TunnelName: subscriptionName,
	}
	resp, err := c.client.DescribeTunnel(req)
	if err != nil {
		return err
	}
	daemon := tunnel.NewTunnelDaemon(c.client, resp.Tunnel.TunnelId, workConfig)
	go func() {
		err := daemon.Run()
		if err != nil {
			fmt.Printf("tunnel worker fatal error: %s", err.Error())
		}
	}()

	{
		stop := make(chan os.Signal, 1)
		signal.Notify(stop, syscall.SIGTERM, syscall.SIGHUP, syscall.SIGINT)
		<-stop
		daemon.Close()
	}
	return nil
}

func (c *realAliTunnelClient) SubscriptionExists(ctx context.Context, tableName, subscriptionName string) (bool, error) {
	req := &tunnel.DescribeTunnelRequest{
		TableName:  tableName,
		TunnelName: subscriptionName,
	}
	_, err := c.client.DescribeTunnel(req)
	if err != nil {
		if strings.Contains(err.Error(), "not exist") {
			return false, nil
		}
		return false, err
	}

	return true, nil
}

func (c *realAliTunnelClient) CreateSubscription(ctx context.Context, tableName, subscriptionName string) error {
	req := &tunnel.CreateTunnelRequest{
		TableName:  tableName,
		TunnelName: subscriptionName,
		Type:       tunnel.TunnelTypeBaseStream,
	}
	_, err := c.client.CreateTunnel(req)
	if err != nil {
		return err
	}

	return nil
}

func (c *realAliTunnelClient) DeleteSubscription(ctx context.Context, tableName, subscriptionName string) error {
	req := &tunnel.DeleteTunnelRequest{
		TableName:  tableName,
		TunnelName: subscriptionName,
	}
	_, err := c.client.DeleteTunnel(req)
	if err != nil {
		return err
	}

	return nil
}
