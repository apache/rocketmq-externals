package tablestore

import (
	"encoding/json"
	"errors"

	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore/otsprotocol"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore/search"
	"github.com/golang/protobuf/proto"
)

type ColumnsToGet struct {
	Columns   []string
	ReturnAll bool
}

type SearchRequest struct {
	TableName     string
	IndexName     string
	SearchQuery   search.SearchQuery
	ColumnsToGet  *ColumnsToGet
	RoutingValues []*PrimaryKey
}

func (r *SearchRequest) SetTableName(tableName string) *SearchRequest {
	r.TableName = tableName
	return r
}

func (r *SearchRequest) SetIndexName(indexName string) *SearchRequest {
	r.IndexName = indexName
	return r
}

func (r *SearchRequest) SetSearchQuery(searchQuery search.SearchQuery) *SearchRequest {
	r.SearchQuery = searchQuery
	return r
}

func (r *SearchRequest) SetColumnsToGet(columnToGet *ColumnsToGet) *SearchRequest {
	r.ColumnsToGet = columnToGet
	return r
}

func (r *SearchRequest) SetRoutingValues(routingValues []*PrimaryKey) *SearchRequest {
	r.RoutingValues = routingValues
	return r
}

func (r *SearchRequest) AddRoutingValue(routingValue *PrimaryKey) *SearchRequest {
	r.RoutingValues = append(r.RoutingValues, routingValue)
	return r
}

func (r *SearchRequest) ProtoBuffer() (*otsprotocol.SearchRequest, error) {
	req := &otsprotocol.SearchRequest{}
	req.TableName = &r.TableName
	req.IndexName = &r.IndexName
	query, err := r.SearchQuery.Serialize()
	if err != nil {
		return nil, err
	}
	req.SearchQuery = query
	pbColumns := &otsprotocol.ColumnsToGet{}
	pbColumns.ReturnType = otsprotocol.ColumnReturnType_RETURN_NONE.Enum()
	if r.ColumnsToGet != nil {
		if r.ColumnsToGet.ReturnAll {
			pbColumns.ReturnType = otsprotocol.ColumnReturnType_RETURN_ALL.Enum()
		} else if len(r.ColumnsToGet.Columns) > 0 {
			pbColumns.ReturnType = otsprotocol.ColumnReturnType_RETURN_SPECIFIED.Enum()
			pbColumns.ColumnNames = r.ColumnsToGet.Columns
		}
	}
	req.ColumnsToGet = pbColumns
	if r.RoutingValues != nil {
		for _, routingValue := range r.RoutingValues {
			req.RoutingValues = append(req.RoutingValues, routingValue.Build(false))
		}
	}
	return req, err
}

type SearchResponse struct {
	TotalCount   int64
	Rows         []*Row
	IsAllSuccess bool
	NextToken    []byte
	ResponseInfo
}

func convertFieldSchemaToPBFieldSchema(fieldSchemas []*FieldSchema) []*otsprotocol.FieldSchema {
	var schemas []*otsprotocol.FieldSchema
	for _, value := range fieldSchemas {
		field := new(otsprotocol.FieldSchema)

		field.FieldName = proto.String(*value.FieldName)
		field.FieldType = otsprotocol.FieldType(int32(value.FieldType)).Enum()

		if value.Index != nil {
			field.Index = proto.Bool(*value.Index)
		} else if value.FieldType != FieldType_NESTED {
			field.Index = proto.Bool(true)
		}
		if value.IndexOptions != nil {
			field.IndexOptions = otsprotocol.IndexOptions(int32(*value.IndexOptions)).Enum()
		}
		if value.Analyzer != nil {
			field.Analyzer = proto.String(string(*value.Analyzer))

			if value.AnalyzerParameter != nil {
				if *value.Analyzer == Analyzer_SingleWord {
					param := &otsprotocol.SingleWordAnalyzerParameter{}
					if value.AnalyzerParameter.(SingleWordAnalyzerParameter).CaseSensitive != nil {
						param.CaseSensitive = proto.Bool(*value.AnalyzerParameter.(SingleWordAnalyzerParameter).CaseSensitive)
					}
					if value.AnalyzerParameter.(SingleWordAnalyzerParameter).DelimitWord != nil {
						param.DelimitWord = proto.Bool(*value.AnalyzerParameter.(SingleWordAnalyzerParameter).DelimitWord)
					}
					if paramBytes, err := proto.Marshal(param); err == nil {
						field.AnalyzerParameter = paramBytes
					}
				} else if *value.Analyzer == Analyzer_Split {
					param := &otsprotocol.SplitAnalyzerParameter {}
					if value.AnalyzerParameter.(SplitAnalyzerParameter).Delimiter != nil {
						param.Delimiter = proto.String(*value.AnalyzerParameter.(SplitAnalyzerParameter).Delimiter)
					}
					if paramBytes, err := proto.Marshal(param); err == nil {
						field.AnalyzerParameter = paramBytes
					}
				} else if *value.Analyzer == Analyzer_Fuzzy {
					fuzzyParam := value.AnalyzerParameter.(FuzzyAnalyzerParameter)
					param := &otsprotocol.FuzzyAnalyzerParameter {}
					if fuzzyParam.MaxChars != 0 {
						param.MaxChars = proto.Int32(fuzzyParam.MaxChars)
					}
					if fuzzyParam.MinChars != 0 {
						param.MinChars = proto.Int32(fuzzyParam.MinChars)
					}
					if paramBytes, err := proto.Marshal(param); err == nil {
						field.AnalyzerParameter = paramBytes
					}
				}
			}
		}
		if value.EnableSortAndAgg != nil {
			field.SortAndAgg = proto.Bool(*value.EnableSortAndAgg)
		}
		if value.Store != nil {
			field.Store = proto.Bool(*value.Store)
		} else if value.FieldType != FieldType_NESTED {
			if *field.FieldType == otsprotocol.FieldType_TEXT {
				field.Store = proto.Bool(false)
			} else {
				field.Store = proto.Bool(true)
			}
		}
		if value.IsArray != nil {
			field.IsArray = proto.Bool(*value.IsArray)
		}
		if value.FieldType == FieldType_NESTED {
			field.FieldSchemas = convertFieldSchemaToPBFieldSchema(value.FieldSchemas)
		}

		schemas = append(schemas, field)
	}

	return schemas
}

func convertToPbSchema(schema *IndexSchema) (*otsprotocol.IndexSchema, error) {
	indexSchema := new(otsprotocol.IndexSchema)
	indexSchema.FieldSchemas = convertFieldSchemaToPBFieldSchema(schema.FieldSchemas)
	indexSchema.IndexSetting = new(otsprotocol.IndexSetting)
	var defaultNumberOfShards int32 = 1
	indexSchema.IndexSetting.NumberOfShards = &defaultNumberOfShards
	if schema.IndexSetting != nil {
		indexSchema.IndexSetting.RoutingFields = schema.IndexSetting.RoutingFields
	}
	if schema.IndexSort != nil {
		pbSort, err := schema.IndexSort.ProtoBuffer()
		if err != nil {
			return nil, err
		}
		indexSchema.IndexSort = pbSort
	}
	return indexSchema, nil
}

func parseFieldSchemaFromPb(pbFieldSchemas []*otsprotocol.FieldSchema) []*FieldSchema {
	var schemas []*FieldSchema
	for _, value := range pbFieldSchemas {
		field := new(FieldSchema)
		field.FieldName = value.FieldName
		field.FieldType = FieldType(*value.FieldType)
		field.Index = value.Index
		if value.IndexOptions != nil {
			indexOption := IndexOptions(*value.IndexOptions)
			field.IndexOptions = &indexOption
		}
		field.Analyzer = (*Analyzer)(value.Analyzer)
		if field.Analyzer != nil && *field.Analyzer == Analyzer_SingleWord && value.AnalyzerParameter != nil {
			param := new(otsprotocol.SingleWordAnalyzerParameter)
			if err := proto.Unmarshal(value.AnalyzerParameter, param); err == nil && param != nil {
				p := SingleWordAnalyzerParameter {}
				if param.CaseSensitive != nil {
					p.CaseSensitive = proto.Bool(*param.CaseSensitive)
				}
				if param.DelimitWord != nil {
					p.DelimitWord = proto.Bool(*param.DelimitWord)
				}
				field.AnalyzerParameter = p
			}
		} else if field.Analyzer != nil && *field.Analyzer == Analyzer_Split && value.AnalyzerParameter != nil {
			param := new(otsprotocol.SplitAnalyzerParameter)
			if err := proto.Unmarshal(value.AnalyzerParameter, param); err == nil && param != nil {
				p := SplitAnalyzerParameter {}
				if param.Delimiter != nil {
					p.Delimiter = proto.String(*param.Delimiter)
				}
				field.AnalyzerParameter = p
			}
		} else if field.Analyzer != nil && *field.Analyzer == Analyzer_Fuzzy && value.AnalyzerParameter != nil {
			param := new(otsprotocol.FuzzyAnalyzerParameter)
			if err := proto.Unmarshal(value.AnalyzerParameter, param); err == nil && param != nil {
				p := FuzzyAnalyzerParameter {}
				if param.MinChars != nil {
					p.MinChars = *param.MinChars
				}
				if param.MaxChars != nil {
					p.MaxChars = *param.MaxChars
				}
				field.AnalyzerParameter = p
			}
		}
		field.EnableSortAndAgg = value.SortAndAgg
		field.Store = value.Store
		field.IsArray = value.IsArray
		if field.FieldType == FieldType_NESTED {
			field.FieldSchemas = parseFieldSchemaFromPb(value.FieldSchemas)
		}
		schemas = append(schemas, field)
	}
	return schemas
}

func parseIndexSortFromPb(pbIndexSort *otsprotocol.Sort) (*search.Sort, error) {
	indexSort := &search.Sort{
		Sorters: make([]search.Sorter, 0),
	}
	for _, sorter := range pbIndexSort.GetSorter() {
		if sorter.GetFieldSort() != nil {
			fieldSort := &search.FieldSort{
				FieldName: *sorter.GetFieldSort().FieldName,
				Order:     search.ParseSortOrder(sorter.GetFieldSort().Order),
			}
			indexSort.Sorters = append(indexSort.Sorters, fieldSort)
		} else if sorter.GetPkSort() != nil {
			pkSort := &search.PrimaryKeySort{
				Order: search.ParseSortOrder(sorter.GetPkSort().Order),
			}
			indexSort.Sorters = append(indexSort.Sorters, pkSort)
		} else {
			return nil, errors.New("unknown index sort type")
		}
	}
	return indexSort, nil
}

func parseFromPbSchema(pbSchema *otsprotocol.IndexSchema) (*IndexSchema, error) {
	schema := &IndexSchema{
		IndexSetting: &IndexSetting{
			RoutingFields: pbSchema.IndexSetting.RoutingFields,
		},
	}
	schema.FieldSchemas = parseFieldSchemaFromPb(pbSchema.GetFieldSchemas())
	indexSort, err := parseIndexSortFromPb(pbSchema.GetIndexSort())
	if err != nil {
		return nil, err
	}
	schema.IndexSort = indexSort
	return schema, nil
}

type IndexSchema struct {
	IndexSetting *IndexSetting
	FieldSchemas []*FieldSchema
	IndexSort    *search.Sort
}

type FieldType int32

const (
	FieldType_LONG      FieldType = 1
	FieldType_DOUBLE    FieldType = 2
	FieldType_BOOLEAN   FieldType = 3
	FieldType_KEYWORD   FieldType = 4
	FieldType_TEXT      FieldType = 5
	FieldType_NESTED    FieldType = 6
	FieldType_GEO_POINT FieldType = 7
)

type IndexOptions int32

const (
	IndexOptions_DOCS      IndexOptions = 1
	IndexOptions_FREQS     IndexOptions = 2
	IndexOptions_POSITIONS IndexOptions = 3
	IndexOptions_OFFSETS   IndexOptions = 4
)

type Analyzer string

const (
	Analyzer_SingleWord Analyzer = "single_word"
	Analyzer_MaxWord    Analyzer = "max_word"
	Analyzer_MinWord	Analyzer = "min_word"
	Analyzer_Split		Analyzer = "split"
	Analyzer_Fuzzy		Analyzer = "fuzzy"
)

type SingleWordAnalyzerParameter struct {
	CaseSensitive	*bool
	DelimitWord		*bool
}

type SplitAnalyzerParameter struct {
	Delimiter		*string
}

type  FuzzyAnalyzerParameter struct {
	MinChars		int32
	MaxChars		int32
}

type FieldSchema struct {
	FieldName        *string
	FieldType        FieldType
	Index            *bool
	IndexOptions     *IndexOptions
	Analyzer         *Analyzer
	AnalyzerParameter	interface{}
	EnableSortAndAgg *bool
	Store            *bool
	IsArray          *bool
	FieldSchemas     []*FieldSchema
}

func (fs *FieldSchema) String() string {
	out, err := json.Marshal(fs)
	if err != nil {
		panic(err)
	}
	return string(out)
}

type IndexSetting struct {
	RoutingFields []string
}

type CreateSearchIndexRequest struct {
	TableName   string
	IndexName   string
	IndexSchema *IndexSchema
}

type CreateSearchIndexResponse struct {
	ResponseInfo ResponseInfo
}

type DescribeSearchIndexRequest struct {
	TableName string
	IndexName string
}

type SyncPhase int32

const (
	SyncPhase_FULL SyncPhase = 1
	SyncPhase_INCR SyncPhase = 2
)

type SyncStat struct {
	SyncPhase            SyncPhase
	CurrentSyncTimestamp *int64
}

type DescribeSearchIndexResponse struct {
	Schema       *IndexSchema
	SyncStat     *SyncStat
	ResponseInfo ResponseInfo
}

type ListSearchIndexRequest struct {
	TableName string
}

type IndexInfo struct {
	TableName string
	IndexName string
}

type ListSearchIndexResponse struct {
	IndexInfo    []*IndexInfo
	ResponseInfo ResponseInfo
}

type DeleteSearchIndexRequest struct {
	TableName string
	IndexName string
}

type DeleteSearchIndexResponse struct {
	ResponseInfo ResponseInfo
}
