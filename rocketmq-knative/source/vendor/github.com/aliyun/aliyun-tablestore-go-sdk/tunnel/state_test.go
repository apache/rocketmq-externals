package tunnel

import (
	"errors"
	"fmt"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/golang/mock/gomock"
	. "github.com/smartystreets/goconvey/convey"
	"go.uber.org/zap"
	"sync"
	"testing"
	"time"
)

func TestTunnelStateMachine_BatchUpdateStatus(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	lg, _ := testLogConfig.Build()
	cidMu := new(sync.Mutex)
	cidTokenMap := map[string]string{}
	bypassApi := NewMocktunnelDataApi(mockCtrl)
	bypassApi.EXPECT().readRecords("tunnelId", "clientId", gomock.Any(), gomock.Any()).DoAndReturn(func(tid, cid, ccid, token string) ([]*Record, string, string, int, error) {
		cidMu.Lock()
		defer cidMu.Unlock()
		if tk, ok := cidTokenMap[ccid]; ok {
			return nil, tk, "traceId", 0, nil
		}
		return make([]*Record, 100), "token", "traceId", 1000 * 1024, nil
	}).AnyTimes()
	bc := &ChannelBackoffConfig{MaxDelay: time.Second}
	setDefault(bc)
	dialer := &channelDialer{
		api: bypassApi,
		lg:  lg,
		bc:  bc,
	}
	processor := &SimpleProcessFactory{
		CpInterval: time.Second,
		ProcessFunc: func(ctx *ChannelContext, r []*Record) error {
			if ctx.ChannelId == "cid-bad" {
				return errors.New("process failed")
			}
			time.Sleep(10 * time.Millisecond)
			return nil
		},
		ShutdownFunc: func(ctx *ChannelContext) {
			time.Sleep(5 * time.Millisecond)
		},
	}

	checkpointServer := mockServer()
	defer checkpointServer.Close()

	api := NewTunnelApi(checkpointServer.URL, "testInstance", "akid", "aksec", nil)

	state, _ := newStateWith4OpenChan(api, dialer, processor, lg)
	defer state.Close()

	sequencialCases := []struct {
		desc              string
		heartbeatChannels []*protocol.Channel
		expectChannels    []*protocol.Channel
		expectConnNum     int
		finishedCid       []string
	}{
		{
			"step1: two open, two closing",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-2", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-3", 1, protocol.ChannelStatus_CLOSING),
				newChannelWithCid("cid-4", 1, protocol.ChannelStatus_CLOSING),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-2", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-3", 2, protocol.ChannelStatus_CLOSE),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_CLOSE),
			},
			2,
			nil,
		},
		{
			"step 2: one open, one closing, two new",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-2", 1, protocol.ChannelStatus_CLOSING),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-2", 2, protocol.ChannelStatus_CLOSE),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
			},
			3,
			nil,
		},
		{
			"step 3: three open, one bad new, one new",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-bad", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-6", 0, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-bad", 1, protocol.ChannelStatus_CLOSE),
				newChannelWithCid("cid-6", 0, protocol.ChannelStatus_OPEN),
			},
			4,
			nil,
		},
		{
			"step 4: four open, one old version",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 1, protocol.ChannelStatus_CLOSING),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-6", 0, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-6", 0, protocol.ChannelStatus_OPEN),
			},
			4,
			nil,
		},
		{
			"step 5: four open, one finished",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-6", 0, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-5", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-6", 1, protocol.ChannelStatus_TERMINATED),
			},
			3,
			[]string{"cid-6"},
		},
		{
			"step 6: three open, somehow one missing(cid-5), one failconn",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-getCheckpointFailed", 1, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-getCheckpointFailed", 2, protocol.ChannelStatus_CLOSE),
			},
			2,
			nil,
		},
		{
			"step 7: all finished",
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 0, protocol.ChannelStatus_OPEN),
				newChannelWithCid("cid-4", 2, protocol.ChannelStatus_OPEN),
			},
			[]*protocol.Channel{
				newChannelWithCid("cid-1", 1, protocol.ChannelStatus_TERMINATED),
				newChannelWithCid("cid-4", 3, protocol.ChannelStatus_TERMINATED),
			},
			0,
			[]string{"cid-1", "cid-4"},
		},
	}

	Convey("check heartbeat load balance", t, func() {
		for _, test := range sequencialCases {
			Convey(test.desc, func() {
				for _, cid := range test.finishedCid {
					cidMu.Lock()
					cidTokenMap[cid] = FinishTag
					cidMu.Unlock()
				}
				state.BatchUpdateStatus(test.heartbeatChannels)

				channelGrace()
				state.BatchUpdateStatus(test.heartbeatChannels) //ask again
				time.Sleep(30 * time.Millisecond)               //heartbeat interval, wait async notify check done
				retChannels, err := state.BatchGetStatus(NewBatchGetStatusReq())
				So(err, ShouldBeNil)
				expects := toMap(test.expectChannels)
				for _, retCh := range retChannels {
					So(retCh, ShouldResemble, expects[retCh.GetChannelId()])
				}
				count := 0
				for _, conn := range state.channelConn {
					if !conn.Closed() {
						count++
					}
				}
				So(count, ShouldEqual, test.expectConnNum)
			})
		}
	})
}

func TestTunnelStateMachine_UpdateStatus(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	lg, _ := testLogConfig.Build()
	bypassApi := NewMocktunnelDataApi(mockCtrl)
	bypassApi.EXPECT().readRecords("tunnelId", "clientId", gomock.Any(), gomock.Any()).Return(nil, "token", "trace", 0, nil).AnyTimes()
	dialer := &channelDialer{
		api: bypassApi,
		lg:  lg,
		bc:  &DefaultBackoffConfig,
	}
	processor := &SimpleProcessFactory{
		CpInterval: time.Second,
		ProcessFunc: func(ctx *ChannelContext, r []*Record) error {
			if ctx.ChannelId == "cid-bad" {
				return errors.New("process failed")
			}
			time.Sleep(10 * time.Millisecond)
			return nil
		},
		ShutdownFunc: func(ctx *ChannelContext) {
			time.Sleep(5 * time.Millisecond)
		},
	}

	checkpointServer := mockServer()
	defer checkpointServer.Close()

	api := NewTunnelApi(checkpointServer.URL, "testInstance", "akid", "aksec", nil)

	state, _ := newStateWith4OpenChan(api, dialer, processor, lg)
	defer state.Close()
	Convey("test notify status", t, func() {
		Convey("update redundant channel", func() {
			c := newChannelWithCid("cidNotExist", 1, protocol.ChannelStatus_OPEN)
			state.UpdateStatus(ToChannelStatus(c))

			channels, err := state.BatchGetStatus(NewBatchGetStatusReq())
			So(err, ShouldBeNil)
			So(len(channels), ShouldEqual, 4)
		})

		Convey("update channel to high version", func() {
			c := newChannelWithCid("cid-1", 1, protocol.ChannelStatus_CLOSING)
			state.UpdateStatus(ToChannelStatus(c))

			channels, err := state.BatchGetStatus(NewBatchGetStatusReq())
			So(err, ShouldBeNil)
			So(len(channels), ShouldEqual, 4)
			So(toMap(channels)[c.GetChannelId()].GetVersion(), ShouldEqual, int64(1))
			So(toMap(channels)[c.GetChannelId()].GetStatus(), ShouldEqual, c.GetStatus())
		})

		Convey("update channel to low version", func() {
			c := newChannelWithCid("cid-1", 0, protocol.ChannelStatus_CLOSING)
			state.UpdateStatus(ToChannelStatus(c))

			channels, err := state.BatchGetStatus(NewBatchGetStatusReq())
			So(err, ShouldBeNil)
			So(len(channels), ShouldEqual, 4)
			So(toMap(channels)[c.GetChannelId()].GetVersion(), ShouldEqual, int64(1))
			So(toMap(channels)[c.GetChannelId()].GetStatus(), ShouldEqual, c.GetStatus())
		})

		Convey("update channel with closed conn", func() {
			c := newChannelWithCid("cid-1", 2, protocol.ChannelStatus_CLOSE)
			state.channelConn[c.GetChannelId()].Close()

			state.UpdateStatus(ToChannelStatus(c))

			channels, err := state.BatchGetStatus(NewBatchGetStatusReq())
			So(err, ShouldBeNil)
			So(len(channels), ShouldEqual, 4)
			So(toMap(channels)[c.GetChannelId()].GetVersion(), ShouldEqual, int64(2))
			So(toMap(channels)[c.GetChannelId()].GetStatus(), ShouldEqual, c.GetStatus())
		})
	})
}

func newStateWith4OpenChan(api *TunnelApi, dialer ChannelDialer, factory ChannelProcessorFactory, lg *zap.Logger) (*TunnelStateMachine, []*protocol.Channel) {
	channels := make([]*protocol.Channel, 4)
	for i := 0; i < 4; i++ {
		channels[i] = newChannelWithCid(fmt.Sprintf("cid-%d", i+1), 0, protocol.ChannelStatus_OPEN)
	}

	state := NewTunnelStateMachine("tunnelId", "clientId", dialer, factory, api, lg)
	state.BatchUpdateStatus(channels)
	channelGrace()
	return state, channels
}

func newChannelWithCid(cid string, version int64, status protocol.ChannelStatus) *protocol.Channel {
	return &protocol.Channel{
		ChannelId: &cid,
		Version:   &version,
		Status:    status.Enum(),
	}
}

func toMap(channels []*protocol.Channel) map[string]*protocol.Channel {
	m := make(map[string]*protocol.Channel)
	for _, c := range channels {
		m[c.GetChannelId()] = c
	}
	return m
}
