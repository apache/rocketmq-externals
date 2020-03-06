package tunnel

import (
	"context"
	"fmt"
	"go.uber.org/zap"
	"sync/atomic"
	"time"
)

var (
	workerReady   = int32(0)
	workerStarted = int32(1)
	workerEnded   = int32(2)

	statusMap = map[int32]string{
		workerReady:   "ready",
		workerStarted: "started",
		workerEnded:   "ended",
	}
)

type TunnelWorker interface {
	ConnectAndWorking() error
	Shutdown()
}

type tunnelWorker struct {
	tunnelId string
	clientId string

	conf     *TunnelWorkerConfig
	channels map[string]ChannelConn

	tunnelApi    *TunnelApi
	stateMachine *TunnelStateMachine

	tunnelCtx context.Context
	cancel    context.CancelFunc

	lg *zap.Logger

	started           int32
	lastHeartbeatTime time.Time
}

func newTunnelWorker(tunnelId string, api *TunnelApi, conf *TunnelWorkerConfig) (TunnelWorker, error) {
	if conf.ProcessorFactory == nil {
		return nil, &TunnelError{Code: ErrCodeClientError, Message: "TunnelWorkerConfig ProcessorFactory can not be nil"}
	}
	ctx, cancel := context.WithCancel(context.Background())
	cloneConf := *conf
	initExceptDialerConfig(&cloneConf)
	lg, err := cloneConf.LogConfig.Build(ReplaceLogCore(cloneConf.LogWriteSyncer, *cloneConf.LogConfig))
	if err != nil {
		return nil, &TunnelError{Code: ErrCodeClientError, Message: err.Error()}
	}
	if cloneConf.ChannelDialer == nil {
		cloneConf.ChannelDialer = &channelDialer{
			api: api,
			lg:  lg,
			bc:  cloneConf.BackoffConfig,
		}
	}
	return &tunnelWorker{
		tunnelId:          tunnelId,
		conf:              &cloneConf,
		channels:          make(map[string]ChannelConn),
		tunnelApi:         api,
		tunnelCtx:         ctx,
		cancel:            cancel,
		lg:                lg,
		started:           workerReady,
		lastHeartbeatTime: time.Time{},
	}, nil
}

func initExceptDialerConfig(conf *TunnelWorkerConfig) {
	if conf.HeartbeatInterval <= 0 {
		conf.HeartbeatInterval = DefaultHeartbeatInterval
	}
	if conf.HeartbeatTimeout <= conf.HeartbeatInterval {
		conf.HeartbeatTimeout = DefaultHeartbeatTimeout
	}
	if conf.LogConfig == nil {
		conf.LogConfig = &DefaultLogConfig
	}
	if conf.LogWriteSyncer == nil {
		conf.LogWriteSyncer = DefaultSyncer
	}
	if conf.BackoffConfig == nil {
		conf.BackoffConfig = &DefaultBackoffConfig
	} else {
		setDefault(conf.BackoffConfig)
	}
}

func (t *tunnelWorker) ConnectAndWorking() error {
	err := t.connect()
	if err != nil {
		return err
	}
	heartbeatTicker := time.NewTicker(t.conf.HeartbeatInterval)
	defer heartbeatTicker.Stop()
	for {
		err = t.heartbeat()
		if err != nil {
			if isFatalError(err) {
				t.lg.Error("heart fatal error", zap.String("tunnelId", t.tunnelId),
					zap.String("clientId", t.clientId), zap.Error(err))
				return err
			}
		}
		select {
		case <-heartbeatTicker.C:
		case <-t.tunnelCtx.Done():
			t.lg.Info("tunnel worker has been shut down")
			err = t.tunnelApi.shutdown(t.tunnelId, t.clientId)
			if err != nil {
				t.lg.Error("tunnel worker call shutdown api failed", zap.String("tunnelId", t.tunnelId),
					zap.String("clientId", t.clientId), zap.Error(err))
				return err
			}
			return &TunnelError{Code: ErrCodeClientError, Message: "tunnel worker has been shut down"}
		}
	}
}

func (t *tunnelWorker) connect() error {
	if !atomic.CompareAndSwapInt32(&t.started, workerReady, workerStarted) {
		return &TunnelError{Code: ErrCodeClientError, Message: fmt.Sprintf("Tunnel worker has already been %s status", statusMap[t.started])}
	}
	id, err := t.tunnelApi.connect(t.tunnelId, getSeconds(t.conf.HeartbeatTimeout))
	if err != nil {
		atomic.StoreInt32(&t.started, workerReady)
		t.lg.Error("connect failed", zap.String("tunnel id", t.tunnelId), zap.Error(err))
		return err
	}
	t.clientId = id
	t.stateMachine = NewTunnelStateMachine(t.tunnelId, t.clientId, t.conf.ChannelDialer, t.conf.ProcessorFactory, t.tunnelApi, t.lg)
	return nil
}

func (t *tunnelWorker) heartbeat() error {
	if !t.lastHeartbeatTime.Equal(time.Time{}) {
		if time.Now().Sub(t.lastHeartbeatTime) > t.conf.HeartbeatTimeout {
			t.lg.Error("tunnel client heartbeat timeout")
			return &TunnelError{Code: ErrCodeResourceGone, Message: "tunnel client heartbeat timeout"}
		}
	}
	curChannels, err := t.stateMachine.BatchGetStatus(NewBatchGetStatusReq())
	if err != nil {
		t.lg.Info("tunnel state machine has been closed")
		return &TunnelError{Code: ErrCodeClientError, Message: err.Error()}
	}
	targetChannels, err := t.tunnelApi.heartbeat(t.tunnelId, t.clientId, curChannels)
	if err != nil {
		t.lg.Error("send heartbeat failed", zap.String("tunnelId", t.tunnelId),
			zap.String("clientId", t.clientId), zap.Error(err))
		return err
	}
	t.lastHeartbeatTime = time.Now()
	t.stateMachine.BatchUpdateStatus(targetChannels)
	return nil
}

func (t *tunnelWorker) Shutdown() {
	t.cancel()
	if t.stateMachine != nil {
		t.stateMachine.Close()
	}
	atomic.StoreInt32(&t.started, workerEnded)
	t.lg.Info("shutdown", zap.String("tunnel id", t.tunnelId))
}

func getSeconds(dur time.Duration) int64 {
	sec := dur / time.Second
	return int64(sec)
}

func isFatalError(err error) bool {
	if te, ok := err.(*TunnelError); ok { //server error
		return !te.Temporary()
	}
	return false
}
