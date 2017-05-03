package model

type SubscriptionData struct {
	Topic           string
	SubString       string
	ClassFilterMode bool
	TagsSet         []string
	CodeSet         []string
	SubVersion      int64
}