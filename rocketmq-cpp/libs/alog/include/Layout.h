/**
*@file Layout.h
*@brief the file to declare Layout class and its children class.
*
*@version 1.0.4
*@date 2009.03.04
*@author bingbing.yang
*/
#ifndef _ALOG_LAYOUT_H_
#define _ALOG_LAYOUT_H_

#include <string>
#include <vector>

namespace alog
{
class LoggingEvent;
class FormatComponent;
/**
*@class Layout
*@brief the root class to format output destination
*
*@version 1.0.4
*@date 2009.03.04
*@author bingbing.yang
*@warning
*/
class Layout
{
public:
    /**
      * @brief Destructor for Layout.
      */
    virtual ~Layout() { };

    /**
      * @brief Formats the LoggingEvent data to a string that appenders can log.
      * @param event The LoggingEvent.
      * @returns an appendable string.
      **/
    virtual std::string format(const LoggingEvent& event) = 0;
};

/**
*@class BasicLayout
*@brief the class to basicly format output destination
*
*@version 1.0.4
*@date 2009.03.05
*@author bingbing.yang
*@warning
*/
class BasicLayout : public Layout
{
public:
    /**
      * @brief Destructor for BasicLayout.
      */
    virtual ~BasicLayout() { };

    /**
      * @brief Formats the LoggingEvent data to a string that appenders can log.
      * @param event The LoggingEvent.
      * @returns an appendable string.
      **/
    virtual std::string format(const LoggingEvent& event);
};

/**
*@class Layout
*@brief the class to simply format output destination
*
*@version 1.0.4
*@date 2009.03.05
*@author bingbing.yang
*@warning
*/
class SimpleLayout : public Layout
{
public:
    /**
      * @brief Destructor for SimpleLayout.
      */
    virtual ~SimpleLayout() { };

    /**
      * @brief Formats the LoggingEvent data to a string that appenders can log.
      * @param event The LoggingEvent.
      * @returns an appendable string.
      **/
    virtual std::string format(const LoggingEvent& event);
};

/**
*@class Layout
*@brief the class to format binary data.
*
*@date 2015.11.18
*@author yuejun.huyj
*/
class BinaryLayout : public Layout
{
public:
    /**
      * @brief Destructor for BinaryLayout.
      */
    virtual ~BinaryLayout() { };

    /**
      * @brief Formats the LoggingEvent data to a string which is exactly the binary message itself.
      * @param event The LoggingEvent.
      * @returns an appendable string.
      **/
    virtual std::string format(const LoggingEvent& event);
};

/**
*@class Layout
*@brief the class to format output destination by user's design pattern
*
*@version 1.0.4
*@date 2009.03.05
*@author bingbing.yang
*@warning
*/
class PatternLayout : public Layout
{
public:
    PatternLayout();
    virtual ~PatternLayout();

    /**
      * @brief Formats the LoggingEvent data to a string that appenders can log.
      * @param event The LoggingEvent.
      * @returns an appendable string.
      **/
    virtual std::string format(const LoggingEvent& event);

    /**
      * @brief Sets the format of log lines handled by this PatternLayout. 
      * @n Format characters are as follows:
      * @n %%c - the logger name
      * @n %%d - the date
      * @n %%m - the message
      * @n %%F - the file name
      * @n %%n - the line number
      * @n %%f - the function name
      * @n %%l - the level
      * @n %%p - the process id
      * @n %%t - the thread id
      * @param logPattern the conversion pattern
      **/
    virtual void setLogPattern(const std::string& logPattern);

    virtual void clearLogPattern();

private:
    typedef std::vector<FormatComponent*> ComponentVector; 
    ComponentVector m_components;
    std::string m_logPattern;
    
};

}
#endif
