/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef _ARG_HELPER_H_
#define _ARG_HELPER_H_

#include <string>
#include <vector>
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API Arg_helper {
 public:
  Arg_helper(int argc, char* argv[]);
  Arg_helper(std::string arg_str_);
  std::string get_option(int idx_) const;
  bool is_enable_option(std::string opt_) const;
  std::string get_option_value(std::string opt_) const;

 private:
  std::vector<std::string> m_args;
};

//<!***************************************************************************
}  //<!end namespace;

#endif  //<!_ARG_HELPER_H_;
