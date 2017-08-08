#ifndef _ALOG_ADAPTER_H_
#define _ALOG_ADAPTER_H_

#include "Logger.h"
#include "MQClient.h"

namespace metaq {

class ALogAdapter {
 public:
  ~ALogAdapter();
  static ALogAdapter& getLogInstance();
  void setLogLevel(elogLevel logLevel);
  elogLevel getLogLevel();
  void setLogFileNumAndSize(int logNum, int sizeOfPerFile);
  alog::Logger* GetLogger() { return alogger_; }

 private:
  ALogAdapter();
  alog::Logger* alogger_;
  elogLevel m_logLevel;
  std::string m_logFile;
};

#define ALOG_ADAPTER ALogAdapter::getLogInstance()

#define AGENT_LOGGER ALOG_ADAPTER.GetLogger()
#define LOG_ERROR(format, args...) ALOG_ERROR(AGENT_LOGGER, format, ##args)
#define LOG_WARN(format, args...) ALOG_WARN(AGENT_LOGGER, format, ##args)
#define LOG_INFO(format, args...) ALOG_INFO(AGENT_LOGGER, format, ##args)
#define LOG_DEBUG(format, args...) ALOG_DEBUG(AGENT_LOGGER, format, ##args)
}
#endif  // MIDDLEWARE_VIPCLIENT_ALOG_ADAPTER_H_
