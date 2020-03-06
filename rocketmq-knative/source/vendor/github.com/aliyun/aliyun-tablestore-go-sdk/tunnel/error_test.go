package tunnel

import (
	. "github.com/smartystreets/goconvey/convey"
	"testing"
)

func TestTunnelError_Temporary(t *testing.T) {
	Convey("Test tunnelError is temporary or not", t, func() {
		cases := []struct {
			err    *TunnelError
			expect bool
		}{
			{new(TunnelError), false},
			{&TunnelError{Code: ErrCodeParamInvalid}, false},
			{&TunnelError{Code: ErrCodeResourceGone}, false},
			{&TunnelError{Code: ErrCodeServerUnavailable}, true},
			{&TunnelError{Code: ErrCodeSequenceNotMatch}, false},
			{&TunnelError{Code: ErrCodeClientError}, false},
			{&TunnelError{Code: ErrCodeTunnelExpired}, false},
			{&TunnelError{Code: ErrCodePermissionDenied}, false},
			{&TunnelError{Code: ErrCodeTunnelExist}, false},
		}

		for _, test := range cases {
			Convey("Test error: "+test.err.Error(), func() {
				ret := test.err.Temporary()
				So(ret, ShouldEqual, test.expect)
			})
		}
	})
}
