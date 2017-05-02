package header


type CreateTopicRequestHeader struct {
	topic string
	defaultTopic string
	readQueueNum int
	writeQueueNum int
	perm int
	topicFilterType string
	topicSysFlag int
	order bool
}

func NewCreateTopicRequestHeader() CreateTopicRequestHeader {
	return CreateTopicRequestHeader{}
}
func (header CreateTopicRequestHeader) FromMap(headerMap map[string]interface{}) {

}
