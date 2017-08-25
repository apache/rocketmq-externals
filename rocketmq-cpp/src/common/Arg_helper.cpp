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

#include "Arg_helper.h"
#include "UtilAll.h"

namespace rocketmq {
//<!***************************************************************************
Arg_helper::Arg_helper(int argc, char* argv[]) {
  for (int i = 0; i < argc; i++) {
    m_args.push_back(argv[i]);
  }
}

Arg_helper::Arg_helper(string arg_str_) {
  vector<string> v;
  UtilAll::Split(v, arg_str_, " ");
  m_args.insert(m_args.end(), v.begin(), v.end());
}

string Arg_helper::get_option(int idx_) const {
  if ((size_t)idx_ >= m_args.size()) {
    return "";
  }
  return m_args[idx_];
}

bool Arg_helper::is_enable_option(string opt_) const {
  for (size_t i = 0; i < m_args.size(); ++i) {
    if (opt_ == m_args[i]) {
      return true;
    }
  }
  return false;
}

string Arg_helper::get_option_value(string opt_) const {
  string ret = "";
  for (size_t i = 0; i < m_args.size(); ++i) {
    if (opt_ == m_args[i]) {
      size_t value_idx = ++i;
      if (value_idx >= m_args.size()) {
        return ret;
      }
      ret = m_args[value_idx];
      return ret;
    }
  }
  return ret;
}

//<!***************************************************************************
}  //<!end namespace;
