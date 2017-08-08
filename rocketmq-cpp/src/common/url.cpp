/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "url.h"
#include <algorithm>
#include <cctype>
#include <functional>
#include <iterator>
#include <string>

namespace metaq {

Url::Url(const std::string& url_s) { parse(url_s); }

void Url::parse(const std::string& url_s) {
  const std::string prot_end("://");
  auto prot_i =
      std::search(url_s.begin(), url_s.end(), prot_end.begin(), prot_end.end());
  protocol_.reserve(std::distance(url_s.begin(), prot_i));
  std::transform(url_s.begin(), prot_i, std::back_inserter(protocol_),
                 std::ptr_fun<int, int>(tolower));  // protocol is icase

  if (prot_i == url_s.end()) return;

  std::advance(prot_i, prot_end.length());

  auto path_i = find(prot_i, url_s.end(), ':');
  std::string::const_iterator path_end_i;
  if (path_i == url_s.end()) {
    // not include port, use default port
    port_ = "80";
    path_i = std::find(prot_i, url_s.end(), '/');
    path_end_i = path_i;
  } else {
    auto port_i = find(path_i + 1, url_s.end(), '/');
    port_.insert(port_.begin(), path_i + 1, port_i);
    path_end_i = path_i + port_.length() + 1;
  }

  host_.reserve(distance(prot_i, path_i));
  std::transform(prot_i, path_i, std::back_inserter(host_),
                 std::ptr_fun<int, int>(tolower));  // host is icase}

  auto query_i = find(path_end_i, url_s.end(), '?');
  path_.assign(path_end_i, query_i);
  if (query_i != url_s.end()) ++query_i;
  query_.assign(query_i, url_s.end());
}

}  // namespace ons
