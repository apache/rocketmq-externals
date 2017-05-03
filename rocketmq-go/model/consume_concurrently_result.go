package model

const (
	CONSUME_SUCCESS = "CONSUME_SUCCESS"
	RECONSUME_LATER = "RECONSUME_LATER"
)

type ConsumeConcurrentlyResult struct {
	ConsumeConcurrentlyStatus string
	AckIndex                  int
}