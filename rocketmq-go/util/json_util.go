/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package util

import (
	"errors"
)

//TOKEN_TYPE json string's token type
type TOKEN_TYPE byte

const (
	//STRING string
	STRING TOKEN_TYPE = iota
	//NUMBER number
	NUMBER
	//START_OBJ start object
	START_OBJ
	//END_OBJ end object
	END_OBJ
	//COMMA comma
	COMMA
	//COLON colon
	COLON
)

type token struct {
	tokenType  TOKEN_TYPE
	tokenValue string
}

////{"offsetTable":{{"brokerName":"broker-b","queueId":122222,"topic":"GoLang"}:9420,{"brokerName":"broker-b","queueId":2,"topic":"GoLang"}:9184,{"brokerName":"broker-b","queueId":1,"topic":"GoLang"}:9260,{"brokerName":"broker-b","queueId":3,"topic":"GoLang"}:9139}}

type parseInfo struct {
	startObjCount int
	// 0 begin 1 key 2 value
	readType int
	nowKey   string
	nowValue string
}

//GetKvStringMap convert json string to map[string]string
func GetKvStringMap(str string) (kvMap map[string]string, err error) {
	var tokenList []token
	tokenList, err = parseTokenList(str)
	kvMap = map[string]string{}
	currentParseInfo := &parseInfo{
		startObjCount: 0,
		readType:      0,
		nowKey:        "",
		nowValue:      "",
	}
	for i := 0; i < len(tokenList); i++ {
		nowToken := &tokenList[i]
		if nowToken.tokenType == START_OBJ {
			currentParseInfo.startObjCount++
		}
		if nowToken.tokenType == END_OBJ {
			currentParseInfo.startObjCount--
		}
		if currentParseInfo.readType == 0 {
			if nowToken.tokenType != START_OBJ {
				err = errors.New("json not start with {")
				return
			}
			currentParseInfo.readType = 1
		} else if currentParseInfo.readType == 1 {
			parseKey(currentParseInfo, nowToken)
		} else if currentParseInfo.readType == 2 {
			var k, v string
			k, v = parseValue(currentParseInfo, nowToken)
			if len(k) > 0 {
				kvMap[k] = v
			}
		} else {
			err = errors.New("this is a bug")
			return
		}
	}
	if len(currentParseInfo.nowKey) > 0 {
		kvMap[currentParseInfo.nowKey] = currentParseInfo.nowValue
	}
	return
}
func parseValue(info *parseInfo, nowToken *token) (key, value string) {
	if nowToken.tokenType == COMMA { // , split kv pair
		if info.startObjCount == 1 {
			key = info.nowKey
			value = info.nowValue
			info.nowKey = ""
			info.nowValue = ""
			info.readType = 1
			return
		}
	}
	if nowToken.tokenType == STRING {
		info.nowValue = info.nowValue + "\"" + nowToken.tokenValue + "\""

	} else {
		if info.startObjCount > 0 { //use less end }
			info.nowValue = info.nowValue + nowToken.tokenValue
		}
	}
	return
}
func parseKey(info *parseInfo, nowToken *token) {
	if nowToken.tokenType == COLON { //: split k and v
		if info.startObjCount == 1 {
			info.readType = 2
			return
		}
	}
	if nowToken.tokenType == STRING {
		info.nowKey = info.nowKey + "\"" + nowToken.tokenValue + "\""
	} else {
		info.nowKey = info.nowKey + nowToken.tokenValue
	}
	return
}

func parseTokenList(str string) (tokenList []token, err error) {

	for i := 0; i < len(str); i++ {
		c := str[i]
		token := token{}
		switch c {
		case '{':
			token.tokenType = START_OBJ
			token.tokenValue = string(c)
			break
		case '}':
			token.tokenType = END_OBJ
			token.tokenValue = string(c)
			break
		case ',':
			token.tokenType = COMMA
			token.tokenValue = string(c)
			break
		case ':':
			token.tokenType = COLON
			token.tokenValue = string(c)
			break
		case '"':
			token.tokenType = STRING
			token.tokenValue = ""
			for i++; str[i] != '"'; i++ {
				token.tokenValue = token.tokenValue + string(str[i])
			}
			break
		default:
			if c == '-' || (str[i] <= '9' && str[i] >= '0') {
				token.tokenType = NUMBER
				token.tokenValue = string(c)
				for i++; str[i] <= '9' && str[i] >= '0'; i++ {
					token.tokenValue = token.tokenValue + string(str[i])
				}
				i--
				break
			}
			err = errors.New("INVALID JSON")
			return
		}
		tokenList = append(tokenList, token)
	}
	return
}
