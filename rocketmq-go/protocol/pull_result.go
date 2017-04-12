package protocol

import (
	"container/list"
	"fmt"
)

type PullResult struct {
	pullStatus      PullStatus
	nextBeginOffset int64
	minOffset       int64
	maxOffset       int64
	msgFoundList    list.List
}

func NewPullResult(ps PullStatus, next, min, max int64, list list.List) *PullResult {
	return &PullResult{
		ps,
		next,
		min,
		max,
		list,
	}
}

func (result *PullResult) PullStatus() PullStatus {
	return result.pullStatus
}

func (result *PullResult) NextBeginOffset() int64 {
	return result.nextBeginOffset
}

func (result *PullResult) MaxOffset() int64 {
	return result.maxOffset
}

func (result *PullResult) MinOffset() int64 {
	return result.minOffset
}

func (result *PullResult) MsgFoundList() list.List {
	return result.msgFoundList
}

func (result *PullResult) SetMsgFoundList(list list.List) {
	result.msgFoundList = list
}

func (result *PullResult) String() string {
	return fmt.Sprintf("PullResult [pullStatus=%s, nextBeginOffset=%s, minOffset=%s, maxOffset=%s, msgFoundList=%s]",
		result.pullStatus, result.nextBeginOffset, result.minOffset, result.maxOffset, result.msgFoundList.Len()) // TODO: msgFoundList maybe nil
}
