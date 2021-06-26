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

import (
	"testing"

	. "github.com/smartystreets/goconvey/convey"
	"github.com/stretchr/testify/assert"
)

func TestProperties(t *testing.T) {
	msg1 := NewMessage("test", nil)
	msg1.properties = map[string]string{
		"k1": "v1",
		"k2": "v2",
	}
	str := msg1.MarshallProperties()
	msg2 := NewMessage("test", nil)
	msg2.UnmarshalProperties([]byte(str))
	assert.Equal(t, msg1.properties, msg2.properties)
}

func TestCreateMessageId(t *testing.T) {
	Convey("MessageId gen", t, func() {
		b := []byte{10, 93, 233, 58}
		port := int32(10911)
		offset := int64(4391252)
		id := createMessageId(b, port, offset)

		Convey("generated messageId should be equal to expected", func() {
			assert.Equal(t, "0A5DE93A00002A9F0000000000430154", id)
		})
	})

}

func TestGetProperties(t *testing.T) {
	msg1 := NewMessage("test", nil)
	msg1.properties = map[string]string{
		"k1": "v1",
		"k2": "v2",
	}
	assert.Equal(t, msg1.GetProperties(), map[string]string{
		"k1": "v1",
		"k2": "v2",
	})
}
