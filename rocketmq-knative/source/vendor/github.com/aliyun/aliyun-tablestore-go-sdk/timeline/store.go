package timeline

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/promise"
	"github.com/aliyun/aliyun-tablestore-go-sdk/timeline/writer"
	"strings"
)

var pkColumns = 2

type MessageStore interface {
	Sync() error
	Store(id string, cols ColumnMap) (int64, error)
	BatchStore(id string, cols ColumnMap) *promise.Future
	Update(id string, seq int64, cols ColumnMap) error
	Load(id string, seq int64) (ColumnMap, error)
	Delete(id string, seq int64) error
	Scan(id string, param *ScanParameter) (map[int64]ColumnMap, int64, error)
	Close()
}

type DefaultStore struct {
	api *writer.BatchWriter
	opt StoreOption
}

func NewDefaultStore(option StoreOption) (MessageStore, error) {
	err := option.prepare()
	if err != nil {
		return nil, err
	}
	client := tablestore.NewClientWithConfig(option.Endpoint, option.Instance, option.AkId, option.AkSecret, "", option.TableStoreConfig)
	bw := writer.NewBatchWriter(client, option.WriterConfig)
	store := &DefaultStore{api: bw, opt: option}
	err = store.Sync()
	if err != nil {
		return nil, err
	}
	return store, nil
}

func (s *DefaultStore) Sync() error {
	resp, err := s.api.DescribeTable(&tablestore.DescribeTableRequest{TableName: s.opt.TableName})
	if err != nil {
		if isTableNotExist(err) {
			return createTable(s.api, &s.opt)
		} else {
			return err
		}
	}
	entries := resp.TableMeta.SchemaEntry
	err = checkEntry(entries, s.opt.Schema)
	if err != nil {
		return err
	}
	//not support throughput update now
	if s.opt.TTL != 0 && s.opt.TTL != resp.TableOption.TimeToAlive {
		updateReq := &tablestore.UpdateTableRequest{
			TableName:   s.opt.TableName,
			TableOption: tablestore.NewTableOption(s.opt.TTL, resp.TableOption.MaxVersion),
		}
		_, err = s.api.UpdateTable(updateReq)
		return err
	}
	return nil
}

func (s *DefaultStore) Store(id string, cols ColumnMap) (int64, error) {
	change := toPutChange(id, cols, &s.opt)
	resp, err := s.api.PutRow(&tablestore.PutRowRequest{PutRowChange: change})
	if err != nil {
		return 0, err
	}
	return getSequence(resp.PrimaryKey.PrimaryKeys, s.opt.Schema)
}

func (s *DefaultStore) BatchStore(id string, cols ColumnMap) *promise.Future {
	f := promise.NewFuture()
	change := toPutChange(id, cols, &s.opt)
	ctx := writer.NewBatchAdd(id, change, f)
	err := s.api.BatchAdd(ctx)
	if err != nil {
		f.Set(nil, err)
	}
	return f
}

func (s *DefaultStore) Update(id string, seq int64, cols ColumnMap) error {
	change := &tablestore.UpdateRowChange{
		TableName:  s.opt.TableName,
		PrimaryKey: toPrimaryKey(id, seq, s.opt.Schema, false),
		Condition:  &tablestore.RowCondition{RowExistenceExpectation: tablestore.RowExistenceExpectation_EXPECT_EXIST},
	}
	for key, value := range cols {
		change.PutColumn(key, value)
	}
	_, err := s.api.UpdateRow(&tablestore.UpdateRowRequest{UpdateRowChange: change})
	return err
}

func (s *DefaultStore) Load(id string, seq int64) (ColumnMap, error) {
	criteria := &tablestore.SingleRowQueryCriteria{
		TableName:  s.opt.TableName,
		PrimaryKey: toPrimaryKey(id, seq, s.opt.Schema, false),
		MaxVersion: 1,
	}
	resp, err := s.api.GetRow(&tablestore.GetRowRequest{SingleRowQueryCriteria: criteria})
	if err != nil {
		return nil, err
	}
	return LoadColumnMap(resp.Columns), nil
}

func (s *DefaultStore) Delete(id string, seq int64) error {
	change := &tablestore.DeleteRowChange{
		TableName:  s.opt.TableName,
		PrimaryKey: toPrimaryKey(id, seq, s.opt.Schema, false),
		Condition:  &tablestore.RowCondition{RowExistenceExpectation: tablestore.RowExistenceExpectation_EXPECT_EXIST},
	}
	_, err := s.api.DeleteRow(&tablestore.DeleteRowRequest{DeleteRowChange: change})
	return err
}

func (s *DefaultStore) Scan(id string, param *ScanParameter) (map[int64]ColumnMap, int64, error) {
	criteria := toRangeCriteria(id, param, &s.opt)
	resp, err := s.api.GetRange(&tablestore.GetRangeRequest{RangeRowQueryCriteria: criteria})
	if err != nil {
		return nil, 0, err
	}
	seqColMap := make(map[int64]ColumnMap)
	for _, row := range resp.Rows {
		seq, cols, err := parseRow(row, &s.opt)
		if err != nil {
			return nil, 0, err
		}
		seqColMap[seq] = cols
	}
	var next int64
	if resp.NextStartPrimaryKey != nil {
		next, _ = getSequence(resp.NextStartPrimaryKey.PrimaryKeys, s.opt.Schema)
	}
	return seqColMap, next, nil
}

func (s *DefaultStore) Close() {
	s.api.Close()
}

func parseRow(row *tablestore.Row, opt *StoreOption) (int64, ColumnMap, error) {
	seq, err := getSequence(row.PrimaryKey.PrimaryKeys, opt.Schema)
	if err != nil {
		return 0, nil, err
	}
	return seq, LoadColumnMap(row.Columns), nil
}

func toRangeCriteria(id string, param *ScanParameter, opt *StoreOption) *tablestore.RangeRowQueryCriteria {
	criteria := &tablestore.RangeRowQueryCriteria{
		TableName:       opt.TableName,
		StartPrimaryKey: toPrimaryKey(id, param.From, opt.Schema, false),
		EndPrimaryKey:   toPrimaryKey(id, param.To, opt.Schema, false),
		MaxVersion:      1,
		ColumnsToGet:    param.ColToGet,
		Filter:          param.Filter,
		Direction:       tablestore.FORWARD,
	}
	if param.IsForward == false {
		criteria.Direction = tablestore.BACKWARD
	}
	return criteria
}

func toPutChange(id string, cols ColumnMap, opt *StoreOption) *tablestore.PutRowChange {
	pk := toPrimaryKey(id, 0, opt.Schema, true)
	change := &tablestore.PutRowChange{
		TableName:  opt.TableName,
		PrimaryKey: pk,
		Condition:  &tablestore.RowCondition{RowExistenceExpectation: tablestore.RowExistenceExpectation_IGNORE},
		ReturnType: tablestore.ReturnType_RT_PK,
	}
	for key, value := range cols {
		change.AddColumn(key, value)
	}
	return change
}

func toPrimaryKey(id string, seq int64, schema *Schema, autoInc bool) *tablestore.PrimaryKey {
	pk := new(tablestore.PrimaryKey)
	pk.AddPrimaryKeyColumn(schema.FirstPk, id)
	if autoInc {
		pk.AddPrimaryKeyColumnWithAutoIncrement(schema.SecondPk)
	} else {
		pk.AddPrimaryKeyColumn(schema.SecondPk, seq)
	}
	return pk
}

func getSequence(pks []*tablestore.PrimaryKeyColumn, schema *Schema) (int64, error) {
	if len(pks) != pkColumns {
		return 0, ErrUnexpected
	}
	if pks[0].ColumnName != schema.FirstPk || pks[1].ColumnName != schema.SecondPk {
		return 0, ErrUnexpected
	}
	seq, ok := pks[1].Value.(int64)
	if !ok {
		return 0, ErrUnexpected
	}
	return seq, nil
}

func isTableNotExist(err error) bool {
	return strings.Contains(err.Error(), "OTSObjectNotExist")
}

func createTable(api tablestore.TableStoreApi, opt *StoreOption) error {
	meta := &tablestore.TableMeta{
		TableName: opt.TableName,
	}
	meta.AddPrimaryKeyColumn(opt.Schema.FirstPk, tablestore.PrimaryKeyType_STRING)
	meta.AddPrimaryKeyColumnOption(opt.Schema.SecondPk, tablestore.PrimaryKeyType_INTEGER, tablestore.AUTO_INCREMENT)
	option := &tablestore.TableOption{
		TimeToAlive: opt.TTL,
		MaxVersion:  1,
	}
	req := &tablestore.CreateTableRequest{
		TableMeta:          meta,
		TableOption:        option,
		ReservedThroughput: opt.Throughput,
	}
	_, err := api.CreateTable(req)
	return err
}

func checkEntry(entry []*tablestore.PrimaryKeySchema, schema *Schema) error {
	if len(entry) != pkColumns {
		return ErrMisuse
	}
	if *entry[0].Name != schema.FirstPk || *entry[0].Type != tablestore.PrimaryKeyType_STRING {
		return ErrMisuse
	}
	if *entry[1].Name != schema.SecondPk || *entry[1].Type != tablestore.PrimaryKeyType_INTEGER ||
		*entry[1].Option != tablestore.AUTO_INCREMENT {
		return ErrMisuse
	}
	return nil
}
