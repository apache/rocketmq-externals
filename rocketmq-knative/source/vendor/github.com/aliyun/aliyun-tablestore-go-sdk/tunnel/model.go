package tunnel

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"strconv"
	"strings"
	"time"
)

type TunnelType string

const (
	TunnelTypeBaseData   TunnelType = "BaseData"
	TunnelTypeStream     TunnelType = "Stream"
	TunnelTypeBaseStream TunnelType = "BaseAndStream"

	FinishTag = "finished"
)

type ResponseInfo struct {
	RequestId string
}

type CreateTunnelRequest struct {
	TableName          string
	TunnelName         string
	Type               TunnelType
	StreamTunnelConfig *StreamTunnelConfig
}

type CreateTunnelResponse struct {
	TunnelId string
	ResponseInfo
}

type ListTunnelRequest struct {
	TableName string
}

type StreamTunnelConfig struct {
	Flag        protocol.StartOffsetFlag
	StartOffset uint64
	EndOffset   uint64
}

type TunnelInfo struct {
	TunnelId           string
	TunnelName         string
	TunnelType         string
	TableName          string
	InstanceName       string
	StreamId           string
	Stage              string
	Expired            bool
	CreateTime         time.Time
	StreamTunnelConfig *StreamTunnelConfig
}

type ListTunnelResponse struct {
	Tunnels []*TunnelInfo
	ResponseInfo
}

type DescribeTunnelRequest struct {
	TableName  string
	TunnelName string
}

type ChannelInfo struct {
	ChannelId     string
	ChannelType   string
	ChannelStatus string
	ClientId      string
	ChannelRPO    int64
}

type DescribeTunnelResponse struct {
	TunnelRPO int64
	Tunnel    *TunnelInfo
	Channels  []*ChannelInfo
	ResponseInfo
}

type GetRpoRequest struct {
	TunnelId string
}

type GetRpoResponse struct {
	RpoInfos       map[string]map[string]*RpoLatency
	TunnelRpoInfos map[string]*TunnelRpoLatency
}

type RpoLatency struct {
	ChannelTyp ChannelType
	Status     string
	TotalCount int64
	AccessTime int64
	RpoTime    int64
}

type TunnelRpoLatency struct {
	Status     string
	TotalCount int64
	AccessTime int64
	RpoTime    int64
}

type ScheduleChannel struct {
	ChannelId     string
	ChannelStatus protocol.ChannelStatus
}

func SuspendChannel(channelId string) *ScheduleChannel {
	return &ScheduleChannel{
		ChannelId:     channelId,
		ChannelStatus: protocol.ChannelStatus_CLOSING,
	}
}

func TerminateChannel(channelId string) *ScheduleChannel {
	return &ScheduleChannel{
		ChannelId:     channelId,
		ChannelStatus: protocol.ChannelStatus_TERMINATED,
	}
}

func ResumeChannel(channelId string) *ScheduleChannel {
	return OpenChannel(channelId)
}

func OpenChannel(channelId string) *ScheduleChannel {
	return &ScheduleChannel{
		ChannelId:     channelId,
		ChannelStatus: protocol.ChannelStatus_OPEN,
	}
}

type ScheduleRequest struct {
	TunnelId string
	Channels []*ScheduleChannel
}

type ScheduleResponse struct {
	ResponseInfo
}

type ChannelType string

const (
	ChannelType_BaseData ChannelType = "BaseData"
	ChannelType_Stream   ChannelType = "Stream"
)

type DeleteTunnelRequest struct {
	TableName  string
	TunnelName string
}

type DeleteTunnelResponse struct {
	ResponseInfo
}

type PrimaryKey struct {
	PrimaryKeys []*PrimaryKeyColumn
}

type PrimaryKeyColumn struct {
	ColumnName string
	Value      interface{}
}

type SequenceInfo struct {
	// Epoch of stream log partition
	Epoch int32
	// stream log timestamp
	Timestamp int64
	// row index of stream log with same log timestamp
	RowIndex int32
}

type Record struct {
	Type      ActionType
	Timestamp int64
	// SequenceInfo is nil when it is a base data record,
	// while SequenceInfo is not nil when it is a stream record.
	SequenceInfo *SequenceInfo
	PrimaryKey   *PrimaryKey // required
	Columns      []*RecordColumn
}

func (r *Record) String() string {
	return fmt.Sprintf(
		"{\"Type\":%s, \"PrimaryKey\":%s, \"Columns\":%s}",
		r.Type,
		*r.PrimaryKey,
		r.Columns)
}

type ActionType int

const (
	AT_Put ActionType = iota
	AT_Update
	AT_Delete
)

func (t ActionType) String() string {
	switch t {
	case AT_Put:
		return "PutRow"
	case AT_Update:
		return "UpdateRow"
	case AT_Delete:
		return "DeleteRow"
	default:
		panic(fmt.Sprintf("unknown action type: %d", int(t)))
	}
}

type RecordColumn struct {
	Type      RecordColumnType
	Name      *string     // required
	Value     interface{} // optional. present when Type is RCT_Put
	Timestamp *int64      // optional, in msec. present when Type is RCT_Put or RCT_DeleteOneVersion
}

func (c *RecordColumn) String() string {
	xs := make([]string, 0)
	xs = append(xs, fmt.Sprintf("\"Name\":%s", strconv.Quote(*c.Name)))
	switch c.Type {
	case RCT_DeleteAllVersions:
		xs = append(xs, "\"Type\":\"DeleteAllVersions\"")
	case RCT_DeleteOneVersion:
		xs = append(xs, "\"Type\":\"DeleteOneVersion\"")
		xs = append(xs, fmt.Sprintf("\"Timestamp\":%d", *c.Timestamp))
	case RCT_Put:
		xs = append(xs, "\"Type\":\"Put\"")
		xs = append(xs, fmt.Sprintf("\"Timestamp\":%d", *c.Timestamp))
		xs = append(xs, fmt.Sprintf("\"Value\":%s", c.Value))
	}
	return fmt.Sprintf("{%s}", strings.Join(xs, ", "))
}

type RecordColumnType int

const (
	RCT_Put RecordColumnType = iota
	RCT_DeleteOneVersion
	RCT_DeleteAllVersions
)
