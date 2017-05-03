package constant

const (
	CompressedFlag int32 = (0x1 << 0)
	MultiTagsFlag int32 = (0x1 << 1)
	TransactionNotType int32 = (0x0 << 2)
	TransactionPreparedType int32 = (0x1 << 2)
	TransactionCommitType int32 = (0x2 << 2)
	TransactionRollbackType int32 = (0x3 << 2)
)
