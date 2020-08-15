package sample

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
)

func CreateTableSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("Begin to create table:", tableName)
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumn("pk2", tablestore.PrimaryKeyType_INTEGER)
	tableMeta.AddPrimaryKeyColumn("pk3", tablestore.PrimaryKeyType_BINARY)
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
}

func CreateTableKeyAutoIncrementSample(client *tablestore.TableStoreClient) {
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = "incrementsampletable"
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableMeta.AddPrimaryKeyColumnOption("pk2", tablestore.PrimaryKeyType_INTEGER, tablestore.AUTO_INCREMENT)
	tableMeta.AddPrimaryKeyColumn("pk3", tablestore.PrimaryKeyType_BINARY)
	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 3
	reservedThroughput := new(tablestore.ReservedThroughput)
	reservedThroughput.Readcap = 0
	reservedThroughput.Writecap = 0
	createtableRequest.TableMeta = tableMeta
	createtableRequest.TableOption = tableOption
	createtableRequest.ReservedThroughput = reservedThroughput

	client.CreateTable(createtableRequest)
}

func DeleteTableSample(client *tablestore.TableStoreClient) {
	fmt.Println("Begin to delete table")
	tableName := "tabletodeletesample"
	CreateTableSample(client, tableName)

	fmt.Println("Begin to delete table:", tableName)
	deleteReq := new(tablestore.DeleteTableRequest)
	deleteReq.TableName = tableName
	_, err := client.DeleteTable(deleteReq)
	if err != nil {
		fmt.Println("Failed to delete table with error:", err)
	} else {
		fmt.Println("Delete table finished")
	}
}

func ListTableSample(client *tablestore.TableStoreClient) {
	fmt.Println("Begin to list table")
	listtables, err := client.ListTable()
	if err != nil {
		fmt.Println("Failed to list table")
	} else {
		fmt.Println("List table result is")
		for _, table := range listtables.TableNames {
			fmt.Println("TableName: ", table)
		}
	}
}

func UpdateTableSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("UpdateTableSample started")
	updateTableReq := new(tablestore.UpdateTableRequest)
	updateTableReq.TableName = tableName
	updateTableReq.TableOption = new(tablestore.TableOption)
	updateTableReq.TableOption.TimeToAlive = -1
	updateTableReq.TableOption.MaxVersion = 5

	_, err := client.UpdateTable(updateTableReq)

	if err != nil {
		fmt.Println("failed to update table with error:", err)
	} else {
		fmt.Println("update finished")
	}
}

func DescribeTableSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("DescribeTableSample started")
	describeTableReq := new(tablestore.DescribeTableRequest)
	describeTableReq.TableName = tableName
	describ, err := client.DescribeTable(describeTableReq)

	if err != nil {
		fmt.Println("failed to update table with error:", err)
	} else {
		fmt.Println("DescribeTableSample finished. Table meta:", describ.TableOption.MaxVersion, describ.TableOption.TimeToAlive)
	}
}

func ComputeSplitPointsBySize(client *tablestore.TableStoreClient, tableName string) {
	req := &tablestore.ComputeSplitPointsBySizeRequest{TableName: tableName, SplitSize: int64(1)}
	va, err := client.ComputeSplitPointsBySize(req)
	if err != nil {
		fmt.Println(err)
	}

	for _, val := range va.Splits {
		fmt.Println(val.Location)
		fmt.Println(*val.LowerBound)
		fmt.Println(*val.UpperBound)
	}
	return
}
