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
#include "VirtualEnvUtil.h"
#include <stdio.h>
#include <stdlib.h>
#include "UtilAll.h"

namespace rocketmq {
const char* VirtualEnvUtil::VIRTUAL_APPGROUP_PREFIX = "%%PROJECT_%s%%";

//<!***************************************************************************
string VirtualEnvUtil::buildWithProjectGroup(const string& origin,
                                             const string& projectGroup) {
  if (!UtilAll::isBlank(projectGroup)) {
    char prefix[1024];
    sprintf(prefix, VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());

    if (origin.find_last_of(prefix) == string::npos) {
      return origin + prefix;
    } else {
      return origin;
    }
  } else {
    return origin;
  }
}

string VirtualEnvUtil::clearProjectGroup(const string& origin,
                                         const string& projectGroup) {
  char prefix[1024];
  sprintf(prefix, VIRTUAL_APPGROUP_PREFIX, projectGroup.c_str());
  string::size_type pos = origin.find_last_of(prefix);

  if (!UtilAll::isBlank(prefix) && pos != string::npos) {
    return origin.substr(0, pos);
  } else {
    return origin;
  }
}

//<!***************************************************************************
}  //<!end namespace;
