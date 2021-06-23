package tunnel

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/cenkalti/backoff"
	"go.uber.org/zap"
	"sync"
	"sync/atomic"
	"time"
)

var (
	pipeChannelSize = 5

	rpoBar     = 500
	rpoSizeBar = 900 * 1024 //900K bytes
)

type ChannelStatus struct {
	ChannelId string
	Version   int64
	Status    protocol.ChannelStatus
}

func ToChannelStatus(c *protocol.Channel) *ChannelStatus {
	return &ChannelStatus{
		ChannelId: c.GetChannelId(),
		Version:   c.GetVersion(),
		Status:    c.GetStatus(),
	}
}

func (c *ChannelStatus) ToPbChannel() *protocol.Channel {
	clone := c
	return &protocol.Channel{
		ChannelId: &clone.ChannelId,
		Version:   &clone.Version,
		Status:    clone.Status.Enum(),
	}
}

type ChannelConn interface {
	NotifyStatus(channel *ChannelStatus)
	Closed() bool
	Close()
}

// failConn do nothing but turn channel state to close
type failConn struct {
	mu           sync.Mutex
	state        *TunnelStateMachine
	currentState *ChannelStatus
}

func (c *failConn) NotifyStatus(channel *ChannelStatus) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.currentState != nil && c.currentState.Version > channel.Version {
		return
	}
	c.currentState = channel
	switch c.currentState.Status {
	case protocol.ChannelStatus_CLOSE:
	case protocol.ChannelStatus_CLOSING:
		c.currentState.Version += 1
		c.currentState.Status = protocol.ChannelStatus_CLOSE
		c.state.UpdateStatus(c.currentState)
	case protocol.ChannelStatus_OPEN:
		c.currentState.Version += 1
		c.currentState.Status = protocol.ChannelStatus_CLOSE
		c.state.UpdateStatus(c.currentState)
	case protocol.ChannelStatus_TERMINATED:
	default:
	}
}

func (c *failConn) Closed() bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.currentState == nil {
		return false
	}
	return c.currentState.Status == protocol.ChannelStatus_CLOSE ||
		c.currentState.Status == protocol.ChannelStatus_TERMINATED
}

func (c *failConn) Close() {}

type ChannelDialer interface {
	ChannelDial(tunnelId, clientId, channelId, token string, p ChannelProcessor, state *TunnelStateMachine) ChannelConn
}

type channelDialer struct {
	api tunnelDataApi
	lg  *zap.Logger
	bc  *ChannelBackoffConfig
}

func (d *channelDialer) ChannelDial(tunnelId, clientId, channelId, token string, p ChannelProcessor, state *TunnelStateMachine) ChannelConn {
	finish := atomic.Value{}
	finish.Store(false)

	isStream, err := streamToken(token)
	if err != nil {
		isStream = true //treat as stream token with flow control
	}
	conn := &channelConn{
		tunnelId:      tunnelId,
		clientId:      clientId,
		channelId:     channelId,
		token:         token,
		api:           d.api,
		p:             p,
		state:         state,
		lg:            d.lg,
		bc:            d.bc,
		finished:      finish,
		streamChannel: isStream,
	}
	return conn
}

var (
	waitStatus    = int32(0)
	runningStatus = int32(1)
	closingStatus = int32(2)
	closedStatus  = int32(3)
)

type tunnelDataApi interface {
	readRecords(tunnelId, clientId string, channelId string, token string) ([]*Record, string, string, int, error)
}

type channelConn struct {
	mu sync.Mutex

	tunnelId  string
	clientId  string
	channelId string
	token     string

	api tunnelDataApi
	p   ChannelProcessor

	currentState *ChannelStatus
	state        *TunnelStateMachine

	lg *zap.Logger
	bc *ChannelBackoffConfig

	status   int32
	finished atomic.Value

	ticker *backoff.Ticker

	streamChannel bool
}

func (c *channelConn) NotifyStatus(channel *ChannelStatus) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.currentState != nil && c.currentState.Version > channel.Version {
		return
	}
	c.currentState = channel
	switch channel.Status {
	case protocol.ChannelStatus_CLOSE:
		c.lg.Info("closed channel status", zap.String("tunnelId", c.tunnelId),
			zap.String("clientId", c.clientId), zap.String("channelId", c.channelId), zap.Int64("version", channel.Version))
		c.close(false)
	case protocol.ChannelStatus_CLOSING: //draw closing action and check closed/finish status
		if atomic.LoadInt32(&c.status) == waitStatus {
			atomic.StoreInt32(&c.status, closedStatus)
		} else {
			atomic.CompareAndSwapInt32(&c.status, runningStatus, closingStatus)
		}
		c.checkUpdateStatus()
	case protocol.ChannelStatus_OPEN: //draw processing action or check closed/finish status
		if atomic.CompareAndSwapInt32(&c.status, waitStatus, runningStatus) {
			go c.workLoop()
		} else {
			c.checkUpdateStatus()
		}
	case protocol.ChannelStatus_TERMINATED:
		c.lg.Info("terminated channel status", zap.String("tunnelId", c.tunnelId),
			zap.String("clientId", c.clientId), zap.String("channelId", c.channelId), zap.Int64("version", channel.Version))
		c.close(true)
	default:
		c.lg.Error("Unexpected channel status", zap.String("tunnelId", c.tunnelId),
			zap.String("clientId", c.clientId), zap.String("channelId", c.channelId),
			zap.Int32("Status", int32(c.currentState.Status)), zap.Int64("version", channel.Version))
	}
}

func (c *channelConn) Closed() bool {
	return atomic.LoadInt32(&c.status) == closedStatus
}

func (c *channelConn) Close() {
	c.close(false)
}

func (c *channelConn) close(finish bool) {
	c.p.Shutdown()
	if finish {
		c.finished.Store(true)
	}
	atomic.StoreInt32(&c.status, closedStatus)
}

func (c *channelConn) checkUpdateStatus() {
	if atomic.LoadInt32(&c.status) == closedStatus {
		c.currentState.Version += 1
		if c.finished.Load().(bool) {
			c.currentState.Status = protocol.ChannelStatus_TERMINATED
		} else {
			c.currentState.Status = protocol.ChannelStatus_CLOSE
		}
		c.state.UpdateStatus(c.currentState)
	}
}

type pipeResult struct {
	finished  bool
	records   []*Record
	traceId   string
	nextToken string
	error     error
}

func (c *channelConn) workLoop() {
	pipeCh := make(chan *pipeResult, pipeChannelSize)
	closeCh := make(chan struct{})
	defer close(closeCh)
	go c.readRecordsPipe(pipeCh, closeCh)

	for atomic.LoadInt32(&c.status) == runningStatus {
		finish, err := c.processRecords(pipeCh)
		if err != nil {
			c.close(false)
			c.lg.Info("channel shutdown with error", zap.String("cid", c.channelId), zap.Error(err))
			break
		} else {
			if finish {
				c.close(true)
				c.lg.Info("channel finished", zap.String("cid", c.channelId))
				break
			}
		}
	}
	if atomic.LoadInt32(&c.status) == closingStatus {
		c.close(false)
		c.lg.Info("channel shutdown", zap.String("cid", c.channelId))
	}
}

func (c *channelConn) readRecordsPipe(outCh chan *pipeResult, closeCh chan struct{}) {
	var bkoff *backoff.ExponentialBackOff
	if c.streamChannel {
		bkoff = ExponentialBackoff(c.bc.baseDelay, c.bc.MaxDelay, 0, c.bc.factor, c.bc.jitter)
	}

	for {
		ret := new(pipeResult)
		select {
		case <-closeCh:
			return
		default:
		}

		if c.token == FinishTag {
			ret.finished = true
		} else {
			s := time.Now()
			records, nextToken, traceId, size, err := c.api.readRecords(c.tunnelId, c.clientId, c.channelId, c.token)
			if err != nil {
				ret.error = err
			} else {
				ret.records = records
				ret.nextToken = nextToken
				ret.traceId = traceId
				if bkoff != nil {
					if streamFullData(len(records), size) {
						bkoff.Reset()
					}
				}
				c.token = nextToken
			}
			c.lg.Info("Metric info", zap.String("tunnelId", c.tunnelId), zap.String("clientId", c.clientId),
				zap.String("channelId", c.channelId), zap.String("token", nextToken),
				zap.String("GetTunnelRecordLatency", time.Now().Sub(s).String()))
		}

		select {
		case outCh <- ret:
		case <-closeCh:
			return
		}
		if bkoff != nil {
			time.Sleep(bkoff.NextBackOff())
		}
	}
}

func (c *channelConn) processRecords(inCh chan *pipeResult) (bool, error) {
	ret := <-inCh
	if ret.error != nil {
		c.lg.Error("Channel read records failed",
			zap.String("tunnelId", c.tunnelId), zap.String("clientId", c.clientId),
			zap.String("channelId", c.channelId), zap.Error(ret.error))
		return false, ret.error
	}
	if ret.finished {
		return true, nil
	}
	s := time.Now()
	if err := c.p.Process(ret.records, ret.nextToken, ret.traceId); err != nil {
		c.lg.Error("Processor process records failed",
			zap.String("tunnelId", c.tunnelId), zap.String("clientId", c.clientId),
			zap.String("channelId", c.channelId), zap.Error(err))
		return false, err
	}
	c.lg.Info("Metric info", zap.String("tunnelId", c.tunnelId), zap.String("clientId", c.clientId),
		zap.String("channelId", c.channelId), zap.String("token", ret.nextToken),
		zap.String("ClientProcessLatency", time.Now().Sub(s).String()))
	return false, nil
}

func streamFullData(numRec int, size int) bool {
	return numRec > rpoBar || size > rpoSizeBar
}
