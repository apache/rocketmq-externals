package sample

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
)

func PutRowSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println()
	putRowRequest := new(tablestore.PutRowRequest)
	putRowChange := new(tablestore.PutRowChange)
	putRowChange.TableName = tableName
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("pk1", "pk1value1")
	putPk.AddPrimaryKeyColumn("pk2", int64(2))
	putPk.AddPrimaryKeyColumn("pk3", []byte("pk3"))

	putRowChange.PrimaryKey = putPk
	putRowChange.AddColumn("col1", "col1data1")
	putRowChange.AddColumn("col2", int64(3))
	putRowChange.AddColumn("col3", []byte("test"))
	putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowRequest.PutRowChange = putRowChange
	_, err := client.PutRow(putRowRequest)

	if err != nil {
		fmt.Println("putrow failed with error:", err)
	} else {
		fmt.Println("putrow finished")
	}
}

func GetRowSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("begin to get row")
	getRowRequest := new(tablestore.GetRowRequest)
	criteria := new(tablestore.SingleRowQueryCriteria)
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("pk1", "pk1value1")
	putPk.AddPrimaryKeyColumn("pk2", int64(2))
	putPk.AddPrimaryKeyColumn("pk3", []byte("pk3"))

	criteria.PrimaryKey = putPk
	getRowRequest.SingleRowQueryCriteria = criteria
	getRowRequest.SingleRowQueryCriteria.TableName = tableName
	getRowRequest.SingleRowQueryCriteria.MaxVersion = 1
	getResp, err := client.GetRow(getRowRequest)

	colmap := getResp.GetColumnMap()

	fmt.Println("length is ", len(colmap.Columns))
	if err != nil {
		fmt.Println("getrow failed with error:", err)
	} else {
		fmt.Println("get row col0 result is ", getResp.Columns[0].ColumnName, getResp.Columns[0].Value)
	}
}

func DeleteRowSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("begin to delete row")
	deleteRowReq := new(tablestore.DeleteRowRequest)
	deleteRowReq.DeleteRowChange = new(tablestore.DeleteRowChange)
	deleteRowReq.DeleteRowChange.TableName = tableName
	deletePk := new(tablestore.PrimaryKey)
	deletePk.AddPrimaryKeyColumn("pk1", "pk1value1")
	deletePk.AddPrimaryKeyColumn("pk2", int64(2))
	deletePk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	deleteRowReq.DeleteRowChange.PrimaryKey = deletePk
	deleteRowReq.DeleteRowChange.SetCondition(tablestore.RowExistenceExpectation_EXPECT_EXIST)
	clCondition1 := tablestore.NewSingleColumnCondition("col2", tablestore.CT_EQUAL, int64(3))
	deleteRowReq.DeleteRowChange.SetColumnCondition(clCondition1)
	_, err := client.DeleteRow(deleteRowReq)

	if err != nil {
		fmt.Println("getrow failed with error:", err)
	} else {
		fmt.Println("delete row finished")
	}
}

func UpdateRowSample(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("begin to update row")
	updateRowRequest := new(tablestore.UpdateRowRequest)
	updateRowChange := new(tablestore.UpdateRowChange)
	updateRowChange.TableName = tableName
	updatePk := new(tablestore.PrimaryKey)
	updatePk.AddPrimaryKeyColumn("pk1", "pk1value1")
	updatePk.AddPrimaryKeyColumn("pk2", int64(2))
	updatePk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	updateRowChange.PrimaryKey = updatePk
	updateRowChange.DeleteColumn("col1")
	updateRowChange.PutColumn("col2", int64(77))
	updateRowChange.PutColumn("col4", "newcol3")
	updateRowChange.SetCondition(tablestore.RowExistenceExpectation_EXPECT_EXIST)
	updateRowRequest.UpdateRowChange = updateRowChange
	_, err := client.UpdateRow(updateRowRequest)

	if err != nil {
		fmt.Println("update failed with error:", err)
	} else {
		fmt.Println("update row finished")
	}
}

func UpdateRowWithIncrement(client *tablestore.TableStoreClient, tableName string) {
	fmt.Println("begin to update row")
	updateRowRequest := new(tablestore.UpdateRowRequest)
	updateRowChange := new(tablestore.UpdateRowChange)
	updateRowChange.TableName = tableName
	updatePk := new(tablestore.PrimaryKey)
	updatePk.AddPrimaryKeyColumn("pk1", "pk1increment")
	updatePk.AddPrimaryKeyColumn("pk2", int64(2))
	updatePk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	updateRowChange.PrimaryKey = updatePk

	updateRowChange.PutColumn("col2", int64(50))
	updateRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	updateRowRequest.UpdateRowChange = updateRowChange
	_, err := client.UpdateRow(updateRowRequest)

	if err != nil {
		fmt.Println("update failed with error:", err)
		return
	} else {
		fmt.Println("update row finished")
	}

	updateRowRequest = new(tablestore.UpdateRowRequest)
	updateRowChange = new(tablestore.UpdateRowChange)
	updateRowChange.TableName = tableName
	updatePk = new(tablestore.PrimaryKey)
	updatePk.AddPrimaryKeyColumn("pk1", "pk1increment")
	updatePk.AddPrimaryKeyColumn("pk2", int64(2))
	updatePk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	updateRowChange.PrimaryKey = updatePk

	updateRowChange.IncrementColumn("col2", int64(10))
	updateRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	updateRowRequest.UpdateRowChange = updateRowChange
	_, err = client.UpdateRow(updateRowRequest)
	if err != nil {
		fmt.Println("update failed with error:", err)
		return
	} else {
		fmt.Println("update row finished")
	}

	updateRowRequest = new(tablestore.UpdateRowRequest)
	updateRowChange = new(tablestore.UpdateRowChange)
	updateRowChange.TableName = tableName
	updatePk = new(tablestore.PrimaryKey)
	updatePk.AddPrimaryKeyColumn("pk1", "pk1increment")
	updatePk.AddPrimaryKeyColumn("pk2", int64(2))
	updatePk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	updateRowChange.PrimaryKey = updatePk

	updateRowChange.IncrementColumn("col2", int64(30))
	updateRowChange.SetReturnIncrementValue()
	updateRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	updateRowChange.AppendIncrementColumnToReturn("col2")
	updateRowRequest.UpdateRowChange = updateRowChange

	resp, err := client.UpdateRow(updateRowRequest)
	if err != nil {
		fmt.Println("update failed with error:", err)
		return
	} else {
		fmt.Println("update row finished")
		fmt.Println(resp)
		fmt.Println(len(resp.Columns))
		fmt.Println(resp.Columns[0].ColumnName)
		fmt.Println(resp.Columns[0].Value)
		fmt.Println(resp.Columns[0].Timestamp)
	}
}

func PutRowWithKeyAutoIncrementSample(client *tablestore.TableStoreClient) {
	fmt.Println("begin to put row")
	putRowRequest := new(tablestore.PutRowRequest)
	putRowChange := new(tablestore.PutRowChange)
	putRowChange.TableName = "incrementsampletable"
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("pk1", "pk1value1")
	putPk.AddPrimaryKeyColumnWithAutoIncrement("pk2")
	putPk.AddPrimaryKeyColumn("pk3", []byte("pk3"))
	putRowChange.PrimaryKey = putPk
	putRowChange.AddColumn("col1", "col1data1")
	putRowChange.AddColumn("col2", int64(100))
	putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowRequest.PutRowChange = putRowChange
	_, err := client.PutRow(putRowRequest)

	if err != nil {
		fmt.Println("put row failed with error:", err)
	} else {
		fmt.Println("put row finished")
	}
}
