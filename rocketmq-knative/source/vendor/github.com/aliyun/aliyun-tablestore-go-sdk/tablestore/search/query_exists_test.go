package search

import (
	"bytes"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore/otsprotocol"
	"github.com/golang/protobuf/proto"
	"testing"
)


func TestExistsQuery_Type(t *testing.T) {
	query := &ExistsQuery{
		FieldName:  "FieldBlabla",
	}
	if query.Type() != QueryType_ExistsQuery {
		t.Error("query type error: ", query.Type().ToPB())
	}
}

func TestExistsQuery_Serialize(t *testing.T) {
	query := &ExistsQuery{
		FieldName:  "FieldBlabla",
	}
	data, err := query.Serialize()
	if err != nil {
		t.Fatal("query serialize error: ", err)
	}
	if data == nil {
		t.Fatal("query serialize data is nil")
	}

	queryPb := new(otsprotocol.ExistsQuery)
	err = proto.Unmarshal(data, queryPb)
	if err != nil {
		t.Fatal("query deserialize failed: ", err)
	}
	if *queryPb.FieldName != "FieldBlabla" {
		t.Fatalf("query deserialize failed, FieldName expected:%v, actual:%v", query.FieldName, *queryPb.FieldName)
	}
}

func TestExistsQuery_ProtoBuffer(t *testing.T) {
	query := &ExistsQuery{
		FieldName:  "FieldBlabla",
	}
	queryPb, err := query.ProtoBuffer()
	if err != nil {
		t.Fatal("build query pb failed: ", err)
	}
	if queryPb.GetType() != otsprotocol.QueryType_EXISTS_QUERY {
		t.Fatalf("build query pb failed, Type expected:%v, actual:%v", otsprotocol.QueryType_EXISTS_QUERY, queryPb.GetType())
	}

	queryInner := &otsprotocol.ExistsQuery{}
	queryInner.FieldName = &query.FieldName
	queryInnerSerialized, _ := proto.Marshal(queryInner)

	if bytes.Compare(queryPb.GetQuery(), queryInnerSerialized) != 0 {
		t.Fatalf("build query pb failed, Query expected:%v, actual:%v", queryInnerSerialized, queryPb.GetQuery())
	}
}
