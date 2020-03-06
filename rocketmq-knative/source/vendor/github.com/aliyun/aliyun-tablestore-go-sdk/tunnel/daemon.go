package tunnel

import (
	"math/rand"
	"sync"
	"sync/atomic"
	"time"
)

var DaemonRandomRetryMs = 10000

type TunnelWorkerDaemon struct {
	mu       sync.Mutex
	client   TunnelClient
	tunnelId string
	conf     *TunnelWorkerConfig
	worker   TunnelWorker
	closed   *atomic.Value
	random   *rand.Rand
}

func NewTunnelDaemon(c TunnelClient, id string, conf *TunnelWorkerConfig) *TunnelWorkerDaemon {
	closed := new(atomic.Value)
	closed.Store(false)
	return &TunnelWorkerDaemon{
		client:   c,
		tunnelId: id,
		closed:   closed,
		conf:     conf,
		random:   rand.New(rand.NewSource(time.Now().UnixNano())),
	}
}

func (d *TunnelWorkerDaemon) Run() error {
	for !d.closed.Load().(bool) {
		worker, err := d.client.NewTunnelWorker(d.tunnelId, d.conf)
		if err != nil {
			return err
		}
		d.mu.Lock()
		d.worker = worker
		d.mu.Unlock()
		err = worker.ConnectAndWorking()
		if err != nil {
			worker.Shutdown()
			if d.closed.Load().(bool) {
				return nil
			}
			if isTunnelInvalid(err) {
				return err
			}
		} else {
			return nil
		}
		dur := d.random.Intn(DaemonRandomRetryMs) + 1
		time.Sleep(time.Duration(dur) * time.Millisecond)
	}
	return nil
}

func (d *TunnelWorkerDaemon) Close() {
	d.closed.Store(true)
	d.mu.Lock()
	defer d.mu.Unlock()
	if d.worker != nil {
		d.worker.Shutdown()
	}
}

func isTunnelInvalid(err error) bool {
	if terr, ok := err.(*TunnelError); ok {
		return terr.Code == ErrCodeParamInvalid || terr.Code == ErrCodeTunnelExpired ||
			terr.Code == ErrCodePermissionDenied
	}
	return false
}
