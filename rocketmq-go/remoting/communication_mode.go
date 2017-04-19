package rocketmq

type CommunicationMode int

const (
	Sync CommunicationMode = iota
	Async
	OneWay
)
