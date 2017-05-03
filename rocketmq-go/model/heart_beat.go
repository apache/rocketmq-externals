package model

type ConsumerData struct {
	GroupName           string
	ConsumeType        string
	MessageModel        string
	ConsumeFromWhere    string
	SubscriptionDataSet []*SubscriptionData
	UnitMode            bool
}
type ProducerData struct {
	GroupName string
}
type HeartbeatData struct {
	ClientId        string
	ConsumerDataSet []*ConsumerData
	ProducerDataSet []*ProducerData
}