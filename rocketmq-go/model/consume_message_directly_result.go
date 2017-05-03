package model

type ConsumeMessageDirectlyResult struct {
	Order          bool `json:"order"`
	AutoCommit     bool `json:"autoCommit"`
	//CR_SUCCESS,
	//CR_LATER,
	//CR_ROLLBACK,
	//CR_COMMIT,
	//CR_THROW_EXCEPTION,
	//CR_RETURN_NULL,
	ConsumeResult  string `json:"consumeResult"`
	Remark         string `json:"remark"`
	SpentTimeMills int64 `json:"spentTimeMills"`
}
