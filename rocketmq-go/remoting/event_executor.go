package rocketmq

import (
	"fmt"
	"github.com/golang/glog"
	"net"
	"sync"
)

type Runnable interface {
	Run()
}

type NetEventType int

const (
	Connect NetEventType = iota
	Close
	Idle
	Exception // TODO error?
)

type NetEvent struct {
	eType         NetEventType
	remoteAddress string
	conn          net.Conn
}

func NewEventType(eType NetEventType, remoteAddr string, conn net.Conn) *NetEvent {
	return &NetEvent{eType, remoteAddr, conn}
}

func (event *NetEvent) Type() NetEventType {
	return event.eType
}

func (event *NetEvent) RemoteAddress() string {
	return event.remoteAddress
}

func (event *NetEvent) Conn() net.Conn {
	return event.conn
}

func (event *NetEvent) String() string {
	return fmt.Sprintf("NettyEvent [type=%s, remoteAddr=%s, channel=%s]",
		event.eType, event.remoteAddress, event.conn)
}

type NetEventExecutor struct {
	hasNotified bool
	running     bool
	stopped     chan int
	mu          sync.RWMutex // TODO need init?
	client      *RemotingClient

	eventQueue chan *NetEvent
	maxSize    int
}

func NewNetEventExecutor(client *RemotingClient) *NetEventExecutor {
	return &NetEventExecutor{
		hasNotified: false,
		running:     false,
		stopped:     make(chan int),
		client:      client,
		eventQueue:  make(chan *NetEvent, 100), // TODO confirm size
		maxSize:     10000,
	}
}

func (executor *NetEventExecutor) Start() {
	go executor.run()
}

func (executor *NetEventExecutor) Shutdown() {
	executor.stopped <- 0
}

func (executor *NetEventExecutor) PutEvent(event *NetEvent) {
	if len(executor.eventQueue) <= executor.maxSize {
		executor.eventQueue <- event //append(executor.eventQueue, event)
	} else {
		fmt.Sprintf("event queue size[%s] enough, so drop this event %s", len(executor.eventQueue), event.String())
	}
}

func (executor *NetEventExecutor) ServiceName() string {
	// TODO
	return nil
}

func (executor *NetEventExecutor) run() {
	glog.Infof("%s service started", executor.ServiceName())

	executor.mu.Lock()
	executor.running = true
	executor.mu.Unlock()

	listener := executor.client.ConnEventListener()
	for executor.running { // TODO optimize
		select {
		case event := <-executor.eventQueue:
			if event != nil && listener != nil {
				switch event.Type() {
				case Connect:
					listener.OnConnConnect(event.remoteAddress, event.Conn())
				case Close:
					listener.OnConnClose(event.remoteAddress, event.Conn())
				case Idle:
					listener.OnConnIdle(event.remoteAddress, event.Conn())
				case Exception:
					listener.OnConnException(event.remoteAddress, event.Conn())
				default:
					break
				}
			}
		case <-executor.stopped:
			executor.mu.Lock()
			executor.running = false
			executor.mu.Unlock()
			break
		}
	}

	glog.Infof("%s service exit.", executor.ServiceName())
}
