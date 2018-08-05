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

package message

import (
	"testing"
)

func TestGeneratorMessageClientId(t *testing.T) {
	id1 := generatorMessageClientId()
	if id1 == "" {
		t.Errorf("TestGeneratorMessageClientId failed")
	}
	id2 := generatorMessageClientId()
	if id2 == "" || id1 == id2 {
		t.Errorf("TestGeneratorMessageClientId failed : create same clientId")
	}
}

func TestGeneratorMessageOffsetId(t *testing.T) {
	id := GeneratorMessageOffsetId([]byte{111}, 1, 1)
	if id != "6F000000010000000000000001" {
		t.Errorf("TestGeneratorMessageOffsetId failed")
	}
}
