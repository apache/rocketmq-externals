package model

const SubscribeAll string = "*"

type SubscriptionData struct {
	ClassFilterMode bool
	Topic string
	subString string
	TagsSet map[string]bool
	CodeSet map[int]bool
	SubVersion int64
}


func BuildSubscriptionData(topic, subString string) (SubscriptionData, error)

func (s SubscriptionData) SubString() string