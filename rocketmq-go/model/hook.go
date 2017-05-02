package model

type ConsumerHook interface {
	Name() string
	DoBefore()
	DoAfter()
}

type FilterMessageHook interface {
	Name() string
	DoBefore()
	DoAfter()
}