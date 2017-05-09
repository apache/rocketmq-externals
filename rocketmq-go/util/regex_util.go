package util

import (
	"regexp"
)

//var regexpMap map[string]*regexp.Regexp
//var rwMutex sync.RWMutex

// todo improve
func MatchString(value, pattern string) bool {
	//rwMutex.RLock()
	//re := regexpMap[pattern]
	re, err := regexp.Compile(pattern)
	if err != nil {
		return false
	}
	//if ( re == nil) {
	//	rwMutex.Lock()
	//	var err error
	//	re, err = regexp.Compile(pattern)
	//	if err != nil {
	//		return false
	//	}
	//	regexpMap[pattern] = re
	//	rwMutex.Unlock()
	//}
	//rwMutex.RUnlock()
	return re.MatchString(value)
}
