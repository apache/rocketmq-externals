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
package remoting

type DefalutRemotingClient struct {
}

func (self *DefalutRemotingClient) InvokeSync(addr string, request *RemotingCommand, timeoutMillis int64) (remotingCommand *RemotingCommand, err error) {
	return
}
func (self *DefalutRemotingClient)InvokeAsync(addr string, request *RemotingCommand, timeoutMillis int64, invokeCallback InvokeCallback) error {
	return
}
func (self *DefalutRemotingClient)InvokeOneWay(addr string, request *RemotingCommand, timeoutMillis int64) error {
	return
}
