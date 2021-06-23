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

package primitive

import "testing"

func TestMessageID(t *testing.T) {
	id := []byte("0AAF0895000078BF000000000009BB4A")
	msgID, err := UnmarshalMsgID(id)
	if err != nil {
		t.Fatalf("unmarshal msg id error, ms is: %s", err.Error())
	}
	if msgID.Addr != "10.175.8.149" {
		t.Fatalf("parse messageID %s error", id)
	}
	if msgID.Port != 30911 {
		t.Fatalf("parse messageID %s error", id)
	}
	if msgID.Offset != 637770 {
		t.Fatalf("parse messageID %s error", id)
	}
	t.Log(msgID)
}
