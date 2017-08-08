/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef ROCKETMQ_CLIENT4CPP__SYNC_HTTP_CLIENT_H_
#define ROCKETMQ_CLIENT4CPP_SYNC_HTTP_CLIENT_H_

#include <string>

namespace metaq {
class Url;

extern bool SyncfetchNsAddr(const Url& url_s, std::string& body);

}  // namespace ons

#endif  // ROCKETMQ_CLIENT4CPP__SYNC_HTTP_CLIENT_H_
