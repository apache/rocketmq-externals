#include "Logging.h"
#include <boost/date_time/gregorian/gregorian.hpp>
#include "UtilAll.h"
#define BOOST_DATE_TIME_SOURCE

namespace rocketmq {
logAdapter* logAdapter::alogInstance;
boost::mutex logAdapter::m_imtx;

logAdapter::~logAdapter() { logging::core::get()->remove_all_sinks(); }

logAdapter* logAdapter::getLogInstance() {
  if (alogInstance == NULL) {
    boost::mutex::scoped_lock guard(m_imtx);
    if (alogInstance == NULL) {
      alogInstance = new logAdapter();
    }
  }
  return alogInstance;
}

logAdapter::logAdapter() : m_logLevel(eLOG_LEVEL_INFO) {
  string homeDir(UtilAll::getHomeDirectory());
  homeDir.append("/logs/rocketmq-cpp/");
  m_logFile += homeDir;
  std::string fileName =
      UtilAll::to_string(getpid()) + "_" + "rocketmq-cpp.log.%N";
  m_logFile += fileName;

  // boost::log::expressions::attr<
  // boost::log::attributes::current_thread_id::value_type>("ThreadID");
  boost::log::register_simple_formatter_factory<
      boost::log::trivial::severity_level, char>("Severity");
  m_logSink = logging::add_file_log(
      keywords::file_name = m_logFile,
      keywords::rotation_size = 100 * 1024 * 1024,
      keywords::time_based_rotation =
          sinks::file::rotation_at_time_point(0, 0, 0),
      keywords::format = "[%TimeStamp%](%Severity%):%Message%",
      keywords::min_free_space = 300 * 1024 * 1024, keywords::target = homeDir,
      keywords::max_size = 200 * 1024 * 1024,  // max keep 3 log file defaultly
      keywords::auto_flush = true);
  logging::core::get()->set_filter(logging::trivial::severity >=
                                   logging::trivial::info);

  logging::add_common_attributes();
}

void logAdapter::setLogLevel(elogLevel logLevel) {
  m_logLevel = logLevel;
  switch (logLevel) {
    case eLOG_LEVEL_FATAL:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::fatal);
      break;
    case eLOG_LEVEL_ERROR:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::error);

      break;
    case eLOG_LEVEL_WARN:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::warning);

      break;
    case eLOG_LEVEL_INFO:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::info);

      break;
    case eLOG_LEVEL_DEBUG:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::debug);

      break;
    case eLOG_LEVEL_TRACE:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::trace);

      break;
    default:
      logging::core::get()->set_filter(logging::trivial::severity >=
                                       logging::trivial::info);

      break;
  }
}

elogLevel logAdapter::getLogLevel() { return m_logLevel; }

void logAdapter::setLogFileNumAndSize(int logNum, int sizeOfPerFile) {
  string homeDir(UtilAll::getHomeDirectory());
  homeDir.append("/logs/rocketmq-cpp/");
  m_logSink->locked_backend()->set_file_collector(sinks::file::make_collector(
      keywords::target = homeDir,
      keywords::max_size = logNum * sizeOfPerFile * 1024 * 1024));
}
}
