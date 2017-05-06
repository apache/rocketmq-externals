/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package util

import (
	"errors"
)

const (
	STRING = "STRING"
	NUMBER = "NUMBER"

	START_OBJ = "START_OBJ" //{
	END_OBJ   = "END_OBJ"   //}
	COMMA     = "COMMA"     //,
	COLON     = "COLON"     //:

	//// may be next version impl it
	//BOOL
	//NULL
	//START_ARRAY //[
	//END_ARRAY //]
	//EOF
)

type Token struct {
	tokenType  string
	tokenValue string
}

////{"offsetTable":{{"brokerName":"broker-b","queueId":122222,"topic":"GoLang"}:9420,{"brokerName":"broker-b","queueId":2,"topic":"GoLang"}:9184,{"brokerName":"broker-b","queueId":1,"topic":"GoLang"}:9260,{"brokerName":"broker-b","queueId":3,"topic":"GoLang"}:9139}}

func GetKvStringMap(str string) (kvMap map[string]string, err error) {
	var tokenList []Token
	tokenList, err = parseTokenList(str)
	kvMap = map[string]string{}
	startObjCount := 0
	readType := 0 // 0 begin 1 key 2 value
	nowKey := ""
	nowValue := ""
	for i := 0; i < len(tokenList); i++ {
		nowToken := tokenList[i]
		if nowToken.tokenType == START_OBJ {
			startObjCount++
		}
		if nowToken.tokenType == END_OBJ {
			startObjCount--
		}
		if readType == 0 {
			if nowToken.tokenType != START_OBJ {
				err = errors.New("json not start with {")
				return
			}
			readType = 1
		} else if readType == 1 {
			if nowToken.tokenType == COLON { //: split k and v
				if startObjCount == 1 {
					readType = 2
					continue
				}
			}
			if nowToken.tokenType == STRING {
				nowKey = nowKey + "\"" + nowToken.tokenValue + "\""
			} else {
				nowKey = nowKey + nowToken.tokenValue
			}
		} else if readType == 2 {
			if nowToken.tokenType == COMMA { // , split kv pair
				if startObjCount == 1 {
					kvMap[nowKey] = nowValue
					nowKey = ""
					nowValue = ""
					readType = 1
					continue
				}
			}
			if nowToken.tokenType == STRING {
				nowValue = nowValue + "\"" + nowToken.tokenValue + "\""

			} else {
				if startObjCount > 0 { //use less end }
					nowValue = nowValue + nowToken.tokenValue
				}
			}

		} else {
			err = errors.New("this is a bug")
			return
		}
	}
	if len(nowKey) > 0 {

		kvMap[nowKey] = nowValue
	}
	return
}

func parseTokenList(str string) (tokenList []Token, err error) {

	for i := 0; i < len(str); i++ {
		c := str[i]
		token := Token{}
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
