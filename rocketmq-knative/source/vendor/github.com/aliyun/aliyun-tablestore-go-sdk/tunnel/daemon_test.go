package tunnel

import (
	"errors"
	"fmt"
	"github.com/golang/mock/gomock"
	"github.com/smartystreets/goconvey/convey"
	"sync/atomic"
	"testing"
	"time"
)

func TestTunnelWorkerDaemon_Run(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	convey.Convey("daemon new tunnel worker failed", t, func() {
		tunnelApi := NewMockTunnelMetaApi(ctrl)
		tunnelApi.EXPECT().NewTunnelWorker("tunnelId", nil).Return(nil, errors.New("config is nil")).Times(1)

		d := NewTunnelDaemon(tunnelApi, "tunnelId", nil)
		err := d.Run()
		convey.So(err.Error(), convey.ShouldEqual, "config is nil")
	})

	convey.Convey("daemon run tunnel not exist", t, func() {
		mockWorker := NewMockTunnelWorker(ctrl)
		tunnelApi := NewMockTunnelMetaApi(ctrl)
		tunnelApi.EXPECT().NewTunnelWorker("tunnelId", gomock.Any()).Return(mockWorker, nil).Times(1)
		terr := &TunnelError{Code: ErrCodeParamInvalid, Message: "tunnel id not exist"}
		mockWorker.EXPECT().ConnectAndWorking().Return(terr).Times(1)
		mockWorker.EXPECT().Shutdown().Times(1)

		d := NewTunnelDaemon(tunnelApi, "tunnelId", nil)
		err := d.Run()
		convey.So(err.Error(), convey.ShouldEqual, terr.Error())
	})

	convey.Convey("daemon run tunnel permission denied", t, func() {
		mockWorker := NewMockTunnelWorker(ctrl)
		tunnelApi := NewMockTunnelMetaApi(ctrl)
		tunnelApi.EXPECT().NewTunnelWorker("tunnelId", gomock.Any()).Return(mockWorker, nil).Times(1)
		terr := &TunnelError{Code: ErrCodePermissionDenied, Message: "not authorised to perform this action"}
		mockWorker.EXPECT().ConnectAndWorking().Return(terr).Times(1)
		mockWorker.EXPECT().Shutdown().Times(1)

		d := NewTunnelDaemon(tunnelApi, "tunnelId", nil)
		err := d.Run()
		convey.So(err.Error(), convey.ShouldEqual, terr.Error())
	})

	convey.Convey("daemon run tunnel other error", t, func(c convey.C) {
		mockWorker := NewMockTunnelWorker(ctrl)
		tunnelApi := NewMockTunnelMetaApi(ctrl)
		tunnelApi.EXPECT().NewTunnelWorker("tunnelId", gomock.Any()).Return(mockWorker, nil).MinTimes(2)
		terr := &TunnelError{Code: ErrCodeResourceGone, Message: "heartbeat timeout"}
		mockWorker.EXPECT().ConnectAndWorking().Return(terr).MinTimes(2)
		mockWorker.EXPECT().Shutdown().MinTimes(2)

		d := NewTunnelDaemon(tunnelApi, "tunnelId", new(TunnelWorkerConfig))
		var flag int32
		go func() {
			err := d.Run()
			c.So(err, convey.ShouldBeNil)
			atomic.StoreInt32(&flag, 1)
		}()

		time.Sleep(time.Duration(DaemonRandomRetryMs+1000) * time.Millisecond)
		d.Close()
		time.Sleep(time.Duration(DaemonRandomRetryMs+1000) * time.Millisecond)
		convey.So(atomic.LoadInt32(&flag), convey.ShouldEqual, int32(1))
	})

	convey.Convey("daemon run until close", t, func(c convey.C) {
		mockWorker := NewMockTunnelWorker(ctrl)
		tunnelApi := NewMockTunnelMetaApi(ctrl)
		tunnelApi.EXPECT().NewTunnelWorker("tunnelId", gomock.Any()).Return(mockWorker, nil).Times(1)
		closed := new(atomic.Value)
		closed.Store(false)
		mockWorker.EXPECT().ConnectAndWorking().DoAndReturn(func() error {
			for !closed.Load().(bool) {
				fmt.Println(closed.Load().(bool))
				time.Sleep(2 * time.Millisecond)
			}
			fmt.Println("return nil")
			return errors.New("shutdown")
		}).Times(1)
		mockWorker.EXPECT().Shutdown().DoAndReturn(func() {
			fmt.Println("closed")
			closed.Store(true)
		}).Times(2)

		d := NewTunnelDaemon(tunnelApi, "tunnelId", new(TunnelWorkerConfig))
		var flag int32
		go func() {
			fmt.Println("Run")
			err := d.Run()
			fmt.Println("Run", err)
			c.So(err, convey.ShouldBeNil)
			atomic.StoreInt32(&flag, 1)
		}()

		time.Sleep(10 * time.Millisecond)
		d.Close()
		time.Sleep(10 * time.Millisecond)
		convey.So(atomic.LoadInt32(&flag), convey.ShouldEqual, int32(1))
	})
}
