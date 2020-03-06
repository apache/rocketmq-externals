package sample

import (
	"encoding/json"
	"fmt"

	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore/search"
	"github.com/golang/protobuf/proto"
	"time"
)

/**
 *创建一个SearchIndex，包含Col_Keyword和Col_Long两列，类型分别设置为字符串(KEYWORD)和整型(LONG)。
 */
func CreateSearchIndex(client *tablestore.TableStoreClient, tableName string, indexName string) {
	fmt.Println("Begin to create table:", tableName)
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 1
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

	fmt.Println("Begin to create index:", indexName)
	request := &tablestore.CreateSearchIndexRequest{}
	request.TableName = tableName // 设置表名
	request.IndexName = indexName // 设置索引名

	schemas := []*tablestore.FieldSchema{}
	field1 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Keyword"),  // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_KEYWORD, // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		EnableSortAndAgg: proto.Bool(true),             // 设置开启排序与统计功能
	}
	field2 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Long"),
		FieldType:        tablestore.FieldType_LONG,
		Index:            proto.Bool(true),
		EnableSortAndAgg: proto.Bool(true),
	}
	schemas = append(schemas, field1, field2)

	request.IndexSchema = &tablestore.IndexSchema{
		FieldSchemas: schemas, // 设置SearchIndex包含的字段
	}
	resp, err := client.CreateSearchIndex(request) // 调用client创建SearchIndex
	if err != nil {
		fmt.Println("error :", err)
		return
	}
	fmt.Println("CreateSearchIndex finished, requestId:", resp.ResponseInfo.RequestId)
}

/**
 *创建一个SearchIndex，包含Col_Keyword和Col_Long两列，类型分别设置为字符串(KEYWORD)和整型(LONG)，设置按照Col_Long这一列预先排序。
 */
func CreateSearchIndexWithIndexSort(client *tablestore.TableStoreClient, tableName string, indexName string) {
	fmt.Println("Begin to create index:", indexName)
	request := &tablestore.CreateSearchIndexRequest{}
	request.TableName = tableName // 设置表名
	request.IndexName = indexName // 设置索引名

	schemas := []*tablestore.FieldSchema{}
	field1 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Keyword"),  // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_KEYWORD, // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		EnableSortAndAgg: proto.Bool(true),             // 设置开启排序与统计功能
	}
	field2 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Long"),
		FieldType:        tablestore.FieldType_LONG,
		Index:            proto.Bool(true),
		EnableSortAndAgg: proto.Bool(true),
	}
	schemas = append(schemas, field1, field2)

	request.IndexSchema = &tablestore.IndexSchema{
		FieldSchemas: schemas, // 设置SearchIndex包含的字段
		IndexSort: &search.Sort{ // 设置indexsort，按照Col_Long的值逆序排序
			Sorters: []search.Sorter{
				&search.FieldSort{
					FieldName: "Col_Long",
					Order:     search.SortOrder_ASC.Enum(),
				},
			},
		},
	}
	resp, err := client.CreateSearchIndex(request) // 调用client创建SearchIndex
	if err != nil {
		fmt.Println("error :", err)
		return
	}
	fmt.Println("CreateSearchIndex finished, requestId:", resp.ResponseInfo.RequestId)
}

func ListSearchIndex(client *tablestore.TableStoreClient, tableName string) {
	request := &tablestore.ListSearchIndexRequest{}
	request.TableName = tableName
	resp, err := client.ListSearchIndex(request)
	if err != nil {
		fmt.Println("error: ", err)
		return
	}
	for _, info := range resp.IndexInfo {
		fmt.Printf("%#v\n", info)
	}
	fmt.Println("ListSearchIndex finished, requestId: ", resp.ResponseInfo.RequestId)
}

func DescribeSearchIndex(client *tablestore.TableStoreClient, tableName string, indexName string) {
	request := &tablestore.DescribeSearchIndexRequest{}
	request.TableName = tableName
	request.IndexName = indexName
	resp, err := client.DescribeSearchIndex(request)
	if err != nil {
		fmt.Println("error: ", err)
		return
	}
	fmt.Println("FieldSchemas:")
	for _, schema := range resp.Schema.FieldSchemas {
		fmt.Printf("%s\n", schema)
	}
	if resp.Schema.IndexSort != nil {
		fmt.Printf("IndexSort:\n")
		for _, sorter := range resp.Schema.IndexSort.Sorters {
			fmt.Printf("\t%#v\n", sorter)
		}
	}
	fmt.Println("DescribeSearchIndex finished, requestId: ", resp.ResponseInfo.RequestId)
}

func DeleteSearchIndex(client *tablestore.TableStoreClient, tableName string, indexName string) {
	request := &tablestore.DeleteSearchIndexRequest{}
	request.TableName = tableName
	request.IndexName = indexName
	resp, err := client.DeleteSearchIndex(request)
	if err != nil {
		fmt.Println("error: ", err)
		return
	}
	fmt.Println("DeleteSearchIndex finished, requestId: ", resp.ResponseInfo.RequestId)
}

func WriteData(client *tablestore.TableStoreClient, tableName string) {
	keywords := []string{"hangzhou", "tablestore", "ots"}
	for i := 0; i < 100; i++ {
		putRowRequest := new(tablestore.PutRowRequest)
		putRowChange := new(tablestore.PutRowChange)
		putRowChange.TableName = tableName
		putPk := new(tablestore.PrimaryKey)
		putPk.AddPrimaryKeyColumn("pk1", fmt.Sprintf("pk_%d", i))

		putRowChange.PrimaryKey = putPk
		putRowChange.AddColumn("Col_Keyword", keywords[i%len(keywords)])
		putRowChange.AddColumn("Col_Long", int64(i))
		putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
		putRowRequest.PutRowChange = putRowChange
		_, err := client.PutRow(putRowRequest)

		if err != nil {
			fmt.Println("putrow failed with error:", err)
		}
	}
}

/**
 * 使用Token进行翻页读取。
 * 如果SearchResponse返回了NextToken，可以使用这个Token发起下一次查询，
 * 直到NextToken为空(nil)，此时代表所有符合条件的数据已经读完。
 */
func QueryRowsWithToken(client *tablestore.TableStoreClient, tableName string, indexName string) {
	querys := []search.Query{
		&search.MatchAllQuery{},
		&search.TermQuery{
			FieldName: "Col_Keyword",
			Term:      "tablestore",
		},
	}
	for _, query := range querys {
		fmt.Printf("Test query: %#v\n", query)
		searchRequest := &tablestore.SearchRequest{}
		searchRequest.SetTableName(tableName)
		searchRequest.SetIndexName(indexName)
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(query)
		searchQuery.SetLimit(10)
		searchQuery.SetGetTotalCount(true)
		searchRequest.SetSearchQuery(searchQuery)
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		rows := searchResponse.Rows
		requestCount := 1
		for searchResponse.NextToken != nil {
			searchQuery.SetToken(searchResponse.NextToken)
			searchResponse, err = client.Search(searchRequest)
			if err != nil {
				fmt.Printf("%#v", err)
				return
			}
			requestCount++
			for _, r := range searchResponse.Rows {
				rows = append(rows, r)
			}
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess)
		fmt.Println("TotalCount: ", searchResponse.TotalCount)
		fmt.Println("RowsSize: ", len(rows))
		fmt.Println("RequestCount: ", requestCount)
	}
}

func MatchAllQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.MatchAllQuery{}
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchQuery.SetLimit(0)
	searchQuery.SetGetTotalCount(true) // 设置GetTotalCount为true后才会返回总条数
	searchRequest.SetSearchQuery(searchQuery)
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess)
	fmt.Println("TotalCount: ", searchResponse.TotalCount)
}

/**
 *  查询表中Col_Keyword这一列的值能够匹配"hangzhou"的数据，返回匹配到的总行数和一些匹配成功的行。
 */
func MatchQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.MatchQuery{}   // 设置查询类型为MatchQuery
	query.FieldName = "Col_Keyword" // 设置要匹配的字段
	query.Text = "hangzhou"         // 设置要匹配的值
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchQuery.SetOffset(0) // 设置offset为0
	searchQuery.SetLimit(20) // 设置limit为20，表示最多返回20条数据
	searchRequest.SetSearchQuery(searchQuery)
	searchResponse, err := client.Search(searchRequest)
	if err != nil { // 判断异常
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("TotalCount: ", searchResponse.TotalCount)     // 匹配的总行数
	fmt.Println("RowCount: ", len(searchResponse.Rows))        // 返回的行数
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody)) // 不设置columnsToGet，默认只返回主键
	}
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err = client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_Text这一列的值能够匹配"hangzhou shanghai"的数据，匹配条件为短语匹配(要求短语完整的按照顺序匹配)，返回匹配到的总行数和一些匹配成功的行。
 */
func MatchPhraseQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.MatchPhraseQuery{} // 设置查询类型为MatchPhraseQuery
	query.FieldName = "Col_Text"        // 设置要匹配的字段
	query.Text = "hangzhou shanghai"    // 设置要匹配的值
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchQuery.SetOffset(0) // 设置offset为0
	searchQuery.SetLimit(20) // 设置limit为20，表示最多返回20条数据
	searchRequest.SetSearchQuery(searchQuery)
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err = client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_Keyword这一列精确匹配"hangzhou"的数据。
 */
func TermQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.TermQuery{}    // 设置查询类型为TermQuery
	query.FieldName = "Col_Keyword" // 设置要匹配的字段
	query.Term = "hangzhou"         // 设置要匹配的值
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchQuery.SetLimit(100)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_Keyword这一列精确匹配"hangzhou"或"tablestore"的数据。
 */
func TermsQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.TermsQuery{}   // 设置查询类型为TermQuery
	query.FieldName = "Col_Keyword" // 设置要匹配的字段
	terms := make([]interface{}, 0)
	terms = append(terms, "hangzhou")
	terms = append(terms, "tablestore")
	query.Terms = terms // 设置要匹配的值
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchQuery.SetLimit(100)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_Keyword这一列前缀为"hangzhou"的数据。
 */
func PrefixQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.PrefixQuery{}  // 设置查询类型为PrefixQuery
	query.FieldName = "Col_Keyword" // 设置要匹配的字段
	query.Prefix = "hangzhou"       // 设置前缀
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 使用通配符查询，查询表中Col_Keyword这一列的值匹配"hang*u"的数据
 */
func WildcardQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.WildcardQuery{} // 设置查询类型为WildcardQuery
	query.FieldName = "Col_Keyword"
	query.Value = "hang*u"
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_Long这一列大于3的数据，结果按照Col_Long这一列的值逆序排序。
 */
func RangeQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	searchQuery := search.NewSearchQuery()
	rangeQuery := &search.RangeQuery{} // 设置查询类型为RangeQuery
	rangeQuery.FieldName = "Col_Long"  // 设置针对哪个字段
	rangeQuery.GT(3)                   // 设置该字段的范围条件，大于3
	searchQuery.SetQuery(rangeQuery)
	// 设置按照Col_Long这一列逆序排序
	searchQuery.SetSort(&search.Sort{
		[]search.Sorter{
			&search.FieldSort{
				FieldName: "Col_Long",
				Order:     search.SortOrder_DESC.Enum(),
			},
		},
	})
	searchRequest.SetSearchQuery(searchQuery)
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * Col_GeoPoint是GeoPoint类型，查询表中Col_GeoPoint这一列的值在左上角为"10,0", 右下角为"0,10"的矩形范围内的数据。
 */
func GeoBoundingBoxQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.GeoBoundingBoxQuery{} // 设置查询类型为GeoBoundingBoxQuery
	query.FieldName = "Col_GeoPoint"       // 设置比较哪个字段的值
	query.TopLeft = "10,0"                 // 设置矩形左上角
	query.BottomRight = "0,10"             // 设置矩形右下角
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_GeoPoint这一列的值距离中心点不超过一定距离的数据。
 */
func GeoDistanceQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.GeoDistanceQuery{} // 设置查询类型为GeoDistanceQuery
	query.FieldName = "Col_GeoPoint"
	query.CenterPoint = "5,5"       // 设置中心点
	query.DistanceInMeter = 10000.0 // 设置到中心点的距离条件，不超过10000米
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 查询表中Col_GeoPoint这一列的值在一个给定多边形范围内的数据。
 */
func GeoPolygonQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)
	query := &search.GeoPolygonQuery{} // 设置查询类型为GeoDistanceQuery
	query.FieldName = "Col_GeoPoint"
	query.Points = []string{"0,0", "5,5", "5,0"} // 设置多边形的顶点
	searchQuery := search.NewSearchQuery()
	searchQuery.SetQuery(query)
	searchRequest.SetSearchQuery(searchQuery)
	// 设置返回所有列
	searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
		ReturnAll: true,
	})
	searchResponse, err := client.Search(searchRequest)
	if err != nil {
		fmt.Printf("%#v", err)
		return
	}
	fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
	fmt.Println("RowCount: ", len(searchResponse.Rows))
	for _, row := range searchResponse.Rows {
		jsonBody, err := json.Marshal(row)
		if err != nil {
			panic(err)
		}
		fmt.Println("Row: ", string(jsonBody))
	}
}

/**
 * 通过BoolQuery进行复合条件查询。
 */
func BoolQuery(client *tablestore.TableStoreClient, tableName string, indexName string) {
	searchRequest := &tablestore.SearchRequest{}
	searchRequest.SetTableName(tableName)
	searchRequest.SetIndexName(indexName)

	/**
	 * 查询条件一：RangeQuery，Col_Long这一列的值要大于3
	 */
	rangeQuery := &search.RangeQuery{}
	rangeQuery.FieldName = "Col_Long"
	rangeQuery.GT(3)

	/**
	 * 查询条件二：MatchQuery，Col_Keyword这一列的值要匹配"hangzhou"
	 */
	matchQuery := &search.MatchQuery{}
	matchQuery.FieldName = "Col_Keyword"
	matchQuery.Text = "hangzhou"

	{
		/**
		 * 构造一个BoolQuery，设置查询条件是必须同时满足"条件一"和"条件二"
		 */
		boolQuery := &search.BoolQuery{
			MustQueries: []search.Query{
				rangeQuery,
				matchQuery,
			},
		}
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(boolQuery)
		searchRequest.SetSearchQuery(searchQuery)
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
	}
	{
		/**
		 * 构造一个BoolQuery，设置查询条件是至少满足"条件一"和"条件二"中的一个
		 */
		boolQuery := &search.BoolQuery{
			ShouldQueries: []search.Query{
				rangeQuery,
				matchQuery,
			},
			MinimumShouldMatch: proto.Int32(1),
		}
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(boolQuery)
		searchRequest.SetSearchQuery(searchQuery)
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
	}
}

/**
 *创建一个SearchIndex，为TEXT类型索引列自定义分词器
 */
func Analysis(client *tablestore.TableStoreClient, tableName string, indexName string) {
	fmt.Println("Begin to create table:", tableName)
	createtableRequest := new(tablestore.CreateTableRequest)

	tableMeta := new(tablestore.TableMeta)
	tableMeta.TableName = tableName
	tableMeta.AddPrimaryKeyColumn("pk1", tablestore.PrimaryKeyType_STRING)
	tableOption := new(tablestore.TableOption)
	tableOption.TimeToAlive = -1
	tableOption.MaxVersion = 1
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

	fmt.Println("Begin to create index:", indexName)
	request := &tablestore.CreateSearchIndexRequest{}
	request.TableName = tableName // 设置表名
	request.IndexName = indexName // 设置索引名

	schemas := []*tablestore.FieldSchema{}

	analyzer1 := tablestore.Analyzer_SingleWord
	analyzerParam1 := tablestore.SingleWordAnalyzerParameter{
		CaseSensitive:	proto.Bool(true),
		DelimitWord:	proto.Bool(true),
	}
	field1 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_SingleWord"),  // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_TEXT,       // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		Analyzer:         &analyzer1,                      // 设置分词器
		AnalyzerParameter: analyzerParam1,                 // 设置分词器参数(可选)
	}

	analyzer2 := tablestore.Analyzer_MaxWord
	field2 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_MaxWord"),  // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_TEXT,       // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		Analyzer:         &analyzer2,                      // 设置分词器
	}

	analyzer3 := tablestore.Analyzer_MinWord
	field3 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_MinWord"),  // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_TEXT,       // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		Analyzer:         &analyzer3,                      // 设置分词器
	}

	analyzer4 := tablestore.Analyzer_Split
	analyzerParam4 := tablestore.SplitAnalyzerParameter{Delimiter:proto.String("-")}
	field4 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Split"),    // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_TEXT,       // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		Analyzer:         &analyzer4,                      // 设置分词器
		AnalyzerParameter: analyzerParam4,                 // 设置分词器参数(可选)
	}

	analyzer5 := tablestore.Analyzer_Fuzzy
	analyzerParam5 := tablestore.FuzzyAnalyzerParameter{
		MinChars: 1,
		MaxChars: 4,
	}
	field5 := &tablestore.FieldSchema{
		FieldName:        proto.String("Col_Fuzzy"),    // 设置字段名，使用proto.String用于获取字符串指针
		FieldType:        tablestore.FieldType_TEXT,       // 设置字段类型
		Index:            proto.Bool(true),             // 设置开启索引
		Analyzer:         &analyzer5,                      // 设置分词器
		AnalyzerParameter: analyzerParam5,                 // 设置分词器参数(可选)
	}

	schemas = append(schemas, field1, field2, field3, field4, field5)

	request.IndexSchema = &tablestore.IndexSchema{
		FieldSchemas: schemas, // 设置SearchIndex包含的字段
	}
	resp, err := client.CreateSearchIndex(request) // 调用client创建SearchIndex
	if err != nil {
		fmt.Println("error :", err)
		return
	}
	fmt.Println("CreateSearchIndex finished, requestId:", resp.ResponseInfo.RequestId)

	// write data
	putRowRequest := new(tablestore.PutRowRequest)
	putRowChange := new(tablestore.PutRowChange)
	putRowChange.TableName = tableName
	putPk := new(tablestore.PrimaryKey)
	putPk.AddPrimaryKeyColumn("pk1", "pk1_value")

	putRowChange.PrimaryKey = putPk
	putRowChange.AddColumn("Col_SingleWord", "中华人民共和国国歌 People's Republic of China")
	putRowChange.AddColumn("Col_MaxWord", "中华人民共和国国歌 People's Republic of China")
	putRowChange.AddColumn("Col_MinWord", "中华人民共和国国歌 People's Republic of China")
	putRowChange.AddColumn("Col_Split", "2019-05-01")
	putRowChange.AddColumn("Col_Fuzzy", "老王是个工程师")
	putRowChange.SetCondition(tablestore.RowExistenceExpectation_IGNORE)
	putRowRequest.PutRowChange = putRowChange
	_, err2 := client.PutRow(putRowRequest)

	if err2 != nil {
		fmt.Println("putrow failed with error:", err2)
	}

	// wait a while
	time.Sleep(time.Duration(30) * time.Second)

	// search
	{
		searchRequest := &tablestore.SearchRequest{}
		searchRequest.SetTableName(tableName)
		searchRequest.SetIndexName(indexName)
		query := &search.MatchQuery{}      // 设置查询类型为MatchQuery
		query.FieldName = "Col_SingleWord" // 设置要匹配的字段
		query.Text = "歌"                   // 设置要匹配的值
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(query)
		searchRequest.SetSearchQuery(searchQuery)

		// 设置返回所有列
		searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
			ReturnAll: true,
		})
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
		for _, row := range searchResponse.Rows {
			jsonBody, err := json.Marshal(row)
			if err != nil {
				panic(err)
			}
			fmt.Println("Row: ", string(jsonBody))
		}
	}

	{
		searchRequest := &tablestore.SearchRequest{}
		searchRequest.SetTableName(tableName)
		searchRequest.SetIndexName(indexName)
		query := &search.MatchQuery{}      // 设置查询类型为MatchQuery
		query.FieldName = "Col_MaxWord" // 设置要匹配的字段
		query.Text = "中华人民共和国"        // 设置要匹配的值
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(query)
		searchRequest.SetSearchQuery(searchQuery)

		// 设置返回所有列
		searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
			ReturnAll: true,
		})
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
		for _, row := range searchResponse.Rows {
			jsonBody, err := json.Marshal(row)
			if err != nil {
				panic(err)
			}
			fmt.Println("Row: ", string(jsonBody))
		}
	}

	{
		searchRequest := &tablestore.SearchRequest{}
		searchRequest.SetTableName(tableName)
		searchRequest.SetIndexName(indexName)
		query := &search.MatchQuery{}      // 设置查询类型为MatchQuery
		query.FieldName = "Col_Split" // 设置要匹配的字段
		query.Text = "2019"        // 设置要匹配的值
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(query)
		searchRequest.SetSearchQuery(searchQuery)

		// 设置返回所有列
		searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
			ReturnAll: true,
		})
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
		for _, row := range searchResponse.Rows {
			jsonBody, err := json.Marshal(row)
			if err != nil {
				panic(err)
			}
			fmt.Println("Row: ", string(jsonBody))
		}
	}

	{
		searchRequest := &tablestore.SearchRequest{}
		searchRequest.SetTableName(tableName)
		searchRequest.SetIndexName(indexName)
		query := &search.MatchQuery{}      // 设置查询类型为MatchQuery
		query.FieldName = "Col_Fuzzy" // 设置要匹配的字段
		query.Text = "程"        // 设置要匹配的值
		searchQuery := search.NewSearchQuery()
		searchQuery.SetQuery(query)
		searchRequest.SetSearchQuery(searchQuery)

		// 设置返回所有列
		searchRequest.SetColumnsToGet(&tablestore.ColumnsToGet{
			ReturnAll: true,
		})
		searchResponse, err := client.Search(searchRequest)
		if err != nil {
			fmt.Printf("%#v", err)
			return
		}
		fmt.Println("IsAllSuccess: ", searchResponse.IsAllSuccess) // 查看返回结果是否完整
		fmt.Println("RowCount: ", len(searchResponse.Rows))
		for _, row := range searchResponse.Rows {
			jsonBody, err := json.Marshal(row)
			if err != nil {
				panic(err)
			}
			fmt.Println("Row: ", string(jsonBody))
		}
	}
}
