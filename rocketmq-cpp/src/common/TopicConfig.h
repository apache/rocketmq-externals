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
#ifndef __TOPICCONFIG_H__
#define __TOPICCONFIG_H__

#include <string>
#include "TopicFilterType.h"
#include "UtilAll.h"
namespace rocketmq {
//<!************************************************************************
class TopicConfig {
 public:
  TopicConfig();
  TopicConfig(const string& topicName);
  TopicConfig(const string& topicName, int readQueueNums, int writeQueueNums,
              int perm);
  ~TopicConfig();

  string encode();
  bool decode(const string& in);
  const string& getTopicName();
  void setTopicName(const string& topicName);
  int getReadQueueNums();
  void setReadQueueNums(int readQueueNums);
  int getWriteQueueNums();
  void setWriteQueueNums(int writeQueueNums);
  int getPerm();
  void setPerm(int perm);
  TopicFilterType getTopicFilterType();
  void setTopicFilterType(TopicFilterType topicFilterType);

 public:
  static int DefaultReadQueueNums;
  static int DefaultWriteQueueNums;

 private:
  static string SEPARATOR;

  string m_topicName;
  int m_readQueueNums;
  int m_writeQueueNums;
  int m_perm;
  TopicFilterType m_topicFilterType;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
