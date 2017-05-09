package util

import (
	"strings"
)

//char 1 and 2 from java code
var NAME_VALUE_SEPARATOR = string(rune(1))

var PROPERTY_SEPARATOR = string(rune(2))

func MessageProperties2String(propertiesMap map[string]string) (ret string) {
	for key, value := range propertiesMap {
		ret = ret + key + NAME_VALUE_SEPARATOR + value + PROPERTY_SEPARATOR
	}
	return
}

func String2MessageProperties(properties string) (ret map[string]string) {
	ret = make(map[string]string)
	for _, nameValueStr := range strings.Split(properties, PROPERTY_SEPARATOR) {
		nameValuePair := strings.Split(nameValueStr, NAME_VALUE_SEPARATOR)
		nameValueLen := len(nameValuePair)
		if nameValueLen != 2 {
			//glog.Error("nameValuePair is error", nameValueStr)
			continue
		}
		ret[nameValuePair[0]] = nameValuePair[1]
	}
	return
}
