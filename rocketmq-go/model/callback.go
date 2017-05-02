package model

type PullCallback interface {
	OnSuccess(pr PullResult)
	OnError(err error)
}

// todo optimize
type PullCallBack int

// TODO
func (pcb PullCallBack) OnSuccess(pr *PullResult) {
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

func (pcb PullCallBack) OnError(err error) {

}

type SendCallback interface {
	OnSuccess(pr SendResult)
	OnError(err error)
}