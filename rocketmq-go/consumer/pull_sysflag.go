package consumer

const (
	FlagCommitOffset int = 0x1 << 0
	FlagSuspend int = 0x1 << 1
	FlagSubscription int = 0x1 << 2
	FlagClassFilter int = 0x1 << 3
)

func BuildSysFlag(commitOffset, suspend, subsription, classFillter bool) int {
	var flag int = 0
	if commitOffset {
		flag |= FlagCommitOffset
	}

	if suspend {
		flag |= FlagSuspend
	}

	if subsription {
		flag |= FlagSubscription
	}

	if classFillter {
		flag |= FlagClassFilter
	}

	return flag
}

func ClearCommitOffsetFlag(sysFlag int) int {
	return sysFlag & (^FlagCommitOffset)
}

func HasClassFilterFlag(sysFlag int) bool {
	return (sysFlag & FlagClassFilter) == FlagClassFilter
}