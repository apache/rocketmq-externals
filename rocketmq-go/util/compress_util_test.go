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
	"testing"
)

func TestCompressWithLevel(t *testing.T) {
	bytes := []byte{120, 156, 202, 72, 205, 201, 201, 215, 81, 40, 207,
		47, 202, 73, 225, 2, 4, 0, 0, 255, 255, 33, 231, 4, 147,
		47, 202, 73, 225, 2, 4, 0, 0, 255, 255, 33, 231, 4, 147,
		47, 202, 73, 225, 2, 4, 0, 0, 255, 255, 33, 231, 4, 147,
		47, 202, 73, 225, 2, 4, 0, 0, 255, 255, 33, 231, 4, 147,
		47, 202, 73, 225, 2, 4, 0, 0, 255, 255, 33, 231, 4, 147}
	body1, error1 := CompressWithLevel(bytes, 5)
	if len(body1) > len(bytes) && error1 != nil {
		t.Errorf("CompressWithLevel failed : cannot transfer byte[]")
	}
	body2, error2 := CompressWithLevel(nil, 10)
	if body2 != nil && error2 != nil {
		t.Errorf("CompressWithLevel failed : cannot handle error")
	}
}
