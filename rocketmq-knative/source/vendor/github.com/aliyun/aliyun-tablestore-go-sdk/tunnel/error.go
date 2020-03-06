package tunnel

import (
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
)

const (
	ErrCodeParamInvalid      = "OTSParameterInvalid"
	ErrCodeResourceGone      = "OTSResourceGone"
	ErrCodeServerUnavailable = "OTSTunnelServerUnavailable"
	ErrCodeSequenceNotMatch  = "OTSSequenceNumberNotMatch"
	ErrCodeClientError       = "OTSClientError"
	ErrCodeTunnelExpired     = "OTSTunnelExpired"
	ErrCodePermissionDenied  = "OTSPermissionDenied"
	ErrCodeTunnelExist       = "OTSTunnelExist"
)

type TunnelError struct {
	Code      string
	Message   string
	RequestId string
	TunnelId  string
}

func (te *TunnelError) Error() string {
	return fmt.Sprintf("RequestId: %s, Code: %s, Message: %s", te.RequestId, te.Code, te.Message)
}

func (te *TunnelError) Temporary() bool {
	return te.Code == ErrCodeServerUnavailable
}

func pbErrToTunnelError(err *protocol.Error, reqId string) *TunnelError {
	return &TunnelError{
		Code:      err.GetCode(),
		Message:   err.GetMessage(),
		RequestId: reqId,
		TunnelId:  err.GetTunnelId(),
	}
}

func rawHttpToTunnelError(code int, body []byte, reqId string) *TunnelError {
	terr := &TunnelError{
		Message:   string(body),
		RequestId: reqId,
	}
	if code >= 500 && code < 600 {
		terr.Code = ErrCodeServerUnavailable
	} else {
		terr.Code = ErrCodeClientError
	}
	return terr
}
