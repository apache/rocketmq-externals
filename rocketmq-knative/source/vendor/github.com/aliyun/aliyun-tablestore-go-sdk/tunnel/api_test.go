package tunnel

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

var (
	alwaysFailUri   = "/tunnel/alwaysFail"
	badGateWayUri   = "/tunnel/badGateway"
	fail3timesUri   = "/tunnel/3timesFail"
	eof4timesUri    = "/tunnel/4timesEOF"
	successUri      = "/tunnel/success"
	reflectTokenUri = "/tunnel/reflectToken"

	requestId = "abcd-123"
)

func TestDoRequest_RetryBackoffElapsedTime(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)
	s := time.Now()

	traceId, _, err := api.doRequest(alwaysFailUri, nil, nil)
	c.Equal(traceId, requestId)
	tunnelErr, ok := err.(*TunnelError)
	c.True(ok)
	c.Equal(tunnelErr.Code, ErrCodeServerUnavailable)

	dur := time.Now().Sub(s)
	c.True(dur > api.retryMaxElapsedTime)
}

func TestDoRequest_BadGateWayElapsedTime(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)
	s := time.Now()

	traceId, _, err := api.doRequest(badGateWayUri, nil, nil)
	c.Equal(traceId, requestId)
	tunnelErr, ok := err.(*TunnelError)
	c.True(ok)
	c.Equal(tunnelErr.Code, ErrCodeServerUnavailable)

	dur := time.Now().Sub(s)
	c.True(dur > api.retryMaxElapsedTime)
}

func TestDoRequest_EOFRetry4Times(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)
	s := time.Now()

	traceId, _, err := api.doRequest(eof4timesUri, nil, nil)
	c.Equal(traceId, requestId)
	c.Nil(err)

	dur := time.Now().Sub(s)
	fmt.Println(dur)
	c.True(dur < 305*time.Millisecond)
	c.True(dur > 145*time.Millisecond)
}

func TestDoRequest_RetryBackoff3Times(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)
	s := time.Now()

	traceId, _, err := api.doRequest(fail3timesUri, nil, nil)
	c.Equal(traceId, requestId)
	c.Nil(err)

	dur := time.Now().Sub(s)
	fmt.Println(dur)
	c.True(dur < 150*time.Millisecond)
	c.True(dur > 74*time.Millisecond)
}

func TestDoRequest_Succeed(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)
	s := time.Now()

	_, size, err := api.doRequest(successUri, nil, nil)
	c.Nil(err)

	dur := time.Now().Sub(s)
	fmt.Println(dur)
	c.True(dur < initRetryInterval)
	c.Equal(size, 0)
}

func TestDoRequest_PassTraceId(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApi(ep, "testInstance", "testAkId", "testAkSec", nil)

	traceId, _, err := api.doRequest(successUri, nil, nil)
	c.Nil(err)
	c.True(traceId != requestId)
}

func TestDoRequest_SetToken(t *testing.T) {
	c := assert.New(t)
	ts := mockServer()
	defer ts.Close()

	ep := ts.URL
	api := NewTunnelApiWithToken(ep, "testInstance", "testAkId", "testAkSec",
		"testToken", nil)

	cp := new(protocol.GetCheckpointResponse)
	_, _, err := api.doRequest(reflectTokenUri, nil, cp)
	c.Nil(err)
	c.EqualValues(cp.GetCheckpoint(), "testToken")
}

func mockServer() *httptest.Server {
	handler := http.NewServeMux()
	handler.HandleFunc(alwaysFailUri, func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set(xOtsRequestId, requestId)
		w.WriteHeader(503)
		errCode := ErrCodeServerUnavailable
		message := "server busy"
		pbErr := &protocol.Error{
			Code:    &errCode,
			Message: &message,
		}
		buf, _ := proto.Marshal(pbErr)
		w.Write(buf)
	})
	handler.HandleFunc(badGateWayUri, func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set(xOtsRequestId, requestId)
		w.WriteHeader(502)
		message := "bad gateway"
		w.Write([]byte(message))
	})
	var times int32
	handler.HandleFunc(fail3timesUri, func(w http.ResponseWriter, r *http.Request) {
		t := atomic.AddInt32(&times, 1)
		if t <= 3 {
			w.Header().Set(xOtsRequestId, requestId)
			w.WriteHeader(503)
			errCode := ErrCodeServerUnavailable
			message := "server busy"
			pbErr := &protocol.Error{
				Code:    &errCode,
				Message: &message,
			}
			buf, _ := proto.Marshal(pbErr)
			w.Write(buf)
		} else {
			w.Header().Set(xOtsRequestId, requestId)
			w.Write(nil)
		}
	})
	var eofTimes int32
	handler.HandleFunc(eof4timesUri, func(w http.ResponseWriter, r *http.Request) {
		t := atomic.AddInt32(&eofTimes, 1)
		if t <= 4 {
			hj, ok := w.(http.Hijacker)
			if !ok {
				panic("hijack failed")
			}
			conn, _, err := hj.Hijack()
			if err != nil {
				panic(err)
			}
			conn.Close()
		} else {
			w.Header().Set(xOtsRequestId, requestId)
			w.Write(nil)
		}
	})
	handler.HandleFunc(successUri, func(w http.ResponseWriter, r *http.Request) {
		if id := r.Header.Get(xOtsHeaderTraceID); id != "" {
			w.Header().Set(xOtsRequestId, id)
		} else {
			w.Header().Set(xOtsRequestId, requestId)
		}
		w.Write(nil)
	})
	handler.HandleFunc(reflectTokenUri, func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get(xOtsHeaderStsToken)
		seq := int64(0)
		//reuse checkpoint response for convenient
		resp := new(protocol.GetCheckpointResponse)
		resp.Checkpoint = &token
		resp.SequenceNumber = &seq
		buf, _ := proto.Marshal(resp)
		w.Write(buf)
	})
	handler.HandleFunc(getCheckpointUri, func(w http.ResponseWriter, r *http.Request) {
		var req protocol.GetCheckpointRequest
		readBuf, err := ioutil.ReadAll(r.Body)
		if err != nil {
			w.WriteHeader(503)
			w.Write([]byte(err.Error()))
		}
		err = proto.Unmarshal(readBuf, &req)
		if err != nil {
			w.WriteHeader(503)
			w.Write([]byte(err.Error()))
		}
		if strings.Contains(req.GetChannelId(), "getCheckpointFailed") {
			code := ErrCodeParamInvalid
			msg := "return get checkpoint failed error"
			perr := &protocol.Error{
				Code:    &code,
				Message: &msg,
			}
			pbuf, _ := proto.Marshal(perr)
			w.WriteHeader(400)
			w.Write(pbuf)
			return
		}
		resp := new(protocol.GetCheckpointResponse)
		token := "token"
		seqNum := int64(1)
		resp.Checkpoint = &token
		resp.SequenceNumber = &seqNum
		buf, _ := proto.Marshal(resp)
		w.Write(buf)
	})
	handler.HandleFunc(checkpointUri, func(w http.ResponseWriter, r *http.Request) {
		if id := r.Header.Get(xOtsHeaderTraceID); id != "" {
			w.Header().Set(xOtsRequestId, id)
		} else {
			w.Header().Set(xOtsRequestId, requestId)
		}
		w.Write(nil)
	})
	return httptest.NewServer(handler)
}
