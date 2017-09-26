# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
 
cmake_minimum_required(VERSION 2.8)


# CMake complains if we don't have this.
if (COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif()

# We're escaping quotes in the Windows version number, because
# for some reason CMake won't do it at config version 2.4.7
# It seems that this restores the newer behaviour where define
# args are not auto-escaped.
if (COMMAND cmake_policy)
    cmake_policy(SET CMP0005 NEW)
endif()

# First, declare project (important for prerequisite checks).
project(all)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "Release")
endif()
set(CMAKE_CONFIGURATION_TYPES "Release")
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake)
set(CMAKE_ALLOW_LOOSE_LOOP_CONSTRUCTS ON)
set(CMAKE_VERBOSE_MAKEFILE 1)

#Find dependency 
set(Boost_USE_STATIC_LIBS        ON) # only find static libs
set(Boost_USE_MULTITHREADED      ON)
set(Boost_USE_STATIC_RUNTIME    ON)
if(WIN32)
    find_package(Boost 1.56 REQUIRED COMPONENTS thread system chrono date_time
        log log_setup regex serialization filesystem locale iostreams zlib)
else()
    find_package(Boost 1.56 REQUIRED COMPONENTS thread system chrono date_time log log_setup regex serialization filesystem locale iostreams) 
endif()

if(Boost_FOUND)
    message(status "** Boost Include dir: ${Boost_INCLUDE_DIR}")
    message(status "** Boost Libraries dir: ${Boost_LIBRARY_DIRS}")
    message(status "** Boost Libraries: ${Boost_LIBRARIES}")
    include_directories(${Boost_INCLUDE_DIRS})
endif()

set(Libevent_USE_STATIC_LIBS    ON) # only find static libs
find_package(Libevent 2.0.22 REQUIRED COMPONENTS) 

if(LIBEVENT_FOUND)
    include_directories(${LIBEVENT_INCLUDE_DIRS})
    message(status "** libevent Include dir: ${LIBEVENT_INCLUDE_DIR}")
    message(status "** libevent Libraries: ${LIBEVENT_LIBRARIES}")
endif()

set(JSONCPP_USE_STATIC_LIBS    ON) # only find static libs
find_package(Jsoncpp 0.10.6)
if(JSONCPP_FOUND)
   include_directories(${JSONCPP_INCLUDE_DIRS})
endif()

# put binaries in a different dir to make them easier to find.
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)

# for unix, put debug files in a separate bin "debug" dir.
# release bin files should stay in the root of the bin dir.
# if (CMAKE_GENERATOR STREQUAL "Unix Makefiles")
#     if (CMAKE_BUILD_TYPE STREQUAL Debug)
#         set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin/debug)
#         set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin/debug)
#     endif()
# endif()

IF (WIN32)
  add_definitions(-DWIN32 -DROCKETMQCLIENT_EXPORTS)
  SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /EHsc")
  set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
  set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /MTd")
ELSE()
set(C_FLAGS
  -g
  -Wall
  -Wno-deprecated
  -fPIC
  -fno-strict-aliasing 
)
set(CXX_FLAGS
 -g
 -Wall
 -Wno-deprecated
 -fPIC
 -fno-strict-aliasing
 -std=c++0x
 # -finline-limit=1000
 # -Wextra
 # -pedantic
 # -pedantic-errors
 # -D_FILE_OFFSET_BITS=64
 # -DVALGRIND
 # -DCHECK_PTHREAD_RETURN_VALUE
 # -Werror
 # -Wconversion
 # -Wno-unused-parameter
 # -Wunused-but-set-variable
 # -Wold-style-cast
 # -Woverloaded-virtual
 # -Wpointer-arith
 # -Wshadow
 # -Wwrite-strings
 # -Wdeprecated-declarations
 # -march=native
 # -MMD
 # -std=c++0x
 # -rdynamic
 )

if(CMAKE_BUILD_BITS EQUAL 32)
  list(APPEND CXX_FLAGS "-m32")
else() #not-condition
  list(APPEND CXX_FLAGS "-m64")
endif()

string(REPLACE ";" " " CMAKE_CXX_FLAGS "${CXX_FLAGS}")
string(REPLACE ";" " " CMAKE_C_FLAGS "${C_FLAGS}")
 
set(CMAKE_CXX_FLAGS_DEBUG   "-O0 -DDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")


# Declare deplibs, so we can use list in linker later. There's probably
# a more elegant way of doing this; with SCons, when you check for the
# lib, it is automatically passed to the linker.
set(deplibs)

# For some reason, the check_function_exists macro doesn't detect
# the inet_aton on some pure Unix platforms (e.g. sunos5). So we
# need to do a more detailed check and also include some extra deplibs.
list(APPEND deplibs dl)
list(APPEND deplibs pthread)
list(APPEND deplibs rt)
list(APPEND deplibs z)

# add include dir for bsd (posix uses /usr/include/)
set(CMAKE_INCLUDE_PATH "${CMAKE_INCLUDE_PATH}:/usr/local/include")
ENDIF()

# For config.h, set some static values; it may be a good idea to make
# these values dynamic for non-standard UNIX compilers.
set(ACCEPT_TYPE_ARG3 socklen_t)
set(HAVE_CXX_BOOL 1)
set(HAVE_CXX_CASTS 1)
set(HAVE_CXX_EXCEPTIONS 1)
set(HAVE_CXX_MUTABLE 1)
set(HAVE_CXX_STDLIB 1)
set(HAVE_PTHREAD_SIGNAL 1)
set(SELECT_TYPE_ARG1 int)
set(SELECT_TYPE_ARG234 "(fd_set *)")
set(SELECT_TYPE_ARG5 "(struct timeval *)")
set(STDC_HEADERS 1)
set(TIME_WITH_SYS_TIME 1)
set(HAVE_SOCKLEN_T 1)

# For config.h, save the results based on a template (config.h.in).
# configure_file(res/config.h.in ${root_dir}/config.h)

# add_definitions(-DSYSAPI_UNIX=1 -DHAVE_CONFIG_H)

add_subdirectory(libs)
add_subdirectory(project)
add_subdirectory(example)
