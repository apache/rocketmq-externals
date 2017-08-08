/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "TopAddressing.h"
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <vector>
#include "UtilAll.h"
#include "sync_http_client.h"
#include "url.h"

namespace metaq {
TopAddressing::TopAddressing(string unitName) : m_unitName(unitName) {}

TopAddressing::~TopAddressing() {}

int TopAddressing::IsIPAddr(const char* sValue) {
  if (NULL == sValue) return -1;

  while (*sValue != '\0') {
    if ((*sValue < '0' || *sValue > '9') && (*sValue != '.')) return -1;
    sValue++;
  }
  return 0;
}

void TopAddressing::updateNameServerAddressList(const string& adds) {
  boost::lock_guard<boost::mutex> lock(m_addrLock);
  vector<string> out;
  UtilAll::Split(out, adds, ";");
  if (out.size() > 0) m_addrs.clear();
  for (size_t i = 0; i < out.size(); i++) {
    string addr = out[i];
    UtilAll::Trim(addr);

    list<string>::iterator findit = find(m_addrs.begin(), m_addrs.end(), addr);
    if (findit == m_addrs.end()) {
      string hostName;
      short portNumber;
      if (UtilAll::SplitURL(addr, hostName, portNumber)) {
        LOG_INFO("updateNameServerAddressList:%s", addr.c_str());
        m_addrs.push_back(addr);
      }
    }
  }
}

string TopAddressing::fetchNSAddr(const string& NSDomain) {
  LOG_DEBUG("fetchNSAddr begin");
  string nsAddr = NSDomain.empty() ? WS_ADDR : NSDomain;
  if (!m_unitName.empty()) {
    nsAddr = nsAddr + "-" + m_unitName + "?nofix=1";
    LOG_INFO("NSAddr is:%s", nsAddr.c_str());
  }

  std::string tmp_nameservers;
  std::string nameservers;
  Url url_s(nsAddr);
  LOG_INFO("protocol: %s, port: %s, host:%s, path:%s, ",
           url_s.protocol_.c_str(), url_s.port_.c_str(), url_s.host_.c_str(),
           url_s.path_.c_str());

  bool ret = SyncfetchNsAddr(url_s, tmp_nameservers);
  if (ret) {
    nameservers = clearNewLine(tmp_nameservers);
    if (nameservers.empty()) {
      LOG_ERROR("fetchNSAddr with domain is empty");
    } else {
      updateNameServerAddressList(nameservers);
    }
  } else {
    LOG_ERROR(
        "fetchNSAddr with domain failed, connect failure or wrnong response");
  }

  return nameservers;
}

string TopAddressing::clearNewLine(const string& str) {
  string newString = str;
  size_t index = newString.find("\r");
  if (index != string::npos) {
    return newString.substr(0, index);
  }

  index = newString.find("\n");
  if (index != string::npos) {
    return newString.substr(0, index);
  }

  return newString;
}
}  //<!end namespace;
