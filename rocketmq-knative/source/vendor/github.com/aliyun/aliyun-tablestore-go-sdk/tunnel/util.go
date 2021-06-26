package tunnel

import (
	"bytes"
	"encoding/base64"
	"errors"
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/cenkalti/backoff"
	"github.com/golang/protobuf/proto"
	"time"
)

var (
	randomizationFactor = 0.33
	backOffMultiplier   = 2.0
)

func StreamRecordSequenceLess(a, b *SequenceInfo) bool {
	if a.Epoch < b.Epoch {
		return true
	}
	if a.Epoch == b.Epoch {
		if a.Timestamp < b.Timestamp {
			return true
		}
		if a.Timestamp == b.Timestamp {
			return a.RowIndex < b.RowIndex
		}
	}
	return false
}

func ParseActionType(pbType *protocol.ActionType) (ActionType, error) {
	switch *pbType {
	case protocol.ActionType_PUT_ROW:
		return AT_Put, nil
	case protocol.ActionType_UPDATE_ROW:
		return AT_Update, nil
	case protocol.ActionType_DELETE_ROW:
		return AT_Delete, nil
	default:
		return ActionType(-1), &TunnelError{Code: ErrCodeClientError, Message: fmt.Sprintf("Unexpected action type %s", pbType.String())}
	}
}

func DeserializeRecordFromRawBytes(data []byte, actionType ActionType) (*Record, error) {
	rows, err := protocol.ReadRowsWithHeader(bytes.NewReader(data))
	if err != nil {
		return nil, err
	}
	record := new(Record)
	record.PrimaryKey = &PrimaryKey{}
	record.Type = actionType

	for _, pk := range rows[0].PrimaryKey {
		pkColumn := &PrimaryKeyColumn{ColumnName: string(pk.CellName), Value: pk.CellValue.Value}
		record.PrimaryKey.PrimaryKeys = append(record.PrimaryKey.PrimaryKeys, pkColumn)
	}

	if rows[0].Extension != nil {
		record.Timestamp = rows[0].Extension.Timestamp
		record.SequenceInfo = &SequenceInfo{
			Timestamp: rows[0].Extension.Timestamp,
			RowIndex:  rows[0].Extension.RowIndex,
		}
	}

	for _, cell := range rows[0].Cells {
		cellName := (string)(cell.CellName)
		dataColumn := &RecordColumn{Name: &cellName, Timestamp: &cell.CellTimestamp}
		if cell.CellValue != nil {
			dataColumn.Value = cell.CellValue.Value
		}
		switch cell.CellType {
		case protocol.DELETE_ONE_VERSION:
			dataColumn.Type = RCT_DeleteOneVersion
		case protocol.DELETE_ALL_VERSION:
			dataColumn.Type = RCT_DeleteAllVersions
		default:
			dataColumn.Type = RCT_Put
		}
		record.Columns = append(record.Columns, dataColumn)
	}

	return record, nil
}

func ExponentialBackoff(interval, maxInterval, maxElapsed time.Duration, multiplier, jitter float64) *backoff.ExponentialBackOff {
	b := backoff.NewExponentialBackOff()
	b.RandomizationFactor = jitter
	b.Multiplier = multiplier
	b.InitialInterval = interval
	b.MaxInterval = maxInterval
	b.MaxElapsedTime = maxElapsed
	b.Reset()
	return b
}

func ParseRequestToken(token string) (*protocol.TokenContentV2, error) {
	decoded, err := base64.StdEncoding.DecodeString(token)
	if err != nil {
		return nil, err
	}
	tokenPb := new(protocol.Token)
	err = proto.Unmarshal(decoded, tokenPb)
	if err != nil {
		return nil, err
	}

	if tokenPb.Version == nil {
		return nil, errors.New("Token miss must filed: version.")
	}

	innerMessage := tokenPb.Content
	if *tokenPb.Version == 1 {
		innerTokenPb := new(protocol.TokenContent)
		err = proto.Unmarshal(innerMessage, innerTokenPb)

		if err != nil {
			return nil, err
		} else {
			initCount := int64(0)
			return &protocol.TokenContentV2{
				PrimaryKey: innerTokenPb.PrimaryKey,
				Timestamp:  innerTokenPb.Timestamp,
				Iterator:   innerTokenPb.Iterator,
				TotalCount: &initCount,
			}, nil
		}
	} else if *tokenPb.Version == 2 {
		innerTokenPbV2 := new(protocol.TokenContentV2)
		err = proto.Unmarshal(innerMessage, innerTokenPbV2)

		if err != nil {
			return nil, err
		} else {
			return innerTokenPbV2, nil
		}
	} else {
		return nil, fmt.Errorf("not support")
	}
}

func streamToken(token string) (bool, error) {
	tok, err := ParseRequestToken(token)
	if err != nil {
		return false, err
	}
	if tok.GetIterator() == "" {
		return false, nil
	}
	return true, nil
}

func parseTunnelStreamConfig(config *StreamTunnelConfig) *protocol.StreamTunnelConfig {
	if config == nil {
		return nil
	}
	pbConfig := new(protocol.StreamTunnelConfig)
	if config.StartOffset != 0 {
		pbConfig.StartOffset = proto.Uint64(config.StartOffset)
	} else {
		pbConfig.Flag = config.Flag.Enum()
	}
	if config.EndOffset != 0 {
		pbConfig.EndOffset = proto.Uint64(config.EndOffset)
	}
	return pbConfig
}

func parseProtoTunnelStreamConfig(pbConfig *protocol.StreamTunnelConfig) *StreamTunnelConfig {
	if pbConfig == nil {
		return nil
	}
	return &StreamTunnelConfig{
		Flag:        pbConfig.GetFlag(),
		StartOffset: pbConfig.GetStartOffset(),
		EndOffset:   pbConfig.GetEndOffset(),
	}
}
