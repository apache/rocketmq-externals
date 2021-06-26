package timeline

import (
	"context"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
)

type Api interface {
	Store(message Message) (int64, error)
	BatchStore(message Message) (*promise.Future, error)
	Update(sequenceId int64, message Message) error
	Load(sequenceId int64) (Message, error)
	Delete(sequenceId int64) error
	Scan(param *ScanParameter) *Iterator
}

type TmLine struct {
	id       string
	adapter  MessageAdapter
	storeApi MessageStore
}

func NewTmLine(id string, adapter MessageAdapter, store MessageStore) (*TmLine, error) {
	if store == nil || adapter == nil {
		return nil, ErrMisuse
	}
	return &TmLine{id: id, adapter: adapter, storeApi: store}, nil
}

func (l *TmLine) Store(message Message) (int64, error) {
	cols, err := l.adapter.Marshal(message)
	if err != nil {
		return 0, err
	}
	return l.storeApi.Store(l.id, cols)
}

func (l *TmLine) BatchStore(message Message) (*promise.Future, error) {
	cols, err := l.adapter.Marshal(message)
	if err != nil {
		return nil, err
	}
	return l.storeApi.BatchStore(l.id, cols), nil
}

func (l *TmLine) Update(sequenceId int64, message Message) error {
	cols, err := l.adapter.Marshal(message)
	if err != nil {
		return err
	}
	return l.storeApi.Update(l.id, sequenceId, cols)
}

func (l *TmLine) Load(sequenceId int64) (Message, error) {
	cols, err := l.storeApi.Load(l.id, sequenceId)
	if err != nil {
		return nil, err
	}
	return l.adapter.Unmarshal(cols)
}

func (l *TmLine) Delete(sequenceId int64) error {
	return l.storeApi.Delete(l.id, sequenceId)
}

func (l *TmLine) Scan(param *ScanParameter) *Iterator {
	retCh := make(chan *Entry, param.BufChanSize)
	errCh := make(chan error, param.ErrorChanSize)
	iterator, ctx := NewIterator(retCh, errCh)
	go l.asyncScan(ctx, param, retCh, errCh)
	return iterator
}

func (l *TmLine) asyncScan(ctx context.Context, param *ScanParameter, retCh chan *Entry, errCh chan error) {
	defer close(retCh)
	defer close(errCh)
	var count int
	for {
		select {
		case <-ctx.Done():
			return
		default:
		}
		retMap, next, err := l.storeApi.Scan(l.id, param)
		if err != nil {
			testClosedAndWriteError(ctx, errCh, err)
			return
		}
		for seq, colMap := range retMap {
			msg, err := l.adapter.Unmarshal(colMap)
			if err != nil {
				if testClosedAndWriteError(ctx, errCh, err) {
					return
				}
			} else {
				count++
				if testClosedAndWriteMsg(ctx, retCh, &Entry{seq, msg}) {
					return
				}
			}
		}
		if next == 0 || count == param.MaxCount {
			return
		}
		param.From = next
	}
}

func testClosedAndWriteError(ctx context.Context, ch chan error, err error) (closed bool) {
	select {
	case <-ctx.Done():
		return true
	case ch <- err:
		return false
	}
}

func testClosedAndWriteMsg(ctx context.Context, ch chan *Entry, msg *Entry) (closed bool) {
	select {
	case <-ctx.Done():
		return true
	case ch <- msg:
		return false
	}
}
