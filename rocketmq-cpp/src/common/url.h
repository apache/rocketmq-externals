/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef ROCKETMQ_CLIENT4CPP_URL_HH_
#define ROCKETMQ_CLIENT4CPP_URL_HH_

#include <string>

namespace metaq {
class Url {
 public:
  Url(const std::string& url_s);  // omitted copy, ==, accessors, ...

 private:
  void parse(const std::string& url_s);

 public:
  std::string protocol_;
  std::string host_;
  std::string port_;
  std::string path_;
  std::string query_;
};
}
#endif  // ROCKETMQ_CLIENT4CPP_URL_HH_
