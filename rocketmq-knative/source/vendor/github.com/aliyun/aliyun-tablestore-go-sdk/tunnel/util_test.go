package tunnel

import (
	"github.com/smartystreets/goconvey/convey"
	"testing"
)

func TestStreamRecordSequenceLess(t *testing.T) {
	convey.Convey("compare stream record sequence", t, func() {
		cases := []struct {
			A      *SequenceInfo
			B      *SequenceInfo
			Expect bool
		}{
			{
				&SequenceInfo{0, 100, 0},
				&SequenceInfo{0, 100, 0},
				false,
			},
			{
				&SequenceInfo{0, 100, 0},
				&SequenceInfo{1, 0, 0},
				true,
			},
			{
				&SequenceInfo{0, 100, 1},
				&SequenceInfo{0, 101, 0},
				true,
			},
			{
				&SequenceInfo{1, 100, 0},
				&SequenceInfo{1, 100, 1},
				true,
			},
			{
				&SequenceInfo{1, 0, 0},
				&SequenceInfo{0, 100, 1},
				false,
			},
			{
				&SequenceInfo{1, 101, 0},
				&SequenceInfo{1, 100, 1},
				false,
			},
			{
				&SequenceInfo{1, 100, 2},
				&SequenceInfo{1, 100, 1},
				false,
			},
		}

		for _, cas := range cases {
			ret := StreamRecordSequenceLess(cas.A, cas.B)
			convey.So(ret, convey.ShouldEqual, cas.Expect)
		}
	})
}
