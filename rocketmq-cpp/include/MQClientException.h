/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MQCLIENTEXCEPTION_H__
#define __MQCLIENTEXCEPTION_H__

#include <exception>
#include <ostream>
#include <sstream>
#include <string>
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MQException : public std::exception {
 public:
  MQException(const std::string& msg, int error, const char* file,
              int line) throw()
      : m_error(error), m_line(line), m_file(file) {
    try {
      std::stringstream ss;
      ss << "msg: " << msg << ",error:" << error << ",in file <" << file
         << "> line:" << line;
      m_msg = ss.str();
    } catch (...) {
    }
  }

  MQException(const std::string& msg, int error, const char* file,
              const char* type, int line) throw()
      : m_error(error), m_line(line), m_file(file), m_type(type) {
    try {
      std::stringstream ss;
      ss << "msg: " << msg << ",error:" << error << ",in file <" << file
         << "> line:" << line;
      m_msg = ss.str();
    } catch (...) {
    }
  }

  virtual ~MQException() throw() {}

  const char* what() const throw() { return m_msg.c_str(); }

  int GetError() const throw() { return m_error; }

  virtual const char* GetType() const throw() { return m_type.c_str(); }

 protected:
  int m_error;
  int m_line;
  std::string m_msg;
  std::string m_file;
  std::string m_type;
};

inline std::ostream& operator<<(std::ostream& os, const MQException& e) {
  os << "Type: " << e.GetType() << " , " << e.what();
  return os;
}

#define DEFINE_MQCLIENTEXCEPTION(name)                                     \
  class ROCKETMQCLIENT_API name : public MQException {                     \
   public:                                                                 \
    name(const std::string& msg, int error, const char* file,              \
         int line) throw()                                                 \
        : MQException(msg, error, file, #name, line) {}                    \
    virtual const char* GetType() const throw() { return m_type.c_str(); } \
  };

DEFINE_MQCLIENTEXCEPTION(MQClientException)
DEFINE_MQCLIENTEXCEPTION(MQBrokerException)
DEFINE_MQCLIENTEXCEPTION(InterruptedException)
DEFINE_MQCLIENTEXCEPTION(RemotingException)
DEFINE_MQCLIENTEXCEPTION(UnknownHostException)

#define THROW_MQEXCEPTION(e, msg, err) throw e(msg, err, __FILE__, __LINE__)
#define NEW_MQEXCEPTION(e, msg, err) e(msg, err, __FILE__, __LINE__)

//<!***************************************************************************
}  //<!end namespace;
#endif
