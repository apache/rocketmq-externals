package sample

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"fmt"
	"time"
)

func PutRowWithTxnSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("begin to do two row operatin in one transaction")

	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("userid", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk2", tablestore.PrimaryKeyType_INTEGER)

	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 3
	reservedThroughput := new(tablestore.ReservedThroughput)
	reservedThroughput.Readcap = 0
	reservedThroughput.Writecap = 0
	createtableRequest.TableMeta = tableMeta
	createtableRequest.TableOption = tableOption
	createtableRequest.ReservedThroughput = reservedThroughput

	_, err := client.CreateTable(createtableRequest)
	if err != nil {
		fmt.Println("Failed to create table with error:", err)
	} else {
		fmt.Println("Create table finished")
	}

	userName := "user2"
	trans := new(tablestore.StartLocalTransactionRequest)
	trans.TableName = tableName
	transPk := new(tablestore.PrimaryKey)
	transPk.AddPrimaryKeyColumn("userid", userName)
	trans.PrimaryKey = transPk
	response, err := client.StartLocalTransaction(trans)
	if err != nil {
		fmt.Println("failed to create transaction", err)
	} else {
		fmt.Println("id:", *response.TransactionId)
	}

	putRowRequest := new(tablestore.PutRowRequest)
	putRowChange := new(tablestore.PutRowChange)
	putRowChange.TableName = tableName
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("userid", userName)
	putPk.AddPrimaryKeyColumn("pk2", int64(2))

	putRowChange.PrimaryKey = putPk
	putRowChange.AddColumn("col1", "col1data1")
	putRowChange.AddColumn("col2", int64(3))
	putRowChange.AddColumn("col3", []byte("test"))
	putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowChange.TransactionId = response.TransactionId
	putRowRequest.PutRowChange = putRowChange
	_, err = client.PutRow(putRowRequest)

	if err != nil {
		fmt.Println("failed to put row1", err)
	}

	getRowPk := new(tablestore.PrimaryKey)
	getRowPk.AddPrimaryKeyColumn("userid", userName)
	getRowPk.AddPrimaryKeyColumn("pk2", int64(3))
	getRowRequest := new(tablestore.GetRowRequest)

	criteria := new(tablestore.SingleRowQueryCriteria)
	criteria.PrimaryKey = getRowPk
	getRowRequest.SingleRowQueryCriteria = criteria
	getRowRequest.SingleRowQueryCriteria.TableName = tableName
	getRowRequest.SingleRowQueryCriteria.MaxVersion = 1
	getRowRequest.SingleRowQueryCriteria.TransactionId = response.TransactionId
	getResp, err := client.GetRow(getRowRequest)
	cols := getResp.GetColumnMap().Columns
	val := cols["col2"]
	var number int64
	if len(val) > 0 {
		number = val[0].Value.(int64) + 5
	} else {
		number = 20
	}

	putRowRequest2 := new(tablestore.PutRowRequest)
	putRowChange2 := new(tablestore.PutRowChange)
	putRowChange2.TableName = tableName
	putPk2 := new(tablestore.PrimaryKey)
	putPk2.AddPrimaryKeyColumn("userid", userName)
	putPk2.AddPrimaryKeyColumn("pk2", int64(3))

	putRowChange2.PrimaryKey = putPk2
	putRowChange2.AddColumn("col1", "col1data1")
	putRowChange2.AddColumn("col2", int64(number))
	putRowChange2.AddColumn("col3", []byte("test"))
	putRowChange2.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowRequest2.PutRowChange = putRowChange2
	putRowChange2.TransactionId = response.TransactionId
	_, err = client.PutRow(putRowRequest2)
	if err != nil {
		fmt.Println("failed to put row2", err)
	}
	fmt.Println("wait to commit")
	time.Sleep(2 * time.Second)
	fmt.Println("prepare to commit ")
	request := &tablestore.CommitTransactionRequest{}
	request.TransactionId = response.TransactionId
	commitResponse, err := client.CommitTransaction(request)
	if err != nil {
		fmt.Println("failed to commit txn:", err)
	} else {
		fmt.Println("finish txn:", commitResponse)
	}
}
