package service_allocate_message

import (
	"errors"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
)

type AllocateMessageQueueAveragely struct{}

func (self *AllocateMessageQueueAveragely) Allocate(consumerGroup string, currentCID string, mqAll []*model.MessageQueue, cidAll []string) ([]model.MessageQueue, error) {

	if currentCID == "" {
		return nil, errors.New("currentCID is empty")
	}

	if mqAll == nil || len(mqAll) == 0 {
		return nil, errors.New("mqAll is nil or mqAll empty")
	}

	if cidAll == nil || len(cidAll) == 0 {
		return nil, errors.New("cidAll is nil or cidAll empty")
	}

	result := make([]model.MessageQueue, 0)
	for i, cid := range cidAll {
		if cid == currentCID {
			mqLen := len(mqAll)
			cidLen := len(cidAll)
			mod := mqLen % cidLen
			var averageSize int
			if mqLen < cidLen {
				averageSize = 1
			} else {
				if mod > 0 && i < mod {
					averageSize = mqLen/cidLen + 1
				} else {
					averageSize = mqLen / cidLen
				}
			}

			var startIndex int
			if mod > 0 && i < mod {
				startIndex = i * averageSize
			} else {
				startIndex = i*averageSize + mod
			}

			var min int
			if averageSize > mqLen-startIndex {
				min = mqLen - startIndex
			} else {
				min = averageSize
			}

			for j := 0; j < min; j++ {
				result = append(result, *mqAll[(startIndex+j)%mqLen])
			}
			return result, nil

		}
	}

	return nil, errors.New("cant't find currentCID")
}
