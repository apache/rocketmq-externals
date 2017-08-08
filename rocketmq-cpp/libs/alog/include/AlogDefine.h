/**
*@file AlogDefine.h
*@brief the file to define some micros.
*
*@version 1.0.7
*@date 2010.01.17
*@author jinhui.li
*/
#ifndef _ALOG_ALOG_DEFINE_H_
#define _ALOG_ALOG_DEFINE_H_

#include "Logger.h"
#include "Configurator.h"
#include <iostream>

#define ALOG_CONFIG_ROOT_LOGGER() alog::Configurator::configureRootLogger()
#define ALOG_CONFIG_LOGGER(filename) do {                       \
        try {                                                   \
            alog::Configurator::configureLogger(filename);      \
        } catch(std::exception &e) {                            \
            std::cerr << "Error!!! Failed to configure logger!" \
                      << e.what() << std::endl;                 \
            exit(-1);                                           \
        }                                                       \
    }while(0)

#define ALOG_SET_ROOT_LOG_LEVEL(level)                                  \
    alog::Logger::getRootLogger()->setLevel(alog::LOG_LEVEL_##level)

#define ALOG_SETUP(n, c) alog::Logger *c::_logger  \
    = alog::Logger::getLogger(#n "." #c)
#define ALOG_SETUP_TEMPLATE(n, c, T) template <typename T> \
    alog::Logger *c<T>::_logger                            \
    = alog::Logger::getLogger(#n "." #c)

#define ALOG_DECLARE() static alog::Logger *_logger
#define ALOG_DECLARE_AND_SETUP_LOGGER(n, c) static alog::Logger *_logger \
    = alog::Logger::getLogger(#n "." #c)
#define ALOG_LOG_SHUTDOWN() alog::Logger::shutdown()
#define ALOG_LOG_FLUSH() alog::Logger::flushAll()


#endif

