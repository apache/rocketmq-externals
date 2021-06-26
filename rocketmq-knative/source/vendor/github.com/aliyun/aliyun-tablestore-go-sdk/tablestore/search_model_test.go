package tablestore

import (
	"github.com/aliyun/aliyun-tablestore-go-sdk/tablestore/otsprotocol"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"testing"
)

// ConvertFieldSchemaToPBFieldSchema

func TestConvertFieldSchemaToPBFieldSchema_SingleWord(t *testing.T) {
	analyzer := Analyzer_SingleWord
	analyzerParam := SingleWordAnalyzerParameter{
		CaseSensitive:	proto.Bool(true),
		DelimitWord:	proto.Bool(true),
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.SingleWordAnalyzerParameter{
		CaseSensitive:	proto.Bool(true),
		DelimitWord:	proto.Bool(true),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("single_word")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_SingleWord_NoDelimitWord(t *testing.T) {
	analyzer := Analyzer_SingleWord
	analyzerParam := SingleWordAnalyzerParameter{
		CaseSensitive:	proto.Bool(true),
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.SingleWordAnalyzerParameter{
		CaseSensitive:	proto.Bool(true),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("single_word")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_SingleWord_NoCaseSensitive(t *testing.T) {
	analyzer := Analyzer_SingleWord
	analyzerParam := SingleWordAnalyzerParameter{
		DelimitWord:	proto.Bool(true),
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.SingleWordAnalyzerParameter{
		DelimitWord:	proto.Bool(true),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("single_word")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_Split(t *testing.T) {
	analyzer := Analyzer_Split
	analyzerParam := SplitAnalyzerParameter{Delimiter:proto.String("-")}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.SplitAnalyzerParameter{
		Delimiter:	proto.String("-"),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("split")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_Split_NoDelimiter(t *testing.T) {
	analyzer := Analyzer_Split
	analyzerParam := SplitAnalyzerParameter{}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.SplitAnalyzerParameter{}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("split")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_Fuzzy(t *testing.T) {
	analyzer := Analyzer_Fuzzy
	analyzerParam := FuzzyAnalyzerParameter{
		MinChars:	2,
		MaxChars:	3,
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.FuzzyAnalyzerParameter{
		MinChars:	proto.Int32(2),
		MaxChars:	proto.Int32(3),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("fuzzy")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_Fuzzy_NoMinChars(t *testing.T) {
	analyzer := Analyzer_Fuzzy
	analyzerParam := FuzzyAnalyzerParameter{
		MaxChars:	3,
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.FuzzyAnalyzerParameter{
		MaxChars:	proto.Int32(3),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("fuzzy")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_Fuzzy_NoMaxChars(t *testing.T) {
	analyzer := Analyzer_Fuzzy
	analyzerParam := FuzzyAnalyzerParameter{
		MaxChars:	3,
	}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbAnalyzerParamExpected := &otsprotocol.FuzzyAnalyzerParameter{
		MaxChars:	proto.Int32(3),
	}
	bytesAnalyzerParamExpected, _ := proto.Marshal(pbAnalyzerParamExpected)

	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("fuzzy")
	pbSchemaExpected.AnalyzerParameter = bytesAnalyzerParamExpected

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, pbSchemaExpected.AnalyzerParameter, pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_MinWord(t *testing.T) {
	analyzer := Analyzer_MinWord

	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("min_word")

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, []byte(nil), pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_MaxWord(t *testing.T) {
	analyzer := Analyzer_MaxWord

	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("max_word")

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, []byte(nil), pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_SingleWordNoParam(t *testing.T) {
	analyzer := Analyzer_SingleWord
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			Analyzer:			&analyzer,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbSchemaExpected.Analyzer = proto.String("single_word")

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Equal(t, *pbSchemaExpected.Analyzer, *pbSchemas[0].Analyzer)
	assert.Equal(t, []byte(nil), pbSchemas[0].AnalyzerParameter)
}

func TestConvertFieldSchemaToPBFieldSchema_NoAnalyzerWithParam(t *testing.T) {
	analyzerParam := SingleWordAnalyzerParameter{CaseSensitive:proto.Bool(true)}
	schemas := []*FieldSchema{
		{
			FieldName:			proto.String("Col_Analyzer"),
			FieldType:			FieldType_TEXT,
			AnalyzerParameter:	analyzerParam,
		},
	}

	// convert to pb
	var pbSchemas []*otsprotocol.FieldSchema
	pbSchemas = convertFieldSchemaToPBFieldSchema(schemas)

	// expect result
	pbSchemaExpected := new(otsprotocol.FieldSchema)
	pbSchemaExpected.FieldName = proto.String("Col_Analyzer")
	pbSchemaExpected.FieldType = otsprotocol.FieldType_TEXT.Enum()

	// assert
	t.Log("pb actural ==> ", pbSchemas)
	assert.Equal(t, len(pbSchemas), 1)

	assert.Equal(t, *pbSchemaExpected.FieldName, *pbSchemas[0].FieldName)
	assert.Equal(t, *pbSchemaExpected.FieldType, *pbSchemas[0].FieldType)
	assert.Nil(t, pbSchemas[0].Analyzer)
	assert.Nil(t, pbSchemas[0].AnalyzerParameter)
}

// parseFieldSchemaFromPb

func TestParseFieldSchemaFromPb_SingleWord(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SingleWordAnalyzerParameter)
	pbParam.CaseSensitive = proto.Bool(true)
	pbParam.DelimitWord   = proto.Bool(true)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("single_word")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_SingleWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, true, *(fieldSchemas[0].AnalyzerParameter).(SingleWordAnalyzerParameter).CaseSensitive)
}

func TestParseFieldSchemaFromPb_SingleWord_NoDelimitWord(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SingleWordAnalyzerParameter)
	pbParam.CaseSensitive = proto.Bool(true)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("single_word")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_SingleWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, true, *(fieldSchemas[0].AnalyzerParameter).(SingleWordAnalyzerParameter).CaseSensitive)
	assert.Nil(t, (fieldSchemas[0].AnalyzerParameter).(SingleWordAnalyzerParameter).DelimitWord)
}

func TestParseFieldSchemaFromPb_SingleWord_NoCaseSensitive(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SingleWordAnalyzerParameter)
	pbParam.DelimitWord = proto.Bool(true)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("single_word")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_SingleWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Nil(t, (fieldSchemas[0].AnalyzerParameter).(SingleWordAnalyzerParameter).CaseSensitive)
	assert.Equal(t, true, *(fieldSchemas[0].AnalyzerParameter).(SingleWordAnalyzerParameter).DelimitWord)
}

func TestParseFieldSchemaFromPb_Split(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SplitAnalyzerParameter)
	pbParam.Delimiter = proto.String("-")
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("split")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Split
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, "-", *(fieldSchemas[0].AnalyzerParameter).(SplitAnalyzerParameter).Delimiter)
}

func TestParseFieldSchemaFromPb_Split_NoDelimiter(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SplitAnalyzerParameter)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("split")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Split
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Nil(t, (fieldSchemas[0].AnalyzerParameter).(SplitAnalyzerParameter).Delimiter)
}

func TestParseFieldSchemaFromPb_Fuzzy(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.FuzzyAnalyzerParameter)
	pbParam.MinChars = proto.Int32(2)
	pbParam.MaxChars = proto.Int32(3)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("fuzzy")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Fuzzy
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, int32(2), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MinChars)
	assert.Equal(t, int32(3), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MaxChars)
}

func TestParseFieldSchemaFromPb_Fuzzy_NoMinChars(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.FuzzyAnalyzerParameter)
	pbParam.MaxChars = proto.Int32(3)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("fuzzy")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Fuzzy
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, int32(0), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MinChars)
	assert.Equal(t, int32(3), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MaxChars)
}

func TestParseFieldSchemaFromPb_Fuzzy_NoMaxChars(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.FuzzyAnalyzerParameter)
	pbParam.MinChars = proto.Int32(2)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("fuzzy")
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Fuzzy
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)

	assert.Equal(t, int32(2), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MinChars)
	assert.Equal(t, int32(0), (fieldSchemas[0].AnalyzerParameter).(FuzzyAnalyzerParameter).MaxChars)
}

func TestParseFieldSchemaFromPb_MinWord(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("min_word")

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_MinWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)
	assert.Equal(t, nil, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_MaxWord(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("max_word")

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_MaxWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)
	assert.Equal(t, nil, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_SingleWordNoParam(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("single_word")

	pbFieldSchemas := []*otsprotocol.FieldSchema{
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_SingleWord
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)
	assert.Equal(t, nil, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_SplitNoParam(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("split")

	pbFieldSchemas := []*otsprotocol.FieldSchema{
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Split
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)
	assert.Equal(t, nil, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_FuzzyNoParam(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.Analyzer = proto.String("fuzzy")

	pbFieldSchemas := []*otsprotocol.FieldSchema{
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	analyzerExpected := Analyzer_Fuzzy
	assert.Equal(t, analyzerExpected, *fieldSchemas[0].Analyzer)
	assert.Equal(t, nil, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_NoAnalyzerWithParam(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SingleWordAnalyzerParameter)
	pbParam.CaseSensitive = proto.Bool(true)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	assert.Nil(t, fieldSchemas[0].Analyzer)
	assert.Nil(t, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_NoAnalyzerWithParam2(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.SplitAnalyzerParameter)
	//pbParam.Delimiter = proto.String("-")
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	assert.Nil(t, fieldSchemas[0].Analyzer)
	assert.Nil(t, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_NoAnalyzerWithParam3(t *testing.T) {
	// build pb
	pbParam := new(otsprotocol.FuzzyAnalyzerParameter)
	pbParam.MinChars = proto.Int32(2)
	pbParam.MaxChars = proto.Int32(3)
	pbParamBytes, _:= proto.Marshal(pbParam)

	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()
	pbFieldSchema.AnalyzerParameter = pbParamBytes

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	assert.Nil(t, fieldSchemas[0].Analyzer)
	assert.Nil(t, fieldSchemas[0].AnalyzerParameter)
}

func TestParseFieldSchemaFromPb_NoAnalyzerNoParam(t *testing.T) {
	// build pb
	pbFieldSchema := new(otsprotocol.FieldSchema)
	pbFieldSchema.FieldName = proto.String("Col_Analyzer")
	pbFieldSchema.FieldType = otsprotocol.FieldType_TEXT.Enum()

	pbFieldSchemas := []*otsprotocol.FieldSchema {
		pbFieldSchema,
	}

	// pb -> model
	fieldSchemas := parseFieldSchemaFromPb(pbFieldSchemas)

	// assert
	t.Log("fieldSchemas ==> ", fieldSchemas)
	assert.Equal(t, len(fieldSchemas), 1)

	assert.Nil(t, fieldSchemas[0].Analyzer)
	assert.Nil(t, fieldSchemas[0].AnalyzerParameter)
}
