package header


type CreateTopicRequestHeader struct {
	Topic string
	DefaultTopic string
	ReadQueueNum int
	WriteQueueNum int
	Perm int
	TopicFilterType string
	TopicSysFlag int
	Order bool
}

func (header *CreateTopicRequestHeader) FromMap(headerMap map[string]interface{}) {

}
