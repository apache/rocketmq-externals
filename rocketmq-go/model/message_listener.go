package model

type MessageListener func(msgs []MessageExt) ConsumeConcurrentlyResult
