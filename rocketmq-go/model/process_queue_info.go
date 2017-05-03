package model


type ProcessQueueInfo struct {
	CommitOffset            int64 `json:"commitOffset"`

	CachedMsgMinOffset      int64 `json:"cachedMsgMinOffset"`
	CachedMsgMaxOffset      int64 `json:"cachedMsgMaxOffset"`
	CachedMsgCount          int32 `json:"cachedMsgCount"`

	TransactionMsgMinOffset int64 `json:"transactionMsgMinOffset"`
	TransactionMsgMaxOffset int64 `json:"transactionMsgMaxOffset"`
	TransactionMsgCount     int32 `json:"transactionMsgCount"`

	Locked                  bool `json:"locked"`
	TryUnlockTimes          int64 `json:"tryUnlockTimes"`
	LastLockTimestamp       int64 `json:"lastLockTimestamp"`

	Droped                  bool `json:"droped"`
	LastPullTimestamp       int64 `json:"lastPullTimestamp"`
	LastConsumeTimestamp    int64 `json:"lastConsumeTimestamp"`
}
//func (self ProcessQueueInfo) BuildFromProcessQueue(processQueue ProcessQueue) (processQueueInfo ProcessQueueInfo) {
//	processQueueInfo = ProcessQueueInfo{}
//	//processQueueInfo.CommitOffset =
//	processQueueInfo.CachedMsgCount = processQueue.GetMsgCount()
//	processQueueInfo.CachedMsgCount
//	return
//}
