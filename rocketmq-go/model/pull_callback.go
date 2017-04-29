package model

type PullCallback interface {
	OnSuccess(pr PullResult)
	OnError(err error)
}

// todo optimize
type DefaultPullCallBack int

// TODO
func (pcb DefaultPullCallBack) OnSuccess(pr *PullResult) {
	if pr == nil {
		return
	}
	switch pr.PullStatus() {
	case Found:
	case NoNewMsg:
	case NoMatchedMsg:
	case OffsetIllegal:
	}
}

func (pcb DefaultPullCallBack) OnError(err error) {

}
