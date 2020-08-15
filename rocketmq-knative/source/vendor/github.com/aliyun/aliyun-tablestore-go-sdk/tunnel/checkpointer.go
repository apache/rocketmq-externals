package tunnel

import "fmt"

type Checkpointer interface {
	Checkpoint(token string) error
}

type defaultCheckpointer struct {
	api            *TunnelApi
	tunnelId       string
	clientId       string
	channelId      string
	sequenceNumber int64
}

func newCheckpointer(api *TunnelApi, tunnelId, clientId, channelId string, sequence int64) *defaultCheckpointer {
	return &defaultCheckpointer{
		api:            api,
		tunnelId:       tunnelId,
		clientId:       clientId,
		channelId:      channelId,
		sequenceNumber: sequence,
	}
}

func (cp *defaultCheckpointer) Checkpoint(token string) error {
	err := cp.api.checkpoint(cp.tunnelId, cp.clientId, cp.channelId, token, cp.sequenceNumber)
	if err != nil {
		if needCheck(err) {
			cerr := cp.checkSequence()
			if cerr != nil {
				return &TunnelError{Code: ErrCodeClientError, Message: fmt.Sprintf("checkpoint failed %s and check sequence failed %s", err, cerr)}
			}
		}
		return err
	} else {
		cp.sequenceNumber += 1
	}
	return nil
}

func needCheck(err error) bool {
	if terr, ok := err.(*TunnelError); ok {
		return terr.Code == ErrCodeSequenceNotMatch
	}
	return false
}

func (cp *defaultCheckpointer) checkSequence() error {
	_, seqNum, err := cp.api.getCheckpoint(cp.tunnelId, cp.clientId, cp.channelId)
	if err != nil {
		return err
	}
	cp.sequenceNumber = seqNum + 1
	return nil
}
