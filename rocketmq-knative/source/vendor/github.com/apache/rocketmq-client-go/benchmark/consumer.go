/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package main

import (
	"flag"
	"fmt"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
	"time"
)

type statiBenchmarkConsumerSnapshot struct {
	receiveMessageTotal   int64
	born2ConsumerTotalRT  int64
	store2ConsumerTotalRT int64
	born2ConsumerMaxRT    int64
	store2ConsumerMaxRT   int64
	createdAt             time.Time
	next                  *statiBenchmarkConsumerSnapshot
}

type consumeSnapshots struct {
	sync.RWMutex
	head, tail, cur *statiBenchmarkConsumerSnapshot
	len             int
}

func (s *consumeSnapshots) takeSnapshot() {
	b := s.cur
	sn := new(statiBenchmarkConsumerSnapshot)
	sn.receiveMessageTotal = atomic.LoadInt64(&b.receiveMessageTotal)
	sn.born2ConsumerMaxRT = atomic.LoadInt64(&b.born2ConsumerMaxRT)
	sn.born2ConsumerTotalRT = atomic.LoadInt64(&b.born2ConsumerTotalRT)
	sn.store2ConsumerMaxRT = atomic.LoadInt64(&b.store2ConsumerMaxRT)
	sn.store2ConsumerTotalRT = atomic.LoadInt64(&b.store2ConsumerTotalRT)
	sn.createdAt = time.Now()

	s.Lock()
	if s.tail != nil {
		s.tail.next = sn
	}
	s.tail = sn
	if s.head == nil {
		s.head = s.tail
	}

	s.len++
	if s.len > 10 {
		s.head = s.head.next
		s.len--
	}
	s.Unlock()
}

func (s *consumeSnapshots) printStati() {
	s.RLock()
	if s.len < 10 {
		s.RUnlock()
		return
	}

	f, l := s.head, s.tail
	respSucCount := float64(l.receiveMessageTotal - f.receiveMessageTotal)
	consumeTps := respSucCount / l.createdAt.Sub(f.createdAt).Seconds()
	avgB2CRT := float64(l.born2ConsumerTotalRT-f.born2ConsumerTotalRT) / respSucCount
	avgS2CRT := float64(l.store2ConsumerTotalRT-f.store2ConsumerTotalRT) / respSucCount
	s.RUnlock()

	fmt.Printf(
		"Consume TPS: %d Average(B2C) RT: %7.3f Average(S2C) RT: %7.3f MAX(B2C) RT: %d MAX(S2C) RT: %d\n",
		int64(consumeTps), avgB2CRT, avgS2CRT, l.born2ConsumerMaxRT, l.store2ConsumerMaxRT,
	)
}

type consumer struct {
	topic          string
	groupPrefix    string
	nameSrv        string
	isPrefixEnable bool
	filterType     string
	expression     string
	testMinutes    int
	instanceCount  int

	flags *flag.FlagSet

	groupID string
}

func init() {
	c := &consumer{}
	flags := flag.NewFlagSet("consumer", flag.ExitOnError)
	c.flags = flags

	flags.StringVar(&c.topic, "t", "BenchmarkTest", "topic")
	flags.StringVar(&c.groupPrefix, "g", "benchmark_consumer", "group prefix")
	flags.StringVar(&c.nameSrv, "n", "", "namesrv address list, separated by comma")
	flags.BoolVar(&c.isPrefixEnable, "p", true, "group prefix is enable")
	flags.StringVar(&c.filterType, "f", "", "filter type,options:TAG|SQL92, or empty")
	flags.StringVar(&c.expression, "e", "*", "expression")
	flags.IntVar(&c.testMinutes, "m", 10, "test minutes")
	flags.IntVar(&c.instanceCount, "i", 1, "instance count")

	registerCommand("consumer", c)
}

func (c *consumer) consumeMsg(stati *statiBenchmarkConsumerSnapshot, exit chan struct{}) {
	//consumer, err := rocketmq.NewPushConsumer(&rocketmq.PushConsumerConfig{
	//	ClientConfig: rocketmq.ClientConfig{
	//		GroupID:    c.groupID,
	//		NameServer: c.nameSrv,
	//	},
	//	ThreadCount:         c.instanceCount,
	//	MessageBatchMaxSize: 16,
	//})
	//if err != nil {
	//	panic("new push consumer error:" + err.Error())
	//}
	//
	//consumer.Subscribe(c.topic, c.expression, func(m *rocketmq.MessageExt) rocketmq.ConsumeStatus {
	//	atomic.AddInt64(&stati.receiveMessageTotal, 1)
	//	now := time.Now().UnixNano() / int64(time.Millisecond)
	//	b2cRT := now - m.BornTimestamp
	//	atomic.AddInt64(&stati.born2ConsumerTotalRT, b2cRT)
	//	s2cRT := now - m.StoreTimestamp
	//	atomic.AddInt64(&stati.store2ConsumerTotalRT, s2cRT)
	//
	//	for {
	//		old := atomic.LoadInt64(&stati.born2ConsumerMaxRT)
	//		if old >= b2cRT || atomic.CompareAndSwapInt64(&stati.born2ConsumerMaxRT, old, b2cRT) {
	//			break
	//		}
	//	}
	//
	//	for {
	//		old := atomic.LoadInt64(&stati.store2ConsumerMaxRT)
	//		if old >= s2cRT || atomic.CompareAndSwapInt64(&stati.store2ConsumerMaxRT, old, s2cRT) {
	//			break
	//		}
	//	}
	//
	//	return rocketmq.ConsumeSuccess
	//})
	//println("Start")
	//consumer.Start()
	//select {
	//case <-exit:
	//	consumer.Shutdown()
	//	return
	//}
}

func (c *consumer) run(args []string) {
	c.flags.Parse(args)
	if c.topic == "" {
		println("empty topic")
		c.usage()
		return
	}

	if c.groupPrefix == "" {
		println("empty group prefix")
		c.usage()
		return
	}

	if c.nameSrv == "" {
		println("empty name server")
		c.usage()
		return
	}

	if c.testMinutes <= 0 {
		println("test time must be positive integer")
		c.usage()
		return
	}

	if c.instanceCount <= 0 {
		println("thread count must be positive integer")
		c.usage()
		return
	}

	c.groupID = c.groupPrefix
	if c.isPrefixEnable {
		c.groupID += fmt.Sprintf("_%d", time.Now().UnixNano()/int64(time.Millisecond)%100)
	}

	stati := statiBenchmarkConsumerSnapshot{}
	snapshots := consumeSnapshots{cur: &stati}
	exitChan := make(chan struct{})

	wg := sync.WaitGroup{}

	wg.Add(1)
	go func() {
		c.consumeMsg(&stati, exitChan)
		wg.Done()
	}()

	// snapshot
	wg.Add(1)
	go func() {
		defer wg.Done()
		ticker := time.NewTicker(time.Second)
		for {
			select {
			case <-ticker.C:
				snapshots.takeSnapshot()
			case <-exitChan:
				ticker.Stop()
				return
			}
		}
	}()

	// print statistic
	wg.Add(1)
	go func() {
		defer wg.Done()
		ticker := time.NewTicker(time.Second * 10)
		for {
			select {
			case <-ticker.C:
				snapshots.printStati()
			case <-exitChan:
				ticker.Stop()
				return
			}
		}
	}()

	signalChan := make(chan os.Signal, 1)
	signal.Notify(signalChan, syscall.SIGINT, syscall.SIGTERM)
	select {
	case <-time.Tick(time.Minute * time.Duration(c.testMinutes)):
	case <-signalChan:
	}

	println("Closed")
	close(exitChan)
	wg.Wait()
	snapshots.takeSnapshot()
	snapshots.printStati()
}

func (c *consumer) usage() {
	c.flags.Usage()
}
