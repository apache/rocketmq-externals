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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "ClientRPCHook.h"
#include "CommandHeader.h"
#include "Logging.h"
extern "C" {
#include "spas_client.h"
}
#include "string"

namespace rocketmq {

const string SessionCredentials::AccessKey = "AccessKey";
const string SessionCredentials::SecretKey = "SecretKey";
const string SessionCredentials::Signature = "Signature";
const string SessionCredentials::SignatureMethod = "SignatureMethod";
const string SessionCredentials::ONSChannelKey = "OnsChannel";

void ClientRPCHook::doBeforeRequest(const string& remoteAddr,
                                    RemotingCommand& request) {
  CommandHeader* header = request.getCommandHeader();

  map<string, string> requestMap;
  string totalMsg;

  requestMap.insert(pair<string, string>(SessionCredentials::AccessKey,
                                         sessionCredentials.getAccessKey()));
  requestMap.insert(pair<string, string>(SessionCredentials::ONSChannelKey,
                                         sessionCredentials.getAuthChannel()));

  LOG_DEBUG("before insert declared filed,MAP SIZE is:" SIZET_FMT "",
            requestMap.size());
  if (header != NULL) {
    header->SetDeclaredFieldOfCommandHeader(requestMap);
  }
  LOG_DEBUG("after insert declared filed, MAP SIZE is:" SIZET_FMT "",
            requestMap.size());

  map<string, string>::iterator it = requestMap.begin();
  for (; it != requestMap.end(); ++it) {
    totalMsg.append(it->second);
  }
  if (request.getMsgBody().length() > 0) {
    LOG_DEBUG("msgBody is:%s, msgBody length is:" SIZET_FMT "",
              request.getMsgBody().c_str(), request.getMsgBody().length());

    totalMsg.append(request.getMsgBody());
  }
  LOG_DEBUG("total msg info are:%s, size is:" SIZET_FMT "", totalMsg.c_str(),
            totalMsg.size());
  char* pSignature =
      rocketmqSignature::spas_sign(totalMsg.c_str(), totalMsg.size(),
                                sessionCredentials.getSecretKey().c_str());
  // char *pSignature = spas_sign(totalMsg.c_str(),
  // sessionCredentials.getSecretKey().c_str());

  if (pSignature != NULL) {
    string signature(static_cast<const char*>(pSignature));
    request.addExtField(SessionCredentials::Signature, signature);
    request.addExtField(SessionCredentials::AccessKey,
                        sessionCredentials.getAccessKey());
    request.addExtField(SessionCredentials::ONSChannelKey,
                        sessionCredentials.getAuthChannel());
    rocketmqSignature::spas_mem_free(pSignature);
  } else {
    LOG_ERROR("signature for request failed");
  }
}
}
