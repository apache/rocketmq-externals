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

package model

//ConsumeResultType message consume result type
type ConsumeResultType string

const (
	//CR_SUCCESS consume success
	CR_SUCCESS ConsumeResultType = "CR_SUCCESS"

	//CR_THROW_EXCEPTION consume error
	CR_THROW_EXCEPTION ConsumeResultType = "CR_THROW_EXCEPTION"
	//CR_RETURN_NULL     ConsumeResultType = "CR_RETURN_NULL"
	//CR_LATER           ConsumeResultType = "CR_LATER"
	//CR_ROLLBACK        ConsumeResultType = "CR_ROLLBACK"
	//CR_COMMIT          ConsumeResultType = "CR_COMMIT"
)

//ConsumeMessageDirectlyResult consume message directly's result
type ConsumeMessageDirectlyResult struct {
	Order          bool              `json:"order"`
	AutoCommit     bool              `json:"autoCommit"`
	ConsumeResult  ConsumeResultType `json:"consumeResult"`
	Remark         string            `json:"remark"`
	SpentTimeMills int64             `json:"spentTimeMills"`
}
