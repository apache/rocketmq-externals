/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "Arg_helper.h"
#include "UtilAll.h"

namespace metaq {
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
