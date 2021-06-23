package sample

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"fmt"
)

func CreateTableWithGlobalIndexSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("Begin to create table:", tableName)
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk2", tablestore.PrimaryKeyType_INTEGER)
	tableMeta.AddDefinedColumn("definedcol1", tablestore.DefinedColumn_STRING)
	tableMeta.AddDefinedColumn("definedcol2", tablestore.DefinedColumn_INTEGER)

	indexMeta := new(tablestore.IndexMeta)
	indexMeta.AddPrimaryKeyColumn("pk1")
	indexMeta.AddDefinedColumn("definedcol1")
	indexMeta.AddDefinedColumn("definedcol2")
	indexMeta.IndexName = "testindex1"

	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 1
	reservedThroughput := new(tablestore.ReservedThroughput)
	reservedThroughput.Readcap = 0
	reservedThroughput.Writecap = 0
	createtableRequest.TableMeta = tableMeta
	createtableRequest.TableOption = tableOption
	createtableRequest.ReservedThroughput = reservedThroughput

	createtableRequest.AddIndexMeta(indexMeta)

	_, err := client.CreateTable(createtableRequest)

	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("Create table finished")
	}


	indexMeta.IndexName = "index2"
	indexReq := &tablestore.CreateIndexRequest{ MainTableName:tableName, IndexMeta: indexMeta, IncludeBaseData: false }
	resp, err := client.CreateIndex(indexReq)
	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("Create index finished", resp)
	}

	deleteIndex := &tablestore.DeleteIndexRequest{ MainTableName:tableName, IndexName: indexMeta.IndexName }
	resp2, err := client.DeleteIndex(deleteIndex)

	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("drop index finished", resp2)
	}

	describeTableReq := new(tablestore.DescribeTableRequest)
	describeTableReq.TableName = tableName
	describ, err := client.DescribeTable(describeTableReq)

	if err != nil {
		fmt.Println("failed to update table with error:", err)
	} else {
		fmt.Println("DescribeTableSample. indexinfo:", describ.IndexMetas[0], len(describ.IndexMetas))
	}

	addColumnsReq := new(tablestore.AddDefinedColumnRequest)
	addColumnsReq.TableName = tableName
	addColumnsReq.AddDefinedColumn("definedcol3", tablestore.DefinedColumn_INTEGER)

	_, err = client.AddDefinedColumn(addColumnsReq)
	if err != nil {
		fmt.Println("failed to add defined column with error:", err)
	}

	describeTableReq.TableName = tableName
	describ, err = client.DescribeTable(describeTableReq)

	if err != nil {
		fmt.Println("failed to describe table with error:", err)
	} else {
		fmt.Println("DescribeTableSample finished. indexinfo:", describ.IndexMetas[0], len(describ.IndexMetas))
	}
}