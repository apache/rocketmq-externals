package timeline

import (
	"context"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
)

type ScanParameter struct {
	From          int64
	To            int64
	MaxCount      int
	IsForward     bool
	ColToGet      []string
	Filter        tablestore.ColumnFilter
	BufChanSize   int
	ErrorChanSize int
}

type Entry struct {
	Sequence int64
	Message  Message
}

type Iterator struct {
	retCh  chan *Entry
	errCh  chan error
	cancel context.CancelFunc
}

func NewIterator(retCh chan *Entry, errCh chan error) (*Iterator, context.Context) {
	ctx, cancel := context.WithCancel(context.Background())
	return &Iterator{
		retCh:  retCh,
		errCh:  errCh,
		cancel: cancel,
	}, ctx
}

func (i *Iterator) Next() (*Entry, error) {
	var (
		msg *Entry
		err error
		ok  bool
	)
	select {
	case msg, ok = <-i.retCh:
	default:
		select {
		case msg, ok = <-i.retCh:
		case err, ok = <-i.errCh:
		}
	}
	if !ok {
		return nil, ErrorDone
	}
	return msg, err
}

func (i *Iterator) Close() {
	i.cancel()
}
