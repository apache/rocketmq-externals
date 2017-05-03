package header

type PullMessageResponseHeader struct {
	SuggestWhichBrokerID int64
	NextBeginOffset int64
	MinOffset int64
	MaxOffset int64
}

func (header *PullMessageResponseHeader) FromMap(headerMap map[string]interface{}) {

}