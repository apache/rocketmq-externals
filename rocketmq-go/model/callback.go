package model

type PullCallback interface {
	OnSuccess(pr PullResult)
	OnError(err error)
}

type DefaultPullCallBack struct {}

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

type SendCallback interface {
	OnSuccess(pr SendResult)
	OnError(err error)
}