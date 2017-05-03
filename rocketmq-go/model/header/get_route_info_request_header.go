package header

type GetRouteInfoRequestHeader struct {
	Topic string `json:"topic"`
}

func (self *GetRouteInfoRequestHeader) FromMap(headerMap map[string]interface{}) {
	return
}