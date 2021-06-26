package tunnel

import (
	"errors"
	"github.com/aliyun/aliyun-tablestore-go-sdk/tunnel/protocol"
	"github.com/golang/mock/gomock"
	. "github.com/smartystreets/goconvey/convey"
	"go.uber.org/zap"
	"sync/atomic"
	"testing"
	"time"
)

var (
	cid = "test-channel-id-abc"

	testLogConfig = zap.Config{
		Level:       zap.NewAtomicLevelAt(zap.WarnLevel),
		Development: false,
		Sampling: &zap.SamplingConfig{
			Initial:    100,
			Thereafter: 100,
		},
		Encoding:      "json",
		EncoderConfig: zap.NewProductionEncoderConfig(),

		OutputPaths:      []string{"ut.log"},
		ErrorOutputPaths: []string{"ut.log"},
	}
)

func TestFailConn_NotifyStatus(t *testing.T) {
	cases := []struct {
		desc        string
		originState *protocol.Channel
		updateState *protocol.Channel
		expectState *protocol.Channel
	}{
		{"nil to open", nil, newChannel(0, protocol.ChannelStatus_OPEN),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
		{"nil to closing", nil, newChannel(0, protocol.ChannelStatus_CLOSING),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
		{"nil to closed", nil, newChannel(0, protocol.ChannelStatus_CLOSE),
			newChannel(0, protocol.ChannelStatus_CLOSE)},
		{"nil to terminated", nil, newChannel(0, protocol.ChannelStatus_TERMINATED),
			newChannel(0, protocol.ChannelStatus_TERMINATED)},
		{"close to closing", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(1, protocol.ChannelStatus_CLOSING),
			newChannel(2, protocol.ChannelStatus_CLOSE)},
		{"close to open", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(1, protocol.ChannelStatus_OPEN),
			newChannel(2, protocol.ChannelStatus_CLOSE)},
		{"close to close new version", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(2, protocol.ChannelStatus_CLOSE),
			newChannel(2, protocol.ChannelStatus_CLOSE)},
		{"close to close same version", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(1, protocol.ChannelStatus_CLOSE),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
		{"close to terminated", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(2, protocol.ChannelStatus_TERMINATED),
			newChannel(2, protocol.ChannelStatus_TERMINATED)},
		{"new version to old version", newChannel(1, protocol.ChannelStatus_CLOSE), newChannel(0, protocol.ChannelStatus_CLOSING),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
	}

	Convey("failConn notifyStatus with an empty state machine", t, func() {
		lg, _ := testLogConfig.Build()
		state := NewTunnelStateMachine("", "", nil, nil, nil, lg)

		for _, test := range cases {
			Convey("NotifyStatus case:"+test.desc, func() {
				failConn := &failConn{
					state:        state,
					currentState: ToChannelStatus(test.originState),
				}
				failConn.NotifyStatus(ToChannelStatus(test.updateState))
				So(failConn.currentState.ToPbChannel(), ShouldResemble, test.expectState)
				failConn.Close()
			})
		}
	})
}

func TestFailConn_Closed(t *testing.T) {
	Convey("init failConn Closed return false", t, func() {
		conn := new(failConn)
		So(conn.Closed(), ShouldBeFalse)
	})

	c := []*protocol.Channel{
		newChannel(1, protocol.ChannelStatus_CLOSING),
		newChannel(1, protocol.ChannelStatus_CLOSE),
		newChannel(1, protocol.ChannelStatus_OPEN),
		newChannel(1, protocol.ChannelStatus_TERMINATED),
	}
	Convey("failConn Closed return true", t, func() {
		lg, _ := testLogConfig.Build()
		state := NewTunnelStateMachine("", "", nil, nil, nil, lg)
		for _, channel := range c {
			conn := &failConn{state: state}
			conn.NotifyStatus(ToChannelStatus(channel))
			So(conn.Closed(), ShouldBeTrue)
		}
	})
}

func TestChannelConn_NotifyStatus(t *testing.T) {
	bc := &ChannelBackoffConfig{MaxDelay: time.Second}
	setDefault(bc)

	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	lg, _ := testLogConfig.Build()
	state := NewTunnelStateMachine("", "", nil, nil, nil, lg)
	bypassApi := NewMocktunnelDataApi(mockCtrl)
	bypassApi.EXPECT().readRecords("tunnelId", "clientId", "channelId", "token").Return(nil, "token", "traceId", 0, nil).AnyTimes()

	Convey("nil state channel notify status", t, func() {
		cases := []struct {
			updateState    *protocol.Channel
			expectState    *protocol.Channel
			expectChStatus int32
		}{
			{newChannel(0, protocol.ChannelStatus_CLOSE),
				newChannel(0, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(0, protocol.ChannelStatus_CLOSING),
				newChannel(1, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(0, protocol.ChannelStatus_OPEN),
				newChannel(0, protocol.ChannelStatus_OPEN), runningStatus},
			{newChannel(0, protocol.ChannelStatus_TERMINATED),
				newChannel(0, protocol.ChannelStatus_TERMINATED), closedStatus},
		}
		dialer := &channelDialer{
			api: bypassApi,
			lg:  lg,
			bc:  bc,
		}

		for _, test := range cases {
			Convey("from nil to status:"+test.updateState.String(), func() {
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(0)), state)
				conn.NotifyStatus(ToChannelStatus(test.updateState))
				cconn := conn.(*channelConn)
				So(cconn.getState(), ShouldResemble, test.expectState)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.expectChStatus)
				cconn.Close()
			})
		}
	})

	Convey("closed state channel notify status", t, func() {
		closedState := newChannel(1, protocol.ChannelStatus_CLOSE)
		cases := []struct {
			updateState    *protocol.Channel
			expectState    *protocol.Channel
			expectChStatus int32
		}{
			{newChannel(1, protocol.ChannelStatus_CLOSE),
				newChannel(1, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_CLOSING),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_OPEN),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_TERMINATED),
				newChannel(1, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(0, protocol.ChannelStatus_CLOSING),
				newChannel(1, protocol.ChannelStatus_CLOSE), closedStatus},
		}
		dialer := &channelDialer{
			api: nil,
			lg:  lg,
			bc:  bc,
		}

		for _, test := range cases {
			Convey("from closed to status:"+test.updateState.String(), func() {
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(0)), state)
				conn.NotifyStatus(ToChannelStatus(closedState))

				conn.NotifyStatus(ToChannelStatus(test.updateState))
				cconn := conn.(*channelConn)
				So(cconn.getState(), ShouldResemble, test.expectState)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.expectChStatus)
				conn.Close()
			})
		}
	})

	Convey("closing state channel notify status", t, func() {
		openState := newChannel(0, protocol.ChannelStatus_OPEN)
		closingState := newChannel(1, protocol.ChannelStatus_CLOSING)
		cases := []struct {
			updateState      *protocol.Channel
			stateAfterCheck  *protocol.Channel
			statusAfterCheck int32
		}{
			{newChannel(1, protocol.ChannelStatus_CLOSE),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_CLOSING),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_OPEN),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_TERMINATED),
				newChannel(2, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(0, protocol.ChannelStatus_CLOSING),
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
		}
		dialer := &channelDialer{
			api: bypassApi,
			lg:  lg,
			bc:  bc,
		}

		for _, test := range cases {
			Convey("from closing to status:"+test.updateState.String(), func() {
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(10*time.Millisecond)), state)
				conn.NotifyStatus(ToChannelStatus(openState))
				time.Sleep(10 * time.Millisecond)
				conn.NotifyStatus(ToChannelStatus(closingState))

				conn.NotifyStatus(ToChannelStatus(test.updateState))
				channelGrace()
				cconn := conn.(*channelConn)
				cconn.checkUpdateStatus()
				So(cconn.getState(), ShouldResemble, test.stateAfterCheck)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.statusAfterCheck)
				conn.Close()
			})
		}
	})

	Convey("open state channel notify status", t, func() {
		openState := newChannel(1, protocol.ChannelStatus_OPEN)
		cases := []struct {
			updateState    *protocol.Channel
			expectState    *protocol.Channel
			expectChStatus int32
			finalState     *protocol.Channel
			finalChStatus  int32
		}{
			{newChannel(1, protocol.ChannelStatus_CLOSE),
				newChannel(1, protocol.ChannelStatus_CLOSE), closedStatus,
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_CLOSING),
				newChannel(1, protocol.ChannelStatus_CLOSING), closingStatus,
				newChannel(2, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_OPEN),
				newChannel(1, protocol.ChannelStatus_OPEN), runningStatus,
				newChannel(1, protocol.ChannelStatus_OPEN), runningStatus},
			{newChannel(1, protocol.ChannelStatus_TERMINATED),
				newChannel(1, protocol.ChannelStatus_TERMINATED), closedStatus,
				newChannel(2, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(0, protocol.ChannelStatus_CLOSING),
				newChannel(1, protocol.ChannelStatus_OPEN), runningStatus,
				newChannel(1, protocol.ChannelStatus_OPEN), runningStatus},
		}
		dialer := &channelDialer{
			api: bypassApi,
			lg:  lg,
			bc:  bc,
		}

		for _, test := range cases {
			Convey("from open to status:"+test.updateState.String(), func() {
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(100*time.Millisecond)), state)
				conn.NotifyStatus(ToChannelStatus(openState))
				time.Sleep(10 * time.Millisecond)
				conn.NotifyStatus(ToChannelStatus(test.updateState))
				cconn := conn.(*channelConn)
				So(cconn.getState(), ShouldResemble, test.expectState)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.expectChStatus)
				channelGrace()
				cconn.checkUpdateStatus()
				So(cconn.getState(), ShouldResemble, test.finalState)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.finalChStatus)
				conn.Close()
			})
		}
	})

	Convey("terminated state channel notify status", t, func() {
		termState := newChannel(1, protocol.ChannelStatus_TERMINATED)
		cases := []struct {
			updateState    *protocol.Channel
			expectState    *protocol.Channel
			expectChStatus int32
		}{
			{newChannel(1, protocol.ChannelStatus_CLOSE),
				newChannel(1, protocol.ChannelStatus_CLOSE), closedStatus},
			{newChannel(1, protocol.ChannelStatus_CLOSING),
				newChannel(2, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(1, protocol.ChannelStatus_OPEN),
				newChannel(2, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(1, protocol.ChannelStatus_TERMINATED),
				newChannel(1, protocol.ChannelStatus_TERMINATED), closedStatus},
			{newChannel(0, protocol.ChannelStatus_CLOSING),
				newChannel(1, protocol.ChannelStatus_TERMINATED), closedStatus},
		}
		dialer := &channelDialer{
			api: nil,
			lg:  lg,
			bc:  bc,
		}

		for _, test := range cases {
			Convey("from closed to status:"+test.updateState.String(), func() {
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(0)), state)
				conn.NotifyStatus(ToChannelStatus(termState))
				conn.NotifyStatus(ToChannelStatus(test.updateState))
				cconn := conn.(*channelConn)
				So(cconn.getState(), ShouldResemble, test.expectState)
				So(atomic.LoadInt32(&cconn.status), ShouldEqual, test.expectChStatus)
				conn.Close()
			})
		}
	})
}

func TestChannelConn_NotifyStatus_ProcessRecords(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	lg, _ := testLogConfig.Build()
	state := NewTunnelStateMachine("", "", nil, nil, nil, lg)
	bypassApi := NewMocktunnelDataApi(mockCtrl)
	bypassApi.EXPECT().readRecords("tunnelId", "clientId", "channelId", "token").Return(nil, "token", "traceId", 0, nil).AnyTimes()

	failApi := NewMocktunnelDataApi(mockCtrl)
	failApi.EXPECT().readRecords("tunnelId", "clientId", "channelId", "token").Return(nil, "token", "traceId", 0, errors.New("abc")).Times(1)

	finishApi := NewMocktunnelDataApi(mockCtrl)
	finishApi.EXPECT().readRecords("tunnelId", "clientId", "channelId", "token").Return(nil, FinishTag, "traceId", 0, nil).Times(1)

	cases := []struct {
		desc        string
		api         tunnelDataApi
		processor   ChannelProcessor
		expectState *protocol.Channel
	}{
		{"tunnel read records failed", failApi, newTestProcessor(time.Duration(0)),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
		{"tunnel read records finished", finishApi,
			newTestProcessor(time.Duration(0)), newChannel(1, protocol.ChannelStatus_TERMINATED)},
		{"tunnel processor process records failed", bypassApi, new(failProcessor),
			newChannel(1, protocol.ChannelStatus_CLOSE)},
	}

	Convey("open channel read records", t, func() {
		for _, test := range cases {
			Convey("read records case: "+test.desc, func() {
				openState := newChannel(0, protocol.ChannelStatus_OPEN)
				dialer := &channelDialer{
					api: test.api,
					lg:  lg,
					bc:  &DefaultBackoffConfig,
				}
				conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", test.processor, state)
				conn.NotifyStatus(ToChannelStatus(openState))

				time.Sleep(30 * time.Millisecond)
				conn.NotifyStatus(ToChannelStatus(openState))
				cconn := conn.(*channelConn)
				So(cconn.getState(), ShouldResemble, test.expectState)
				conn.Close()
			})
		}
	})
}

func TestChannelConn_Close(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	lg, _ := testLogConfig.Build()
	state := NewTunnelStateMachine("", "", nil, nil, nil, lg)
	bypassApi := NewMocktunnelDataApi(mockCtrl)
	bypassApi.EXPECT().readRecords("tunnelId", "clientId", "channelId", "token").Return(nil, "token", "traceId", 0, nil).AnyTimes()

	Convey("open tunnel is closed", t, func() {
		openState := newChannel(0, protocol.ChannelStatus_OPEN)
		dialer := &channelDialer{
			api: bypassApi,
			lg:  lg,
			bc:  &DefaultBackoffConfig,
		}
		conn := dialer.ChannelDial("tunnelId", "clientId", "channelId", "token", newTestProcessor(time.Duration(0)), state)
		conn.NotifyStatus(ToChannelStatus(openState))

		time.Sleep(time.Second)
		conn.Close()
		So(conn.Closed(), ShouldBeTrue)
	})
}

func newChannel(v int64, status protocol.ChannelStatus) *protocol.Channel {
	return &protocol.Channel{
		ChannelId: &cid,
		Version:   &v,
		Status:    status.Enum(),
	}
}

type testProcessor struct {
	sleepDur time.Duration
}

func newTestProcessor(dur time.Duration) *testProcessor {
	return &testProcessor{dur}
}

func (p *testProcessor) Process(records []*Record, nextToken, traceId string) error {
	return nil
}

func (p *testProcessor) Shutdown() {
	time.Sleep(p.sleepDur)
}

type failProcessor struct{}

func (p *failProcessor) Process(records []*Record, nextToken, traceId string) error {
	return errors.New("failed")
}

func (p *failProcessor) Shutdown() {}

func (c *channelConn) getState() *protocol.Channel {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.currentState.ToPbChannel()
}

func channelGrace() {
	time.Sleep(2 * time.Second)
}
