#ifndef _ALOG_LOGGER_H_
#define _ALOG_LOGGER_H_

#include <set>
#include <map>
#include <vector>
#include <string>
#include <stdint.h>
#include <stdarg.h>
#include "Sync.h"
#include "Version.h"

namespace alog
{
class LoggingEvent;
class Appender;
/** the disable message level*/
const uint32_t LOG_LEVEL_DISABLE = 0;
/** the fatal message level*/
const uint32_t LOG_LEVEL_FATAL = LOG_LEVEL_DISABLE + 1;
/** the error message level*/
const uint32_t LOG_LEVEL_ERROR = LOG_LEVEL_FATAL + 1;
/** the warning message level*/
const uint32_t LOG_LEVEL_WARN = LOG_LEVEL_ERROR + 1;
/** the information level*/
const uint32_t LOG_LEVEL_INFO = LOG_LEVEL_WARN + 1;
/** the debug message level*/
const uint32_t LOG_LEVEL_DEBUG = LOG_LEVEL_INFO + 1;
/** the trace level 1 */
const uint32_t LOG_LEVEL_TRACE1 = LOG_LEVEL_DEBUG + 1;
/** the trace level 2 */
const uint32_t LOG_LEVEL_TRACE2 = LOG_LEVEL_TRACE1 + 1;
/** the trace level 3 */
const uint32_t LOG_LEVEL_TRACE3 = LOG_LEVEL_TRACE2 + 1;
/** level not set */
const uint32_t LOG_LEVEL_NOTSET = LOG_LEVEL_TRACE3 + 1;
/** the count of log level defined above. */
const uint32_t LOG_LEVEL_COUNT = LOG_LEVEL_NOTSET + 1;

/**
 *@class Logger
 *@brief The main class of logger module
 *
 *The Logger class is the handle to output log information, one logger may have many output destinations called Appender class,
 *@n and you can call Logger's method to change output destination, log level etc.
 *@n There is a root Logger object initialized in getRootLogger() function, and its default output destination is console, default log
 *@n level is LOG_LEVEL_INFO, if you do not want to change the logger's action in multi-module, you had better use the root Logger
 *@n object for high performance.
 *
 *@version 1.0.0
 *@date 2008.12.19
 *@author jinhui.li
 *@warning
 */
class Logger
{
public:
    /**
     *@brief Get the root Logger object.
     *
     *the name of root logger is "", and the default output destination is console, default log level is LOG_LEVEL_INFO.
     *@return The root logger's pointer.
     */
    static Logger *getRootLogger();
    /**
     *@brief get the logger object by the name, like "com.alibaba.isearch".
     *
     *you will get the same logger object if the name is same.
     *
     *@param name The logger's name
     *@param bInherit to imply if the logger's action inherit from parent and root logger
     *@return the logger's pointer whose name is called 'name'
     */
    static Logger *getLogger(const char *name, bool bInherit = true);
    /**
     *@brief clear all the loggers and it's Appender list.
     *
     *the root logger would be removed here.
     */
    static void shutdown();
    /**
     *@brief flush all logger's message.
     */
    static void flushAll();

    /**
     *@brief set trash directory.
     *@param trash directory name, if dir is empty, Logger will not use trash.
     */
    static void setTrashDir(const std::string& dir);

    static std::string getTrashDir();

    /**
     *@brief output a message like printf() function in c language.
     *
     *@param level The message's level, which can be one value of LOG_LEVEL_DISABLE, LOG_LEVEL_FATAL, LOG_LEVEL_ERROR, 
     *@n LOG_LEVEL_WARN, LOG_LEVEL_INFO, LOG_LEVEL_DEBUG, LOG_LEVEL_TRACE1, LOG_LEVEL_TRACE2, LOG_LEVEL_TRACE3.
     *@param format the format that you want to output.
     */
    void log(uint32_t level, const char *format, ...) __attribute__((format(printf, 3, 4)));

    void logVaList(uint32_t level, const char *filename, int line, const char* function, const char *format, va_list ap);

    /**
     *@brief output a message like printf() function in c language, and output the __FILE__, __FUNCTION__, __LINE__ informations
     *
     *@param level The message's level, which can be one value of LOG_LEVEL_DISABLE, LOG_LEVEL_FATAL, LOG_LEVEL_ERROR, 
     *@n LOG_LEVEL_WARN, LOG_LEVEL_INFO, LOG_LEVEL_DEBUG, LOG_LEVEL_TRACE1, LOG_LEVEL_TRACE2, LOG_LEVEL_TRACE3.
     *@param format the format that you want to output.
     *@param filename the file name to input log message
     *@param function the function name to input log message
     *@param lineNumber the line number to input log message
     */
    void log(uint32_t level, const char *filename, int line, const char* function, const char *format, ...) __attribute__((format(printf, 6, 7)));

    /**
     *@brief output a pure message, there is no va_list.
     *
     *@param level The message's level, which can be one value of LOG_LEVEL_DISABLE, LOG_LEVEL_FATAL, LOG_LEVEL_ERROR, 
     *@n LOG_LEVEL_WARN, LOG_LEVEL_INFO, LOG_LEVEL_DEBUG, LOG_LEVEL_TRACE1, LOG_LEVEL_TRACE2, LOG_LEVEL_TRACE3.
     *@param message the information that you want to output.
     *@param name the logger name which will be outputed. default NULL, then name will be revalued to this->m_loggerName.
     *@param isLevel whether need to compare log level. default true. if this method is called from it's child,this value should be false.
     */
    void logPureMessage(uint32_t level, const char *message);

    /**
     *@brief output a binary message, there is no va_list.
     *
     *@param level The message's level, which can be one value of LOG_LEVEL_DISABLE, LOG_LEVEL_FATAL, LOG_LEVEL_ERROR,
     *@n LOG_LEVEL_WARN, LOG_LEVEL_INFO, LOG_LEVEL_DEBUG, LOG_LEVEL_TRACE1, LOG_LEVEL_TRACE2, LOG_LEVEL_TRACE3.
     *@param message the information that you want to output.
     */
    void logBinaryMessage(uint32_t level, const std::string &message);

    void logBinaryMessage(uint32_t level, const char *message, size_t len);

    /**
     *@brief flush the logger's message.
     */
    void flush();

    /**
     *@brief set appender for this logger
     *
     *this method will remove all the appenders belong to the logger before, then set the appender as the logger's output destination.
     * If you want to add a appender to logger's appender list, please call addAppender() function.
     *@param appender the pointer of new appender.
     */
    void setAppender(Appender *appender);
    /**
     *@brief add a appender to  logger's appender list.
     *
     *@param appender the pointer of added appender.
     */
    void addAppender(Appender *appender);
    /**
     *@brief to clear all the appenders for this logger, but do not delete.
     */
    void removeAllAppenders();
    /**
     *@brief set the log level for this logger.
     *
     *@param level the logger's level you want to set.
     */
    void setLevel(uint32_t level);
    /**
     *@brief get the log level of this logger.
     *
     *@return level the logger's level.
     */
    uint32_t getLevel() const;
    /**
     *@brief to judge if the log level is enabled.
     *
     *@return 'true' if the level is enabled, and 'false' if the level is disabled.
     */
    inline bool isLevelEnabled(uint32_t level)
    {
        return (level <= m_loggerLevel)? true : false;
    }
    /**
     *@brief get the logger name, and the root logger's name is ""
     *
     *@return the logger's name
     */
    const std::string& getName() const;
    /**
     *@brief get the inherit flag.
     *@return 'true' imply to inherit from parent logger, 'false' imply not to inherit.
     */
    bool getInheritFlag() const;
    /**
     *@brief set the inherit flag.
     *@param bInherit 'true' imply to inherit from parent logger, 'false' imply not to inherit.
     */
    void setInheritFlag(bool bInherit);
    /**
     *@brief get the parent logger.
     *@return parent logger's point
     */
    const Logger *getParentLogger() const
    {
        return m_parent;
    }

    static int MAX_MESSAGE_LENGTH;

private:
    //Logger();
    Logger(const char *logName, uint32_t level = LOG_LEVEL_INFO, Logger *parent = NULL, bool bInherit = true);
    Logger(const Logger &logger) {}
    ~Logger();

    /**
     *@brief to log parm msg to all appenders by calling append function.
     *
     *@param level for log level.
     *@param msg message for log.
     *@param name for logger name.
     */
    void _log(LoggingEvent& event);
    /**
     *@brief to get a logger by name and inherit, and recursively init parent logger.
     *
     *@param name for logger name, bInherit for logger inherit flag.
     */
    static Logger* _getLogger(const char *name, bool bInherit = true);
    /**
     *@brief set the log level from parent logger.
     *
     *@param level the logger's level you want to set.
     */
    void setLevelByParent(uint32_t level);
    
    /* trash directory. */
    static std::string s_trashDir;
    static Mutex s_trashDirMutex;
    
    /* Global root logger and its locker. */
    static Logger *s_rootLogger;
    static Mutex s_rootMutex;
    /* Global loggers stored in map, and its locker. */
    static std::map<std::string, Logger *> *s_loggerMap;
    static Mutex s_loggerMapMutex;
    /* Logger's appender set and its locker.*/
    std::set<Appender *> m_appenderSet;
    Mutex m_appenderMutex;
    /* Logger's name.*/
    std::string m_loggerName;
    /* Logger's level.*/
    uint32_t m_loggerLevel;
    /* Is level set not by parent.*/
    bool m_bLevelSet;
    /* Parent logger pointer */
    Logger *m_parent;
    /* Children logger pointers */
    std::vector<Logger*> m_children;
    /* Inherit flag */
    bool m_bInherit;

};

/** define the macro of simple function call,
 * and these defines are available for gcc compile, unavailable for VC
 */
#define ALOG_FATAL(logger, format, args...) {                           \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_FATAL), 0))          \
            logger->log(alog::LOG_LEVEL_FATAL, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_ERROR(logger, format, args...) {                           \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_ERROR), 0))          \
            logger->log(alog::LOG_LEVEL_ERROR, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_WARN(logger, format, args...) {                            \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_WARN), 0))          \
            logger->log(alog::LOG_LEVEL_WARN, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_INFO(logger, format, args...) {                            \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_INFO), 0))          \
            logger->log(alog::LOG_LEVEL_INFO, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_DEBUG(logger, format, args...) {                           \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_DEBUG), 0))          \
            logger->log(alog::LOG_LEVEL_DEBUG, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_TRACE1(logger, format, args...) {                          \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_TRACE1), 0))          \
            logger->log(alog::LOG_LEVEL_TRACE1, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_TRACE2(logger, format, args...) {                          \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_TRACE2), 0))          \
            logger->log(alog::LOG_LEVEL_TRACE2, __FILE__, __LINE__, __FUNCTION__, format, ##args);}
#define ALOG_TRACE3(logger, format, args...) {                          \
        if(__builtin_expect(logger->isLevelEnabled(alog::LOG_LEVEL_TRACE3), 0))          \
            logger->log(alog::LOG_LEVEL_TRACE3, __FILE__, __LINE__, __FUNCTION__, format, ##args);}

#define ALOG_LOG(logger, level, format, args...) {                      \
        if(__builtin_expect(logger->isLevelEnabled(level), 0))          \
            logger->log(level, __FILE__, __LINE__, __FUNCTION__, format, ##args);}

}

#endif

