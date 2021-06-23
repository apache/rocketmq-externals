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

package internal

import (
	"errors"
	"fmt"
	"github.com/apache/rocketmq-client-go/internal/remote"
	"github.com/apache/rocketmq-client-go/primitive"
	"github.com/apache/rocketmq-client-go/rlog"
	"io/ioutil"
	"net/http"
	"os"
	"os/user"
	"path"
	"regexp"
	"strings"
	"sync"
	"time"
)

const (
	DEFAULT_NAMESRV_ADDR = "http://jmenv.tbsite.net:8080/rocketmq/nsaddr"
)

var (
	ipRegex, _ = regexp.Compile(`^((25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))`)

	ErrNoNameserver = errors.New("nameServerAddrs can't be empty.")
	ErrMultiIP      = errors.New("multiple IP addr does not support")
	ErrIllegalIP    = errors.New("IP addr error")
)

//go:generate mockgen -source namesrv.go -destination mock_namesrv.go -self_package github.com/apache/rocketmq-client-go/internal  --package internal Namesrvs
type Namesrvs interface {
	UpdateNameServerAddress(nameServerDomain, instanceName string)

	AddBroker(routeData *TopicRouteData)

	cleanOfflineBroker()

	UpdateTopicRouteInfo(topic string) (routeData *TopicRouteData, changed bool)

	FetchPublishMessageQueues(topic string) ([]*primitive.MessageQueue, error)

	FindBrokerAddrByTopic(topic string) string

	FindBrokerAddrByName(brokerName string) string

	FindBrokerAddressInSubscribe(brokerName string, brokerId int64, onlyThisBroker bool) *FindBrokerResult

	FetchSubscribeMessageQueues(topic string) ([]*primitive.MessageQueue, error)

	AddrList() []string
}

// namesrvs rocketmq namesrv instance.
type namesrvs struct {
	// namesrv addr list
	srvs []string

	// lock for getNameServerAddress in case of update index race condition
	lock sync.Locker

	// index indicate the next position for getNameServerAddress
	index int

	// brokerName -> *BrokerData
	brokerAddressesMap sync.Map

	// brokerName -> map[string]int32: brokerAddr -> version
	brokerVersionMap map[string]map[string]int32
	// lock for broker version read/write
	brokerLock *sync.RWMutex

	//subscribeInfoMap sync.Map
	routeDataMap sync.Map

	lockNamesrv sync.Mutex

	nameSrvClient remote.RemotingClient
}

var _ Namesrvs = &namesrvs{}

// NewNamesrv init Namesrv from namesrv addr string.
func NewNamesrv(addr primitive.NamesrvAddr) (*namesrvs, error) {
	if err := addr.Check(); err != nil {
		return nil, err
	}
	nameSrvClient := remote.NewRemotingClient()
	return &namesrvs{
		srvs:             addr,
		lock:             new(sync.Mutex),
		nameSrvClient:    nameSrvClient,
		brokerVersionMap: make(map[string]map[string]int32, 0),
		brokerLock:       new(sync.RWMutex),
	}, nil
}

// getNameServerAddress return namesrv using round-robin strategy.
func (s *namesrvs) getNameServerAddress() string {
	s.lock.Lock()
	defer s.lock.Unlock()

	addr := s.srvs[s.index]
	index := s.index + 1
	if index < 0 {
		index = -index
	}
	index %= len(s.srvs)
	s.index = index
	return strings.TrimLeft(addr, "http(s)://")
}

func (s *namesrvs) Size() int {
	return len(s.srvs)
}

func (s *namesrvs) String() string {
	return strings.Join(s.srvs, ";")
}
func (s *namesrvs) SetCredentials(credentials primitive.Credentials) {
	s.nameSrvClient.RegisterInterceptor(remote.ACLInterceptor(credentials))
}

func (s *namesrvs) AddrList() []string {
	return s.srvs
}

func getSnapshotFilePath(instanceName string) string {
	homeDir := ""
	if usr, err := user.Current(); err == nil {
		homeDir = usr.HomeDir
	} else {
		rlog.Error("name server domain, can't get user home directory", map[string]interface{}{
			"err": err,
		})
	}
	storePath := path.Join(homeDir, "/logs/rocketmq-go/snapshot")
	if _, err := os.Stat(storePath); os.IsNotExist(err) {
		if err = os.MkdirAll(storePath, 0755); err != nil {
			rlog.Fatal("can't create name server snapshot directory", map[string]interface{}{
				"path": storePath,
				"err":  err,
			})
		}
	}
	filePath := path.Join(storePath, fmt.Sprintf("nameserver_addr-%s", instanceName))
	return filePath
}

// UpdateNameServerAddress will update srvs.
// docs: https://rocketmq.apache.org/docs/best-practice-namesvr/
func (s *namesrvs) UpdateNameServerAddress(nameServerDomain, instanceName string) {
	s.lock.Lock()
	defer s.lock.Unlock()

	if nameServerDomain == "" {
		// try to get from environment variable
		if v := os.Getenv("NAMESRV_ADDR"); v != "" {
			s.srvs = strings.Split(v, ";")
			return
		}
		// use default domain
		nameServerDomain = DEFAULT_NAMESRV_ADDR
	}

	client := http.Client{Timeout: 10 * time.Second}
	resp, err := client.Get(nameServerDomain)
	if err == nil {
		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		if err == nil {
			oldBodyStr := strings.Join(s.srvs, ";")
			bodyStr := string(body)
			if bodyStr != "" && oldBodyStr != bodyStr {
				s.srvs = strings.Split(string(body), ";")

				rlog.Info("name server address changed", map[string]interface{}{
					"old": oldBodyStr,
					"new": bodyStr,
				})
				// save to local snapshot
				filePath := getSnapshotFilePath(instanceName)
				if err := ioutil.WriteFile(filePath, body, 0644); err == nil {
					rlog.Info("name server snapshot save successfully", map[string]interface{}{
						"filePath": filePath,
					})
				} else {
					rlog.Error("name server snapshot save failed", map[string]interface{}{
						"filePath": filePath,
						"err":      err,
					})
				}
			}
			rlog.Info("name server http fetch successfully", map[string]interface{}{
				"addrs": bodyStr,
			})
			return
		} else {
			rlog.Error("name server http fetch failed", map[string]interface{}{
				"NameServerDomain": nameServerDomain,
				"err":              err,
			})
		}
	}

	// load local snapshot if need when name server domain request failed
	if len(s.srvs) == 0 {
		filePath := getSnapshotFilePath(instanceName)
		if _, err := os.Stat(filePath); !os.IsNotExist(err) {
			if bs, err := ioutil.ReadFile(filePath); err == nil {
				rlog.Info("load the name server snapshot local file", map[string]interface{}{
					"filePath": filePath,
				})
				s.srvs = strings.Split(string(bs), ";")
				return
			}
		} else {
			rlog.Warning("name server snapshot local file not exists", map[string]interface{}{
				"filePath": filePath,
			})
		}
	}
}
