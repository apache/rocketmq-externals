package tunnel

import (
	"errors"
	"go.uber.org/zap"
	"sync"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
)

type BatchGetStatusReq struct {
	callbackCh chan map[string]*protocol.Channel
}

func NewBatchGetStatusReq() *BatchGetStatusReq {
	return &BatchGetStatusReq{callbackCh: make(chan map[string]*protocol.Channel, 1)}
}

type TunnelStateMachine struct {
	batchUpdateStatusCh chan []*protocol.Channel
	batchGetStatusCh    chan *BatchGetStatusReq
	updateStatusCh      chan *protocol.Channel
	closeCh             chan struct{}
	wg                  sync.WaitGroup
	closeOnce           sync.Once

	dialer   ChannelDialer
	pFactory ChannelProcessorFactory
	tunnelId string
	clientId string

	api *TunnelApi

	channelConn     map[string]ChannelConn
	currentChannels map[string]*protocol.Channel

	lg *zap.Logger
}

func NewTunnelStateMachine(tunnelId, clientId string, dialer ChannelDialer, factory ChannelProcessorFactory, api *TunnelApi, lg *zap.Logger) *TunnelStateMachine {
	stateMachine := &TunnelStateMachine{
		batchUpdateStatusCh: make(chan []*protocol.Channel),
		batchGetStatusCh:    make(chan *BatchGetStatusReq),
		updateStatusCh:      make(chan *protocol.Channel),
		closeCh:             make(chan struct{}),
		dialer:              dialer,
		pFactory:            factory,
		tunnelId:            tunnelId,
		clientId:            clientId,
		api:                 api,
		channelConn:         make(map[string]ChannelConn),
		currentChannels:     make(map[string]*protocol.Channel),
		lg:                  lg,
	}
	stateMachine.wg.Add(1)
	go stateMachine.bgLoop()
	return stateMachine
}

func (s *TunnelStateMachine) BatchUpdateStatus(batchChannels []*protocol.Channel) {
	select {
	case s.batchUpdateStatusCh <- batchChannels:
	case <-s.closeCh:
	}
}

func (s *TunnelStateMachine) UpdateStatus(channel *ChannelStatus) {
	pbChannel := channel.ToPbChannel()
	select {
	case s.updateStatusCh <- pbChannel:
	case <-s.closeCh:
	}
}

func (s *TunnelStateMachine) BatchGetStatus(req *BatchGetStatusReq) ([]*protocol.Channel, error) {
	select {
	case s.batchGetStatusCh <- req:
		ret := <-req.callbackCh
		channels := make([]*protocol.Channel, len(ret))
		i := 0
		for _, channel := range ret {
			channels[i] = channel
			i++
		}
		return channels, nil
	case <-s.closeCh:
		return nil, errors.New("state machine is closed")
	}
}

func (s *TunnelStateMachine) Close() {
	s.closeOnce.Do(func() {
		close(s.closeCh)
		s.wg.Wait()
		for _, conn := range s.channelConn {
			go conn.Close()
		}
		s.lg.Info("state machine is closed")
	})
}

func (s *TunnelStateMachine) bgLoop() {
	defer s.wg.Done()
	for {
		select {
		case channels := <-s.batchUpdateStatusCh:
			s.doBatchUpdateStatus(channels)
		case channel := <-s.updateStatusCh:
			s.doUpdateStatus(channel)
		case req := <-s.batchGetStatusCh:
			req.callbackCh <- s.currentChannels
		case <-s.closeCh:
			s.lg.Info("state machine background loop is going to quite...")
			return
		}
	}
}

func (s *TunnelStateMachine) doUpdateStatus(channel *protocol.Channel) {
	cid := channel.GetChannelId()
	curChannel, ok := s.currentChannels[cid]
	if !ok {
		s.lg.Info("redundant channel", zap.String("channelId", cid),
			zap.String("status", protocol.ChannelStatus_name[int32(channel.GetStatus())]))
		return
	}
	if curChannel.GetVersion() >= channel.GetVersion() {
		s.lg.Info("expired channel version", zap.String("channelId", cid),
			zap.Int64("current version", curChannel.GetVersion()), zap.Int64("old version", channel.GetVersion()))
		return
	}
	s.currentChannels[cid] = channel
	if conn, ok := s.channelConn[cid]; ok {
		if conn.Closed() {
			delete(s.channelConn, cid)
		}
	}
}

func (s *TunnelStateMachine) doBatchUpdateStatus(batchChannels []*protocol.Channel) {
	s.currentChannels = validateChannels(batchChannels, s.currentChannels)
	// update
	for cid, channel := range s.currentChannels {
		conn, ok := s.channelConn[cid]
		if !ok {
			token, sequenceNumber, err := s.api.getCheckpoint(s.tunnelId, s.clientId, cid)
			if err != nil {
				s.lg.Error("get channel checkpoint failed", zap.String("tunnelId", s.tunnelId), zap.String("clientId", s.clientId),
					zap.String("channelId", cid), zap.Error(err))
				//failConn will turn channel to closed if necessary
				conn = &failConn{state: s}
			} else {
				processor := s.pFactory.NewProcessor(s.tunnelId, s.clientId, cid,
					newCheckpointer(s.api, s.tunnelId, s.clientId, cid, sequenceNumber+1))
				conn = s.dialer.ChannelDial(s.tunnelId, s.clientId, cid, token, processor, s)
			}
			s.channelConn[cid] = conn
		}
		go conn.NotifyStatus(ToChannelStatus(channel))
	}
	// clean
	for cid, conn := range s.channelConn {
		if _, ok := s.currentChannels[cid]; !ok {
			s.lg.Info("redundant channel conn", zap.String("channelId", cid))
			if !conn.Closed() {
				go conn.Close()
			}
			delete(s.channelConn, cid)
		}
	}
}

func validateChannels(newChans []*protocol.Channel, currentChans map[string]*protocol.Channel) map[string]*protocol.Channel {
	updateChannels := make(map[string]*protocol.Channel)
	for _, newChannel := range newChans {
		id := newChannel.GetChannelId()
		if oldChannel, ok := currentChans[id]; ok {
			if newChannel.GetVersion() >= oldChannel.GetVersion() {
				updateChannels[id] = newChannel
			} else {
				updateChannels[id] = oldChannel
			}
		} else {
			updateChannels[id] = newChannel
		}
	}
	return updateChannels
}
