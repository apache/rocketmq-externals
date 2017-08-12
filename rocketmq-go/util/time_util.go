package util

import (
	"time"
	"strconv"
)

func CurrentTimeMillisInt64() (ret int64) {
	ret = time.Now().UnixNano() / 1000000
	return
}
func CurrentTimeMillisStr() (ret string) {
	ret = strconv.FormatInt(CurrentTimeMillisInt64(), 10)
	return
}
