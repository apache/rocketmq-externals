package timeline

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"reflect"
	"strings"
)

var typeOfBytes = reflect.TypeOf([]byte(nil))

var DefaultStreamAdapter = &StreamMessageAdapter{
	IdKey:        "Id",
	ContentKey:   "Content",
	TimestampKey: "Timestamp",
	AttrPrefix:   "Attr_",
}

type Message interface{}

type ColumnMap map[string]interface{}

func LoadColumnMap(attrs []*tablestore.AttributeColumn) ColumnMap {
	cols := make(map[string]interface{})
	for _, attr := range attrs {
		cols[attr.ColumnName] = attr.Value
	}
	return cols
}

type MessageAdapter interface {
	Marshal(msg Message) (ColumnMap, error)
	Unmarshal(cols ColumnMap) (Message, error)
}

type StreamMessage struct {
	Id        string
	Content   interface{}
	Timestamp int64
	Attr      map[string]interface{}
}

type StreamMessageAdapter struct {
	IdKey        string
	ContentKey   string
	TimestampKey string
	AttrPrefix   string
}

func (s *StreamMessageAdapter) Marshal(msg Message) (ColumnMap, error) {
	sMsg, ok := msg.(*StreamMessage)
	if !ok {
		return nil, ErrUnexpected
	}
	cols := make(map[string]interface{})

	cols[s.IdKey] = sMsg.Id
	if err := checkInterface(sMsg.Content); err != nil {
		return nil, err
	}
	cols[s.ContentKey] = sMsg.Content
	cols[s.TimestampKey] = sMsg.Timestamp
	for key, val := range sMsg.Attr {
		if err := checkInterface(val); err != nil {
			return nil, err
		}
		cols[s.AttrPrefix+key] = val
	}
	return cols, nil
}

func checkInterface(val interface{}) error {
	v := reflect.ValueOf(val)
	switch v.Kind() {
	case reflect.Bool:
		return nil
	case reflect.Int64:
		return nil
	case reflect.String:
		return nil
	case reflect.Slice:
		if v.Type() == typeOfBytes {
			return nil
		}
		return ErrMisuse
	default:
		return ErrMisuse
	}
}

func (s *StreamMessageAdapter) Unmarshal(cols ColumnMap) (Message, error) {
	sMsg := new(StreamMessage)
	sMsg.Attr = make(map[string]interface{})
	for key, val := range cols {
		switch key {
		case s.IdKey:
			if id, ok := val.(string); ok {
				sMsg.Id = id
			} else {
				return nil, ErrUnexpected
			}
		case s.ContentKey:
			sMsg.Content = val
		case s.TimestampKey:
			if timestamp, ok := val.(int64); ok {
				sMsg.Timestamp = timestamp
			} else {
				return nil, ErrUnexpected
			}
		default:
			if strings.HasPrefix(key, s.AttrPrefix) {
				realKey := key[len(s.AttrPrefix):]
				sMsg.Attr[realKey] = val
			}
		}
	}
	return sMsg, nil
}
