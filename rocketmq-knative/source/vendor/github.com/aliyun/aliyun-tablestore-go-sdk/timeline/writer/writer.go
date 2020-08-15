package writer

import (
	"context"
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
	"time"
)

var (
	defaultConcurrent    = 300
	defaultFlushInterval = 30 * time.Millisecond
	defaultRetryTimeout  = 5 * time.Second
)

type Config struct {
	Concurrent    int
	FlushInterval time.Duration
	RetryTimeout  time.Duration
}

type BatchAddContext struct {
	id     string
	change tablestore.RowChange
	done   *promise.Future

	resp    *BatchAddResult
	start   time.Time
	retries int
}

type BatchAddResult struct {
	Id    string
	Value tablestore.RowResult
	Err   error
}

func NewBatchAdd(id string, change tablestore.RowChange, future *promise.Future) *BatchAddContext {
	return &BatchAddContext{
		id:     id,
		change: change,
		done:   future,
		start:  time.Now(),
	}
}

type BatchWriter struct {
	tablestore.TableStoreApi

	inputCh      chan *BatchAddContext
	flushCh      chan struct{}
	retryTimeout time.Duration

	cancel context.CancelFunc
	ctx    context.Context
}

func NewBatchWriter(client tablestore.TableStoreApi, conf *Config) *BatchWriter {
	if conf == nil {
		conf = &Config{
			Concurrent:    defaultConcurrent,
			FlushInterval: defaultFlushInterval,
			RetryTimeout:  defaultRetryTimeout,
		}
	}
	asyncDIn := make(chan *BatchAddContext, 1000)
	uploaderIn := make(chan map[string][]*BatchAddContext)
	reducerIn := make(chan *BatchAddContext, 1000)
	ctx, cancel := context.WithCancel(context.Background())
	w := &BatchWriter{
		TableStoreApi: client,
		inputCh:       asyncDIn,
		flushCh:       make(chan struct{}),
		retryTimeout:  conf.RetryTimeout,
		cancel:        cancel,
		ctx:           ctx,
	}
	ticker := time.NewTicker(conf.FlushInterval)
	go w.tickFlush(ticker)
	go w.asyncDispatcher(asyncDIn, uploaderIn)
	var concurrent = defaultConcurrent
	if conf.Concurrent > 0 {
		concurrent = conf.Concurrent
	}
	for i := 0; i < concurrent; i++ {
		go w.uploader(uploaderIn, reducerIn)
	}
	go w.reducer(reducerIn)
	return w
}

func (w *BatchWriter) BatchAdd(ctx *BatchAddContext) error {
	select {
	case w.inputCh <- ctx:
		return nil
	case <-w.ctx.Done():
		return w.ctx.Err()
	}
}

func (w *BatchWriter) Flush() error {
	select {
	case w.flushCh <- struct{}{}:
		return nil
	case <-w.ctx.Done():
		return w.ctx.Err()
	}
}

func (w *BatchWriter) Close() {
	w.cancel()
}

func (w *BatchWriter) tickFlush(ticker *time.Ticker) {
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			w.Flush()
		case <-w.ctx.Done():
			return
		}
	}
}

func (w *BatchWriter) asyncDispatcher(input <-chan *BatchAddContext, output chan<- map[string][]*BatchAddContext) {
	limit := 200
	batch := make(map[string][]*BatchAddContext)
	i := 0
	for {
		send := false
		select {
		case req := <-input:
			batch[req.change.GetTableName()] = append(batch[req.change.GetTableName()], req)
			i++
			if i == limit {
				send = true
			}
		case <-w.ctx.Done():
			return
		default:
			select {
			case req := <-input:
				batch[req.change.GetTableName()] = append(batch[req.change.GetTableName()], req)
				i++
				if i == limit {
					send = true
				}
			case <-w.flushCh:
				if i != 0 {
					send = true
				}
			case <-w.ctx.Done():
				return
			}
		}
		if send {
			select {
			case output <- batch:
				batch = make(map[string][]*BatchAddContext)
				i = 0
			case <-w.ctx.Done():
				return
			}
		}
	}
}

func (w *BatchWriter) uploader(input <-chan map[string][]*BatchAddContext, output chan<- *BatchAddContext) {
	for {
		var reqMap map[string][]*BatchAddContext
		select {
		case reqMap = <-input:
			otsReq := new(tablestore.BatchWriteRowRequest)
			for _, reqSlice := range reqMap {
				for _, req := range reqSlice {
					otsReq.AddRowChange(req.change)
				}
			}
			otsResp, err := w.BatchWriteRow(otsReq)
			if err != nil {
				for _, reqSlice := range reqMap {
					for _, req := range reqSlice {
						req.resp = &BatchAddResult{Err: err}
					}
				}
			} else {
				for _, results := range otsResp.TableToRowsResult {
					for _, result := range results {
						if result.IsSucceed {
							reqMap[result.TableName][result.Index].resp = &BatchAddResult{Value: result}
						} else {
							reqMap[result.TableName][result.Index].resp = &BatchAddResult{
								Err: fmt.Errorf("%s: %s", result.Error.Code, result.Error.Message),
							}
						}
					}
				}
			}
		case <-w.ctx.Done():
			return
		}
		for _, reqSlice := range reqMap {
			for _, req := range reqSlice {
				select {
				case output <- req:
				case <-w.ctx.Done():
					return
				}
			}
		}
	}
}

func (w *BatchWriter) reducer(input <-chan *BatchAddContext) {
	for {
		writeBack := false
		var req *BatchAddContext
		select {
		case req = <-input:
			if req.resp.Err == nil {
				writeBack = true
			} else {
				dur := defaultBackoff.backoff(req.retries)
				if time.Now().Add(dur).Sub(req.start) > w.retryTimeout {
					writeBack = true
				} else {
					req.retries++
					go w.backoffRetry(req, dur)
				}

			}
		case <-w.ctx.Done():
			return
		}
		if writeBack {
			req.done.Set(req.resp, req.resp.Err)
		}
	}
}

func (w *BatchWriter) backoffRetry(req *BatchAddContext, backoffDur time.Duration) {
	time.Sleep(backoffDur)
	select {
	case w.inputCh <- req:
	case <-w.ctx.Done():
	}
}
