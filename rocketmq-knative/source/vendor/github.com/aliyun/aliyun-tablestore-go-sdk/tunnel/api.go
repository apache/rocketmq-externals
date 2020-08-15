package tunnel

import (
	"bytes"
	"crypto/md5"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/cenkalti/backoff"
	"github.com/golang/protobuf/proto"
	"github.com/satori/go.uuid"
	"io"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"strings"
	"time"
)

const (
	userAgent      = "tablestore-tunnel-sdk-go/1.0.0"
	apiVersion     = "2018-04-01"
	xOtsDateFormat = "2006-01-02T15:04:05.123Z"

	createTunnelUri   = "/tunnel/create"
	deleteTunnelUri   = "/tunnel/delete"
	listTunnelUri     = "/tunnel/list"
	describeTunnelUri = "/tunnel/describe"
	connectUri        = "/tunnel/connect"
	heartbeatUri      = "/tunnel/heartbeat"
	shutdownUri       = "/tunnel/shutdown"
	getCheckpointUri  = "/tunnel/getcheckpoint"
	readRecordsUri    = "/tunnel/readrecords"
	checkpointUri     = "/tunnel/checkpoint"
	getRpoUri         = "/tunnel/getrpo"
	scheduleUri       = "/tunnel/schedule"
)

var (
	initRetryInterval = 16 * time.Millisecond
	maxRetryInterval  = time.Second
)

type TunnelMetaApi interface {
	CreateTunnel(req *CreateTunnelRequest) (resp *CreateTunnelResponse, err error)
	ListTunnel(req *ListTunnelRequest) (resp *ListTunnelResponse, err error)
	DescribeTunnel(req *DescribeTunnelRequest) (resp *DescribeTunnelResponse, err error)
	DeleteTunnel(req *DeleteTunnelRequest) (resp *DeleteTunnelResponse, err error)
	GetRpo(req *GetRpoRequest) (resp *GetRpoResponse, err error)
	Schedule(req *ScheduleRequest) (resp *ScheduleResponse, err error)
}

type TunnelApi struct {
	endpoint        string
	instanceName    string
	accessKeyId     string
	accessKeySecret string
	securityToken   string

	httpClient *http.Client

	retryMaxElapsedTime time.Duration
}

func NewTunnelApi(endpoint, instanceName, accessKeyId, accessKeySecret string, conf *TunnelConfig) *TunnelApi {
	return NewTunnelApiWithToken(endpoint, instanceName, accessKeyId, accessKeySecret, "", conf)
}

func NewTunnelApiWithToken(endpoint, instanceName, accessKeyId, accessKeySecret, token string, conf *TunnelConfig) *TunnelApi {
	tunnelApi := &TunnelApi{
		endpoint:        endpoint,
		instanceName:    instanceName,
		accessKeyId:     accessKeyId,
		accessKeySecret: accessKeySecret,
		securityToken:   token,
	}
	if conf == nil {
		conf = DefaultTunnelConfig
	}
	tunnelApi.httpClient = &http.Client{
		Transport: conf.Transport,
		Timeout:   conf.RequestTimeout,
	}
	tunnelApi.retryMaxElapsedTime = conf.MaxRetryElapsedTime
	return tunnelApi
}

// 请求服务端
func (api *TunnelApi) doRequest(uri string, req, resp proto.Message) (string, int, error) {
	//end := time.Now().Add(api.config.MaxRetryTime)
	url := fmt.Sprintf("%s%s", api.endpoint, uri)
	/* request body */
	var body []byte
	var err error
	if req != nil {
		body, err = proto.Marshal(req)
		if err != nil {
			return "", 0, &TunnelError{Code: ErrCodeClientError, Message: err.Error()}
		}
	} else {
		body = nil
	}
	var respBody []byte
	var requestId string
	bkoff := ExponentialBackoff(initRetryInterval, maxRetryInterval, api.retryMaxElapsedTime, backOffMultiplier, randomizationFactor)
	for {
		respBody, err, requestId = api.doRequestInternal(url, uri, body, resp)
		if err == nil {
			break
		} else {
			if !shouldRetry(err) {
				return requestId, 0, err
			}

			nextBkoff := bkoff.NextBackOff()
			if nextBkoff == backoff.Stop {

				return requestId, 0, err
			}
			time.Sleep(nextBkoff)
		}
	}

	if respBody == nil || len(respBody) == 0 {
		return requestId, 0, nil
	}

	err = proto.Unmarshal(respBody, resp)
	if err != nil {
		return requestId, 0, &TunnelError{Code: ErrCodeClientError, Message: fmt.Sprintf("decode resp failed: %s", err), RequestId: requestId}
	}

	return requestId, len(respBody), nil
}

func (api *TunnelApi) doRequestInternal(url string, uri string, body []byte, resp proto.Message) ([]byte, error, string) {
	hreq, err := http.NewRequest("POST", url, bytes.NewBuffer(body))
	if err != nil {
		return nil, err, ""
	}
	/* set headers */
	hreq.Header.Set("User-Agent", userAgent)

	date := time.Now().UTC().Format(xOtsDateFormat)

	hreq.Header.Set(xOtsDate, date)
	hreq.Header.Set(xOtsApiversion, apiVersion)
	hreq.Header.Set(xOtsAccesskeyid, api.accessKeyId)
	hreq.Header.Set(xOtsInstanceName, api.instanceName)

	md5Byte := md5.Sum(body)
	md5Base64 := base64.StdEncoding.EncodeToString(md5Byte[:16])
	hreq.Header.Set(xOtsContentmd5, md5Base64)

	otshead := createOtsHeaders(api.accessKeySecret)
	otshead.set(xOtsDate, date)
	otshead.set(xOtsApiversion, apiVersion)
	otshead.set(xOtsAccesskeyid, api.accessKeyId)
	if api.securityToken != "" {
		hreq.Header.Set(xOtsHeaderStsToken, api.securityToken)
		otshead.set(xOtsHeaderStsToken, api.securityToken)
	}
	traceId,_ := uuid.NewV4()
	hreq.Header.Set(xOtsHeaderTraceID, traceId.String())
	otshead.set(xOtsHeaderTraceID, traceId.String())

	otshead.set(xOtsContentmd5, md5Base64)
	otshead.set(xOtsInstanceName, api.instanceName)
	sign, err := otshead.signature(uri, "POST", api.accessKeySecret)

	if err != nil {
		return nil, err, ""
	}
	hreq.Header.Set(xOtsSignature, sign)

	/* end set headers */
	return api.postReq(hreq, url)
}

func (api *TunnelApi) postReq(req *http.Request, url string) ([]byte, error, string) {
	resp, err := api.httpClient.Do(req)
	if err != nil {
		return nil, err, ""
	}
	defer resp.Body.Close()

	reqId := getRequestId(resp)
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err, reqId
	}

	if (resp.StatusCode >= 200 && resp.StatusCode < 300) == false {
		terr := new(TunnelError)
		e := new(protocol.Error)
		errUm := proto.Unmarshal(body, e)
		if errUm != nil {
			terr = rawHttpToTunnelError(resp.StatusCode, body, reqId)
		} else {
			terr = pbErrToTunnelError(e, reqId)
		}
		return nil, terr, getRequestId(resp)
	}

	return body, nil, getRequestId(resp)
}

func getRequestId(response *http.Response) string {
	if response == nil {
		return ""
	}
	return response.Header.Get(xOtsRequestId)
}

func (api *TunnelApi) CreateTunnel(req *CreateTunnelRequest) (*CreateTunnelResponse, error) {
	tunnel := new(protocol.Tunnel)
	tunnel.TableName = &req.TableName
	tunnel.TunnelName = &req.TunnelName
	tunnel.StreamTunnelConfig = parseTunnelStreamConfig(req.StreamTunnelConfig)
	switch req.Type {
	case TunnelTypeBaseData:
		tunnel.TunnelType = protocol.TunnelType_BaseData.Enum()
	case TunnelTypeStream:
		tunnel.TunnelType = protocol.TunnelType_Stream.Enum()
	case TunnelTypeBaseStream:
		tunnel.TunnelType = protocol.TunnelType_BaseAndStream.Enum()
	default:
		return nil, errors.New("tunnelType should be BaseData or Stream or BaseAndStream.")
	}
	createTunnelRequest := &protocol.CreateTunnelRequest{
		Tunnel: tunnel,
	}
	createTunnelResponse := new(protocol.CreateTunnelResponse)
	traceId, _, err := api.doRequest(createTunnelUri, createTunnelRequest, createTunnelResponse)
	if err != nil {
		return nil, err
	}
	resp := &CreateTunnelResponse{
		ResponseInfo: ResponseInfo{traceId},
		TunnelId:     *createTunnelResponse.TunnelId,
	}
	return resp, nil
}

func (api *TunnelApi) DeleteTunnel(req *DeleteTunnelRequest) (*DeleteTunnelResponse, error) {
	deleteTunnelRequest := &protocol.DeleteTunnelRequest{
		TableName:  &req.TableName,
		TunnelName: &req.TunnelName,
	}
	deleteTunnelResponse := new(protocol.DeleteTunnelResponse)
	traceId, _, err := api.doRequest(deleteTunnelUri, deleteTunnelRequest, deleteTunnelResponse)
	if err != nil {
		return nil, err
	}
	return &DeleteTunnelResponse{ResponseInfo{traceId}}, nil
}

func (api *TunnelApi) ListTunnel(req *ListTunnelRequest) (*ListTunnelResponse, error) {
	listTunnelRequest := &protocol.ListTunnelRequest{
		TableName: &req.TableName,
	}
	listTunnelResponse := new(protocol.ListTunnelResponse)
	traceId, _, err := api.doRequest(listTunnelUri, listTunnelRequest, listTunnelResponse)
	if err != nil {
		return nil, err
	}
	tunnels := make([]*TunnelInfo, 0)
	for _, t := range listTunnelResponse.Tunnels {
		ti := &TunnelInfo{
			TunnelId:           *t.TunnelId,
			TunnelName:         t.GetTunnelName(),
			TunnelType:         *t.TunnelType,
			TableName:          *t.TableName,
			InstanceName:       *t.InstanceName,
			StreamId:           *t.StreamId,
			Stage:              *t.Stage,
			Expired:            t.GetExpired(),
			CreateTime:         time.Unix(0, t.GetCreateTime()),
			StreamTunnelConfig: parseProtoTunnelStreamConfig(t.StreamTunnelConfig),
		}
		tunnels = append(tunnels, ti)
	}
	resp := &ListTunnelResponse{
		ResponseInfo: ResponseInfo{traceId},
		Tunnels:      tunnels,
	}
	return resp, nil
}

func (api *TunnelApi) DescribeTunnel(req *DescribeTunnelRequest) (*DescribeTunnelResponse, error) {
	describeTunnelRequest := &protocol.DescribeTunnelRequest{
		TableName:  &req.TableName,
		TunnelName: &req.TunnelName,
	}
	describeTunnelResponse := new(protocol.DescribeTunnelResponse)
	traceId, _, err := api.doRequest(describeTunnelUri, describeTunnelRequest, describeTunnelResponse)
	if err != nil {
		return nil, err
	}
	t := describeTunnelResponse.Tunnel
	resp := &DescribeTunnelResponse{
		ResponseInfo: ResponseInfo{traceId},
		TunnelRPO:    describeTunnelResponse.GetTunnelRpo(),
		Tunnel: &TunnelInfo{
			TunnelId:           *t.TunnelId,
			TunnelName:         t.GetTunnelName(),
			TunnelType:         *t.TunnelType,
			TableName:          *t.TableName,
			InstanceName:       *t.InstanceName,
			StreamId:           *t.StreamId,
			Stage:              *t.Stage,
			Expired:            t.GetExpired(),
			CreateTime:         time.Unix(0, t.GetCreateTime()),
			StreamTunnelConfig: parseProtoTunnelStreamConfig(t.StreamTunnelConfig),
		},
		Channels: make([]*ChannelInfo, 0),
	}
	for _, c := range describeTunnelResponse.Channels {
		channelInfo := &ChannelInfo{
			ChannelId: *c.ChannelId,
		}
		if c.ChannelType != nil {
			channelInfo.ChannelType = *c.ChannelType
		}
		if c.ChannelStatus != nil {
			channelInfo.ChannelStatus = *c.ChannelStatus
		}
		if c.ClientId != nil {
			channelInfo.ClientId = *c.ClientId
		}
		channelInfo.ChannelRPO = c.GetChannelRpo()
		resp.Channels = append(resp.Channels, channelInfo)
	}
	return resp, nil
}

func (api *TunnelApi) GetRpo(req *GetRpoRequest) (*GetRpoResponse, error) {
	getRpoRequest := &protocol.GetRpoRequest{
		TunnelId: &req.TunnelId,
	}

	getRpoResponse := new(protocol.GetRpoResponse)
	_, _, err := api.doRequest(getRpoUri, getRpoRequest, getRpoResponse)
	if err != nil {
		return nil, err
	}

	resp := &GetRpoResponse{}
	if err := json.Unmarshal(getRpoResponse.RpoInfos, &(resp.RpoInfos)); err != nil {
		return nil, err
	}

	if err := json.Unmarshal(getRpoResponse.TunnelRpoInfos, &(resp.TunnelRpoInfos)); err != nil {
		return nil, err
	}
	return resp, nil
}

func (api *TunnelApi) connect(tunnelId string, timeout int64) (string, error) {
	connectRequest := new(protocol.ConnectRequest)
	connectRequest.TunnelId = &tunnelId
	clientConfig := new(protocol.ClientConfig)
	clientConfig.Timeout = &timeout
	tag, _ := os.Hostname()
	clientConfig.ClientTag = &tag
	connectRequest.ClientConfig = clientConfig
	connectResponse := new(protocol.ConnectResponse)
	_, _, err := api.doRequest(connectUri, connectRequest, connectResponse)
	if err != nil {
		return "", err
	}
	return *connectResponse.ClientId, nil
}

func (api *TunnelApi) heartbeat(tunnelId, clientId string, currentChannels []*protocol.Channel) ([]*protocol.Channel, error) {
	heartbeatRequest := new(protocol.HeartbeatRequest)
	heartbeatRequest.TunnelId = &tunnelId
	heartbeatRequest.ClientId = &clientId

	if currentChannels != nil && len(currentChannels) != 0 {
		heartbeatRequest.Channels = currentChannels
	}

	heartbeatResponse := new(protocol.HeartbeatResponse)
	_, _, err := api.doRequest(heartbeatUri, heartbeatRequest, heartbeatResponse)
	if err != nil {
		return nil, err
	}
	return heartbeatResponse.Channels, nil
}

func (api *TunnelApi) shutdown(tunnelId, clientId string) error {
	shutdownRequest := new(protocol.ShutdownRequest)
	shutdownRequest.TunnelId = &tunnelId
	shutdownRequest.ClientId = &clientId

	shutdownResponse := new(protocol.ShutdownResponse)
	_, _, err := api.doRequest(shutdownUri, shutdownRequest, shutdownResponse)
	if err != nil {
		return err
	}
	return nil
}

func (api *TunnelApi) getCheckpoint(tunnelId, clientId string, channelId string) (string, int64, error) {
	getCheckpointRequest := &protocol.GetCheckpointRequest{
		TunnelId:  &tunnelId,
		ChannelId: &channelId,
		ClientId:  &clientId,
	}

	getCheckpointResponse := new(protocol.GetCheckpointResponse)
	_, _, err := api.doRequest(getCheckpointUri, getCheckpointRequest, getCheckpointResponse)
	if err != nil {
		return "", 0, err
	}
	return *getCheckpointResponse.Checkpoint, *getCheckpointResponse.SequenceNumber, nil
}

func (api *TunnelApi) readRecords(tunnelId, clientId string, channelId string, token string) ([]*Record, string, string, int, error) {
	readRecordsRequest := &protocol.ReadRecordsRequest{
		TunnelId:  &tunnelId,
		ClientId:  &clientId,
		ChannelId: &channelId,
		Token:     &token,
	}

	readRecordsResponse := new(protocol.ReadRecordsResponse)
	traceId, size, err := api.doRequest(readRecordsUri, readRecordsRequest, readRecordsResponse)
	if err != nil {
		return nil, "", traceId, 0, err
	}

	records := make([]*Record, 0)
	for _, record := range readRecordsResponse.Records {
		typ, err := ParseActionType(record.ActionType)
		if err != nil {
			return nil, "", traceId, 0, err
		}
		record, err := DeserializeRecordFromRawBytes(record.Record, typ)
		if err != nil {
			return nil, "", traceId, 0, err
		}
		records = append(records, record)
	}
	nextToken := *readRecordsResponse.NextToken
	return records, nextToken, traceId, size, nil
}

func (api *TunnelApi) checkpoint(tunnelId, clientId string, channelId string, token string, sequenceNumber int64) error {
	checkpointRequest := &protocol.CheckpointRequest{
		TunnelId:       &tunnelId,
		ClientId:       &clientId,
		ChannelId:      &channelId,
		Checkpoint:     &token,
		SequenceNumber: &sequenceNumber,
	}

	checkpointResponse := new(protocol.CheckpointResponse)
	_, _, err := api.doRequest(checkpointUri, checkpointRequest, checkpointResponse)
	if err != nil {
		return err
	}
	return nil
}

func (api *TunnelApi) Schedule(req *ScheduleRequest) (*ScheduleResponse, error) {
	scheduleRequest := &protocol.ScheduleRequest{
		TunnelId: &req.TunnelId,
	}
	scheduleRequest.Channels = make([]*protocol.Channel, len(req.Channels))
	for i, ch := range req.Channels {
		version := int64(0) // version will not be used, just make proto 2 happy
		pCh := &protocol.Channel{
			ChannelId: proto.String(ch.ChannelId),
			Status:    ch.ChannelStatus.Enum(),
			Version:   &version,
		}
		scheduleRequest.Channels[i] = pCh
	}
	scheduleResponse := new(protocol.ScheduleResponse)
	traceId, _, err := api.doRequest(scheduleUri, scheduleRequest, scheduleResponse)
	if err != nil {
		return nil, err
	}
	return &ScheduleResponse{ResponseInfo{traceId}}, nil
}

func shouldRetry(err error) bool {
	if tErr, ok := err.(*TunnelError); ok {
		return tErr.Temporary()
	}
	if err == io.EOF || err == io.ErrUnexpectedEOF ||
		strings.Contains(err.Error(), io.EOF.Error()) || //retry on special net error contains EOF or reset
		strings.Contains(err.Error(), "Connection reset by peer") {
		return true
	}
	if nErr, ok := err.(net.Error); ok {
		return nErr.Temporary()
	}
	return false
}
