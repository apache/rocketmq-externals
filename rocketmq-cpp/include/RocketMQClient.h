/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/** A platform-independent 8-bit signed integer type. */
typedef signed char int8;
/** A platform-independent 8-bit unsigned integer type. */
typedef unsigned char uint8;
/** A platform-independent 16-bit signed integer type. */
typedef signed short int16;
/** A platform-independent 16-bit unsigned integer type. */
typedef unsigned short uint16;
/** A platform-independent 32-bit signed integer type. */
typedef signed int int32;
/** A platform-independent 32-bit unsigned integer type. */
typedef unsigned int uint32;
/** A platform-independent 64-bit integer type. */
typedef long long int64;
/** A platform-independent 64-bit unsigned integer type. */
typedef unsigned long long uint64;

#endif
