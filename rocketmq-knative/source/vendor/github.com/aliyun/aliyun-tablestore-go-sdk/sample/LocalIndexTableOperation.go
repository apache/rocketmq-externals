package sample

import (
	"fmt"
	"strconv"

	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
)

func CreateTableWithLocalIndexSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("Begin to create table:", tableName)
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk2", tablestore.PrimaryKeyType_INTEGER)
	tableMeta.AddDefinedColumn("definedcol1", tablestore.DefinedColumn_STRING)
	tableMeta.AddDefinedColumn("definedcol2", tablestore.DefinedColumn_INTEGER)

	indexMeta := new(tablestore.IndexMeta)
	indexMeta.SetAsLocalIndex()
	indexMeta.AddPrimaryKeyColumn("pk1")
	indexMeta.AddPrimaryKeyColumn("definedcol1")
	indexMeta.AddDefinedColumn("definedcol2")
	indexMeta.IndexName = "testLocalIndex1"

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

	indexMeta.IndexName = "testLocalIndex2"
	indexReq := &tablestore.CreateIndexRequest{MainTableName: tableName, IndexMeta: indexMeta, IncludeBaseData: false}
	resp, err := client.CreateIndex(indexReq)
	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("Create index finished", resp)
	}

	deleteIndex := &tablestore.DeleteIndexRequest{MainTableName: tableName, IndexName: indexMeta.IndexName}
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
		fmt.Println("DescribeTableSample finished. indexinfo:", describ.IndexMetas[0], len(describ.IndexMetas))
	}

	// put single row to main table
	putRowRequest := new(tablestore.PutRowRequest)
	putRowChange := new(tablestore.PutRowChange)
	putRowChange.TableName = tableName
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("pk1", "pk1value1")
	putPk.AddPrimaryKeyColumn("pk2", int64(2))

	putRowChange.PrimaryKey = putPk
	putRowChange.AddColumn("definedcol1", "col1data1")
	putRowChange.AddColumn("definedcol2", int64(3))
	putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowRequest.PutRowChange = putRowChange
	_, err = client.PutRow(putRowRequest)

	if err != nil {
		fmt.Println("putrow failed with error:", err)
	} else {
		fmt.Println("putrow finished")
	}

	//get row from local index table
	fmt.Println("begin to get row")
	getRowRequest := new(tablestore.GetRowRequest)
	criteria := new(tablestore.SingleRowQueryCriteria)
	getPk := new(tablestore.PrimaryKey)
	getPk.AddPrimaryKeyColumn("pk1", "pk1value1")
	getPk.AddPrimaryKeyColumn("definedcol1", "col1data1")
	getPk.AddPrimaryKeyColumn("pk2", int64(2))

	criteria.PrimaryKey = getPk
	getRowRequest.SingleRowQueryCriteria = criteria
	getRowRequest.SingleRowQueryCriteria.TableName = "testLocalIndex1"
	getRowRequest.SingleRowQueryCriteria.MaxVersion = 1
	getResp, err1 := client.GetRow(getRowRequest)

	if err1 != nil {
		fmt.Println("getrow failed with error:", err1)
	} else {
		colmap := getResp.GetColumnMap()
		fmt.Println("length is ", len(colmap.Columns))
		fmt.Println("get row col0 result is ", getResp.Columns[0].ColumnName, getResp.Columns[0].Value)
	}

	//Multiple row operation
	fmt.Println("batch write row started")
	batchWriteReq := &tablestore.BatchWriteRowRequest{}
	for i := 0; i < 100; i++ {
		putRowChange := new(tablestore.PutRowChange)
		putRowChange.TableName = tableName
		putPk := new(tablestore.PrimaryKey)
		putPk.AddPrimaryKeyColumn("pk1", "pk1value1")
		putPk.AddPrimaryKeyColumn("pk2", int64(i))
		putRowChange.PrimaryKey = putPk
		putRowChange.AddColumn("definedcol1", "col1data"+strconv.Itoa(i))
		putRowChange.AddColumn("definedcol2", int64(i))
		putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
		batchWriteReq.AddRowChange(putRowChange)
	}

	response, err2 := client.BatchWriteRow(batchWriteReq)
	if err2 != nil {
		fmt.Println("batch request failed with:", response)
	} else {
		// todo check all succeed
		fmt.Println("batch write row finished")
	}

	//batch get row from local index table
	fmt.Println("batch get row from local index started")
	batchGetReq := &tablestore.BatchGetRowRequest{}
	mqCriteria := &tablestore.MultiRowQueryCriteria{}

	for i := 0; i < 100; i++ {
		pkToGet := new(tablestore.PrimaryKey)
		pkToGet.AddPrimaryKeyColumn("pk1", "pk1value1")
		pkToGet.AddPrimaryKeyColumn("definedcol1", "col1data"+strconv.Itoa(i))
		pkToGet.AddPrimaryKeyColumn("pk2", int64(i))
		mqCriteria.AddRow(pkToGet)
	}

	mqCriteria.MaxVersion = 1
	mqCriteria.TableName = "testLocalIndex1"
	batchGetReq.MultiRowQueryCriteria = append(batchGetReq.MultiRowQueryCriteria, mqCriteria)

	/*condition := tablestore.NewSingleColumnCondition("col1", tablestore.CT_GREATER_THAN, int64(0))
	mqCriteria.Filter = condition*/

	batchGetResponse, err := client.BatchGetRow(batchGetReq)

	if err != nil {
		fmt.Println("batachget failed with error:", err)
	} else {
		for _, row := range batchGetResponse.TableToRowsResult[mqCriteria.TableName] {
			if row.PrimaryKey.PrimaryKeys != nil {
				fmt.Println("get row with key", row.PrimaryKey.PrimaryKeys[0].Value, row.PrimaryKey.PrimaryKeys[1].Value, row.PrimaryKey.PrimaryKeys[2].Value)
			} else {
				fmt.Println("this row is not exist")
			}
		}
		fmt.Println("batchget finished")
	}
}
