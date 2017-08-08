/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __ROCKETMQCLIENT_H__
#define __ROCKETMQCLIENT_H__

#ifdef WIN32
#ifdef ROCKETMQCLIENT_EXPORTS
#define ROCKETMQCLIENT_API __declspec(dllexport)
#else
#define ROCKETMQCLIENT_API __declspec(dllimport)
#endif
#else
#define ROCKETMQCLIENT_API
#endif

#endif
