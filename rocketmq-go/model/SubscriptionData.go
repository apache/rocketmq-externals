package model

type SubscriptionData struct {
	// TODO
}

func BuildSubscriptionData(topic, subString string) (SubscriptionData, error)

func (s SubscriptionData) SubString() string