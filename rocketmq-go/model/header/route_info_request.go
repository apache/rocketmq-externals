package header

type RouteInfoRequestHeader struct {
	Topic string
}

func (header *RouteInfoRequestHeader) FromMap(headerMap map[string]interface{}) {
}