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
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"strings"
	"sync"
	"testing"

	. "github.com/smartystreets/goconvey/convey"
	"github.com/stretchr/testify/assert"
)

// TestSelector test roundrobin selector in namesrv
func TestSelector(t *testing.T) {
	srvs := []string{"127.0.0.1:9876", "127.0.0.1:9879", "12.24.123.243:10911", "12.24.123.243:10915"}
	namesrv, err := NewNamesrv(srvs)
	assert.Nil(t, err)

	assert.Equal(t, srvs[0], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[1], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[2], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[3], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[0], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[1], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[2], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[3], namesrv.getNameServerAddress())
	assert.Equal(t, srvs[0], namesrv.getNameServerAddress())
}

func TestGetNamesrv(t *testing.T) {
	Convey("Test GetNamesrv round-robin strategy", t, func() {
		ns := &namesrvs{
			srvs: []string{"192.168.100.1",
				"192.168.100.2",
				"192.168.100.3",
				"192.168.100.4",
				"192.168.100.5",
			},
			lock: new(sync.Mutex),
		}

		index1 := ns.index
		IP1 := ns.getNameServerAddress()

		index2 := ns.index
		IP2 := ns.getNameServerAddress()

		So(index1+1, ShouldEqual, index2)
		So(IP1, ShouldEqual, ns.srvs[index1])
		So(IP2, ShouldEqual, ns.srvs[index2])
	})
}

func TestUpdateNameServerAddress(t *testing.T) {
	Convey("Test UpdateNameServerAddress method", t, func() {
		srvs := []string{
			"192.168.100.1",
			"192.168.100.2",
			"192.168.100.3",
			"192.168.100.4",
			"192.168.100.5",
		}
		http.HandleFunc("/nameserver/addrs", func(w http.ResponseWriter, r *http.Request) {
			fmt.Fprintf(w, strings.Join(srvs, ";"))
		})
		server := &http.Server{Addr: ":0", Handler: nil}
		listener, _ := net.Listen("tcp", ":0")
		go server.Serve(listener)

		port := listener.Addr().(*net.TCPAddr).Port
		nameServerDommain := fmt.Sprintf("http://127.0.0.1:%d/nameserver/addrs", port)
		fmt.Println("temporary name server domain: ", nameServerDommain)

		ns := &namesrvs{
			srvs: []string{},
			lock: new(sync.Mutex),
		}
		ns.UpdateNameServerAddress(nameServerDommain, "DEFAULT")

		index1 := ns.index
		IP1 := ns.getNameServerAddress()

		index2 := ns.index
		IP2 := ns.getNameServerAddress()

		So(index1+1, ShouldEqual, index2)
		So(IP1, ShouldEqual, srvs[index1])
		So(IP2, ShouldEqual, srvs[index2])
	})
}

func TestUpdateNameServerAddressSaveLocalSnapshot(t *testing.T) {
	Convey("Test UpdateNameServerAddress Save Local Snapshot", t, func() {
		srvs := []string{
			"192.168.100.1",
			"192.168.100.2",
			"192.168.100.3",
			"192.168.100.4",
			"192.168.100.5",
		}
		http.HandleFunc("/nameserver/addrs2", func(w http.ResponseWriter, r *http.Request) {
			fmt.Fprintf(w, strings.Join(srvs, ";"))
		})
		server := &http.Server{Addr: ":0", Handler: nil}
		listener, _ := net.Listen("tcp", ":0")
		go server.Serve(listener)

		port := listener.Addr().(*net.TCPAddr).Port
		nameServerDommain := fmt.Sprintf("http://127.0.0.1:%d/nameserver/addrs2", port)
		fmt.Println("temporary name server domain: ", nameServerDommain)

		ns := &namesrvs{
			srvs: []string{},
			lock: new(sync.Mutex),
		}
		ns.UpdateNameServerAddress(nameServerDommain, "DEFAULT")
		// check snapshot saved
		filePath := getSnapshotFilePath("DEFAULT")
		body := strings.Join(srvs, ";")
		bs, _ := ioutil.ReadFile(filePath)
		So(string(bs), ShouldEqual, body)
	})
}

func TestUpdateNameServerAddressUseEnv(t *testing.T) {
	Convey("Test UpdateNameServerAddress Use Env", t, func() {
		srvs := []string{
			"192.168.100.1",
			"192.168.100.2",
			"192.168.100.3",
			"192.168.100.4",
			"192.168.100.5",
		}

		ns := &namesrvs{
			srvs: []string{},
			lock: new(sync.Mutex),
		}
		os.Setenv("NAMESRV_ADDR", strings.Join(srvs, ";"))
		ns.UpdateNameServerAddress("", "DEFAULT")

		index1 := ns.index
		IP1 := ns.getNameServerAddress()

		index2 := ns.index
		IP2 := ns.getNameServerAddress()

		So(index1+1, ShouldEqual, index2)
		So(IP1, ShouldEqual, srvs[index1])
		So(IP2, ShouldEqual, srvs[index2])
	})
}

func TestUpdateNameServerAddressUseSnapshotFile(t *testing.T) {
	Convey("Test UpdateNameServerAddress Use Local Snapshot", t, func() {
		srvs := []string{
			"192.168.100.1",
			"192.168.100.2",
			"192.168.100.3",
			"192.168.100.4",
			"192.168.100.5",
		}

		ns := &namesrvs{
			srvs: []string{},
			lock: new(sync.Mutex),
		}

		os.Setenv("NAMESRV_ADDR", "") // clear env
		// setup local snapshot file
		filePath := getSnapshotFilePath("DEFAULT")
		body := strings.Join(srvs, ";")
		_ = ioutil.WriteFile(filePath, []byte(body), 0644)

		ns.UpdateNameServerAddress("http://127.0.0.1:80/error/nsaddrs", "DEFAULT")

		index1 := ns.index
		IP1 := ns.getNameServerAddress()

		index2 := ns.index
		IP2 := ns.getNameServerAddress()

		So(index1+1, ShouldEqual, index2)
		So(IP1, ShouldEqual, srvs[index1])
		So(IP2, ShouldEqual, srvs[index2])
	})
}

func TestUpdateNameServerAddressLoadSnapshotFileOnce(t *testing.T) {
	Convey("Test UpdateNameServerAddress Load Local Snapshot Once", t, func() {
		srvs := []string{
			"192.168.100.1",
			"192.168.100.2",
			"192.168.100.3",
			"192.168.100.4",
			"192.168.100.5",
		}

		ns := &namesrvs{
			srvs: []string{},
			lock: new(sync.Mutex),
		}

		os.Setenv("NAMESRV_ADDR", "") // clear env
		// setup local snapshot file
		filePath := getSnapshotFilePath("DEFAULT")
		body := strings.Join(srvs, ";")
		_ = ioutil.WriteFile(filePath, []byte(body), 0644)
		// load local snapshot file first time
		ns.UpdateNameServerAddress("http://127.0.0.1:80/error/nsaddrs", "DEFAULT")

		// change the local snapshot file to check load once
		_ = ioutil.WriteFile(filePath, []byte("127.0.0.1;127.0.0.2"), 0644)
		ns.UpdateNameServerAddress("http://127.0.0.1:80/error/nsaddrs", "DEFAULT")

		index1 := ns.index
		IP1 := ns.getNameServerAddress()

		index2 := ns.index
		IP2 := ns.getNameServerAddress()

		So(index1+1, ShouldEqual, index2)
		So(IP1, ShouldEqual, srvs[index1])
		So(IP2, ShouldEqual, srvs[index2])
	})
}
