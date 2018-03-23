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
#include "UtilAll.h"

namespace rocketmq {
//<!************************************************************************
std::string UtilAll::s_localHostName;
std::string UtilAll::s_localIpAddress;

bool UtilAll::startsWith_retry(const string &topic) {
  return topic.find(RETRY_GROUP_TOPIC_PREFIX) == 0;
}

string UtilAll::getRetryTopic(const string &consumerGroup) {
  return RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
}

void UtilAll::Trim(string &str) {
  str.erase(0, str.find_first_not_of(' '));  // prefixing spaces
  str.erase(str.find_last_not_of(' ') + 1);  // surfixing spaces
}

bool UtilAll::isBlank(const string &str) {
  if (str.empty()) {
    return true;
  }

  string::size_type left = str.find_first_not_of(WHITESPACE);

  if (left == string::npos) {
    return true;
  }

  return false;
}

uint64 UtilAll::hexstr2ull(const char *str) {
  return boost::lexical_cast<uint64>(str);
}

int64 UtilAll::str2ll(const char *str) {
  return boost::lexical_cast<int64>(str);
}

string UtilAll::bytes2string(const char *bytes, int len) {
  if (bytes == NULL || len <= 0) {
    return string();
  }

#ifdef WIN32
  string buffer;
  for (int i = 0; i < len; i++) {
    char tmp[3];
    sprintf(tmp, "%02X", (unsigned char)bytes[i]);
    buffer.append(tmp);
  }

  return buffer;
#else
  char hex_str[] = "0123456789ABCDEF";
  char result[len * 2 + 1];
  result[len * 2] = 0;

  for (int i = 0; i < len; i++) {
    result[i * 2 + 0] = hex_str[(bytes[i] >> 4) & 0x0F];
    result[i * 2 + 1] = hex_str[(bytes[i]) & 0x0F];
  }

  string buffer(result);
  return buffer;
#endif
}

bool UtilAll::SplitURL(const string &serverURL, string &addr, short &nPort) {
  size_t pos = serverURL.find(':');
  if (pos == string::npos) {
    return false;
  }

  addr = serverURL.substr(0, pos);
  if (0 == addr.compare("localhost")) {
    addr = "127.0.0.1";
  }

  pos++;
  string port = serverURL.substr(pos, serverURL.length() - pos);
  nPort = atoi(port.c_str());
  if (nPort == 0) {
    return false;
  }
  return true;
}

int UtilAll::Split(vector<string> &ret_, const string &strIn, const char sep) {
  if (strIn.empty()) return 0;

  string tmp;
  string::size_type pos_begin = strIn.find_first_not_of(sep);
  string::size_type comma_pos = 0;

  while (pos_begin != string::npos) {
    comma_pos = strIn.find(sep, pos_begin);
    if (comma_pos != string::npos) {
      tmp = strIn.substr(pos_begin, comma_pos - pos_begin);
      pos_begin = comma_pos + 1;
    } else {
      tmp = strIn.substr(pos_begin);
      pos_begin = comma_pos;
    }

    if (!tmp.empty()) {
      ret_.push_back(tmp);
      tmp.clear();
    }
  }
  return ret_.size();
}
int UtilAll::Split(vector<string> &ret_, const string &strIn,
                   const string &sep) {
  if (strIn.empty()) return 0;

  string tmp;
  string::size_type pos_begin = strIn.find_first_not_of(sep);
  string::size_type comma_pos = 0;

  while (pos_begin != string::npos) {
    comma_pos = strIn.find(sep, pos_begin);
    if (comma_pos != string::npos) {
      tmp = strIn.substr(pos_begin, comma_pos - pos_begin);
      pos_begin = comma_pos + sep.length();
    } else {
      tmp = strIn.substr(pos_begin);
      pos_begin = comma_pos;
    }

    if (!tmp.empty()) {
      ret_.push_back(tmp);
      tmp.clear();
    }
  }
  return ret_.size();
}

int32_t UtilAll::StringToInt32(const std::string &str, int32_t &out) {
  out = 0;
  if (str.empty()) {
    return false;
  }

  char *end = NULL;
  errno = 0;
  long l = strtol(str.c_str(), &end, 10);
  /* Both checks are needed because INT_MAX == LONG_MAX is possible. */
  if (l > INT_MAX || (errno == ERANGE && l == LONG_MAX)) return false;
  if (l < INT_MIN || (errno == ERANGE && l == LONG_MIN)) return false;
  if (*end != '\0') return false;
  out = l;
  return true;
}

int64_t UtilAll::StringToInt64(const std::string &str, int64_t &val) {
  char *endptr = NULL;
  errno = 0; /* To distinguish success/failure after call */
  val = strtoll(str.c_str(), &endptr, 10);

  /* Check for various possible errors */
  if ((errno == ERANGE && (val == LONG_MAX || val == LONG_MIN)) ||
      (errno != 0 && val == 0)) {
    return false;
  }
  /*no digit was found Or  Further characters after number*/
  if (endptr == str.c_str()) {
    return false;
  }
  /*no digit was found Or  Further characters after number*/
  if (*endptr != '\0') {
    return false;
  }
  /* If we got here, strtol() successfully parsed a number */
  return true;
}

string UtilAll::getLocalHostName() {
  if (s_localHostName.empty()) {
    // boost::system::error_code error;
    // s_localHostName = boost::asio::ip::host_name(error);

    char name[1024];
    boost::system::error_code ec;
    if (boost::asio::detail::socket_ops::gethostname(name, sizeof(name), ec) !=
        0) {
      return std::string();
    }
    s_localHostName.append(name, strlen(name));
  }
  return s_localHostName;
}

string UtilAll::getLocalAddress() {
  if (s_localIpAddress.empty()) {
    boost::asio::io_service io_service;
    boost::asio::ip::tcp::resolver resolver(io_service);
    boost::asio::ip::tcp::resolver::query query(getLocalHostName(), "");
    boost::system::error_code error;
    boost::asio::ip::tcp::resolver::iterator iter =
        resolver.resolve(query, error);
    if (error) {
      return "";
    }
    boost::asio::ip::tcp::resolver::iterator end;  // End marker.
    boost::asio::ip::tcp::endpoint ep;
    while (iter != end) {
      ep = *iter++;
    }
    s_localIpAddress = ep.address().to_string();
  }
  return s_localIpAddress;
}

string UtilAll::getHomeDirectory() {
#ifndef WIN32
  char *homeEnv = getenv("HOME");
  string homeDir;
  if (homeEnv == NULL) {
    homeDir.append(getpwuid(getuid())->pw_dir);
  } else {
    homeDir.append(homeEnv);
  }
#else
  string homeDir(getenv("USERPROFILE"));
#endif
  return homeDir;
}

string UtilAll::getProcessName() {
#ifndef WIN32
  char buf[PATH_MAX + 1] = {0};
  int count = PATH_MAX + 1;
  char procpath[PATH_MAX + 1] = {0};
  sprintf(procpath, "/proc/%d/exe", getpid());

  if (access(procpath, F_OK) == -1) {
    return "";
  }

  int retval = readlink(procpath, buf, count - 1);
  if ((retval < 0 || retval >= count - 1)) {
    return "";
  }
  if (!strcmp(buf + retval - 10, " (deleted)"))
    buf[retval - 10] = '\0';  // remove last " (deleted)"
  else
    buf[retval] = '\0';

  char *process_name = strrchr(buf, '/');
  if (process_name) {
    return std::string(process_name + 1);
  } else {
    return "";
  }
#else
  TCHAR szFileName[MAX_PATH + 1];
  GetModuleFileName(NULL, szFileName, MAX_PATH + 1);
  return std::string(szFileName);
#endif
}

uint64_t UtilAll::currentTimeMillis() {
  boost::posix_time::ptime current_date_microseconds =
      boost::posix_time::microsec_clock::local_time();
  return current_date_microseconds.time_of_day().total_milliseconds();
}

uint64_t UtilAll::currentTimeSeconds() {
  boost::posix_time::ptime current_date_microseconds =
      boost::posix_time::microsec_clock::local_time();
  return current_date_microseconds.time_of_day().total_seconds();
}

bool UtilAll::deflate(std::string &input, std::string &out, int level) {
  boost::iostreams::zlib_params zlibParams(level,
                                           boost::iostreams::zlib::deflated);
  boost::iostreams::filtering_ostream compressingStream;
  compressingStream.push(boost::iostreams::zlib_compressor(zlibParams));
  compressingStream.push(boost::iostreams::back_inserter(out));
  compressingStream << input;
  boost::iostreams::close(compressingStream);

  return true;
}

bool UtilAll::inflate(std::string &input, std::string &out) {
  boost::iostreams::filtering_ostream decompressingStream;
  decompressingStream.push(boost::iostreams::zlib_decompressor());
  decompressingStream.push(boost::iostreams::back_inserter(out));
  decompressingStream << input;
  boost::iostreams::close(decompressingStream);

  return true;
}

bool UtilAll::ReplaceFile(const std::string &from_path,
                          const std::string &to_path) {
#ifdef WIN32
  // Try a simple move first.  It will only succeed when |to_path| doesn't
  // already exist.
  if (::MoveFile(from_path.c_str(), to_path.c_str())) return true;
  // Try the full-blown replace if the move fails, as ReplaceFile will only
  // succeed when |to_path| does exist. When writing to a network share, we may
  // not be able to change the ACLs. Ignore ACL errors then
  // (REPLACEFILE_IGNORE_MERGE_ERRORS).
  if (::ReplaceFile(to_path.c_str(), from_path.c_str(), NULL,
                    REPLACEFILE_IGNORE_MERGE_ERRORS, NULL, NULL)) {
    return true;
  }
  return false;
#else
  if (rename(from_path.c_str(), to_path.c_str()) == 0) return true;
  return false;
#endif
}
}
