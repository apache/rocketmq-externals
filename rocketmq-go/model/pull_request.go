package model

type PullRequest struct {
	ConsumerGroup string
	MessageQueue  *MessageQueue
	ProcessQueue  *ProcessQueue
	NextOffset    int64
}