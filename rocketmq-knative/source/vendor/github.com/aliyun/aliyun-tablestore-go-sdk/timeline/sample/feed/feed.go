package main

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/writer"
	"math"
)

type Feed struct {
	historyStore timeline.MessageStore
	syncStore    timeline.MessageStore

	adapter timeline.MessageAdapter

	userId    string
	followers []string
}

func NewFeed(user string, storeOption, syncOption timeline.StoreOption, adapter timeline.MessageAdapter) (*Feed, error) {
	history, err := timeline.NewDefaultStore(storeOption)
	if err != nil {
		return nil, err
	}
	// if table is not exist, sync will create table
	// if table is already exist and StoreOption.TTL is not zero, sync will check and update table TTL if needed
	err = history.Sync()
	if err != nil {
		return nil, err
	}
	sync, err := timeline.NewDefaultStore(syncOption)
	if err != nil {
		return nil, err
	}
	err = sync.Sync()
	if err != nil {
		return nil, err
	}
	im := &Feed{
		userId:       user,
		historyStore: history,
		syncStore:    sync,
		adapter:      adapter,
	}
	return im, nil
}

func (f *Feed) Post(activity timeline.Message) ([]string, error) {
	// write activity to user's history activity store
	history, err := timeline.NewTmLine(f.userId, f.adapter, f.historyStore)
	if err != nil {
		return nil, err
	}
	seq, err := history.Store(activity)
	if err != nil {
		return nil, err
	}
	fmt.Println("activity auto increment sequence", seq)

	futures := make([]*promise.Future, len(f.followers))
	for i, m := range f.followers {
		receiver, err := timeline.NewTmLine(m, f.adapter, f.syncStore)
		if err != nil {
			return nil, err
		}
		f, err := receiver.BatchStore(activity)
		if err != nil {
			return nil, err
		}
		futures[i] = f
	}

	fanFuture := promise.FanIn(futures...)
	fanResult, err := fanFuture.FanInGet()
	if err != nil {
		return nil, err
	}
	failedId := make([]string, 0)
	for _, result := range fanResult {
		if result.Err != nil {
			failedId = append(failedId, result.Result.(*writer.BatchAddResult).Id)
		}
	}
	return failedId, nil
}

func (f *Feed) Refresh(lastReadSeq int64) ([]*timeline.Entry, error) {
	receiver, err := timeline.NewTmLine(f.userId, f.adapter, f.syncStore)
	if err != nil {
		return nil, err
	}
	iterator := receiver.Scan(&timeline.ScanParameter{
		From:        math.MaxInt64,
		To:          lastReadSeq,
		MaxCount:    100,
		BufChanSize: 10,
	})
	entries := make([]*timeline.Entry, 0)
	//avoid scanner goroutine leak
	defer iterator.Close()
	for {
		entry, err := iterator.Next()
		if err != nil {
			if err == timeline.ErrorDone {
				break
			} else {
				return entries, err
			}
		}
		entries = append(entries, entry)
	}
	return entries, nil
}

func (f *Feed) OwnHistory(numOfHistory int) ([]*timeline.Entry, error) {
	receiver, err := timeline.NewTmLine(f.userId, f.adapter, f.historyStore)
	if err != nil {
		return nil, err
	}
	iterator := receiver.Scan(&timeline.ScanParameter{
		From:        math.MaxInt64,
		To:          0,
		MaxCount:    numOfHistory,
		BufChanSize: 10,
	})
	entries := make([]*timeline.Entry, 0)
	//avoid scanner goroutine leak
	defer iterator.Close()
	for {
		entry, err := iterator.Next()
		if err != nil {
			if err == timeline.ErrorDone {
				break
			} else {
				return entries, err
			}
		}
		entries = append(entries, entry)
	}
	return entries, nil
}
