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

# Find jsoncpp
#
# Find the jsoncpp includes and library
#
# if you nee to add a custom library search path, do it via via CMAKE_PREFIX_PATH
#
# -*- cmake -*-
# - Find JSONCpp
# Find the JSONCpp includes and library
# This module defines
#  JSONCPP_INCLUDE_DIRS, where to find json.h, etc.
#  JSONCPP_LIBRARIES, the libraries needed to use jsoncpp.
#  JSONCPP_FOUND, If false, do not try to use jsoncpp.
#  also defined, but not for general use are
#  JSONCPP_LIBRARIES, where to find the jsoncpp library.

# Support preference of static libs by adjusting CMAKE_FIND_LIBRARY_SUFFIXES
if( JSONCPP_USE_STATIC_LIBS )
  set(_jsoncpp_ORIG_CMAKE_FIND_LIBRARY_SUFFIXES    :${CMAKE_FIND_LIBRARY_SUFFIXES})
    if(WIN32)
        list(INSERT CMAKE_FIND_LIBRARY_SUFFIXES 0 .lib .a)
    else()
        set(CMAKE_FIND_LIBRARY_SUFFIXES .a)
    endif()
else()
  set(_jsoncpp_ORIG_CMAKE_FIND_LIBRARY_SUFFIXES    :${CMAKE_FIND_LIBRARY_SUFFIXES})
    if(WIN32)
        list(INSERT CMAKE_FIND_LIBRARY_SUFFIXES 0 .dll .so)
    else()
        set(CMAKE_FIND_LIBRARY_SUFFIXES .so)
    endif()
endif()

FIND_PATH(JSONCPP_INCLUDE_DIRS 
NAMES
	json.h
	json/json.h
PATHS
	/usr/include
    /usr/local/include
    C:/jsoncpp/include
    ${CMAKE_SOURCE_DIR}/win32-deps/include
    C:/jsoncpp-0.10.6/include
PATH_SUFFIXES jsoncpp
)

find_library(JSONCPP_LIBRARIES
    NAMES jsoncpp
    PATHS /usr/lib /usr/local/lib C:/jsoncpp/lib ${CMAKE_SOURCE_DIR}/win32-deps/lib C:/jsoncpp-0.10.6/
)
IF (JSONCPP_LIBRARIES AND JSONCPP_INCLUDE_DIRS)
    SET(JSONCPP_LIBRARIES ${JSONCPP_LIBRARIES})
    SET(JSONCPP_FOUND "YES")
ELSE (JSONCPP_LIBRARIES AND JSONCPP_INCLUDE_DIRS)
  SET(JSONCPP_FOUND "NO")
ENDIF (JSONCPP_LIBRARIES AND JSONCPP_INCLUDE_DIRS)


IF (JSONCPP_FOUND)
   IF (NOT JSONCPP_FIND_QUIETLY)
      MESSAGE(STATUS "Found JSONCpp: ${JSONCPP_LIBRARIES}")
   ENDIF (NOT JSONCPP_FIND_QUIETLY)
ELSE (JSONCPP_FOUND)
   IF (JSONCPP_FIND_REQUIRED)
      MESSAGE(FATAL_ERROR "Could not find JSONCPP library include: ${JSONCPP_INCLUDE_DIRS}, lib: ${JSONCPP_LIBRARIES}")
   ENDIF (JSONCPP_FIND_REQUIRED)
ENDIF (JSONCPP_FOUND)

# Deprecated declarations.
SET (NATIVE_JSONCPP_INCLUDE_PATH ${JSONCPP_INCLUDE_DIRS} )
GET_FILENAME_COMPONENT (NATIVE_JSONCPP_LIB_PATH ${JSONCPP_LIBRARIES} PATH)

MARK_AS_ADVANCED(
  JSONCPP_LIBRARIES
  JSONCPP_INCLUDE_DIRS
  )

# Restore the original find library ordering
if( JSONCPP_USE_STATIC_LIBS )
  set(CMAKE_FIND_LIBRARY_SUFFIXES ${_jsoncpp_ORIG_CMAKE_FIND_LIBRARY_SUFFIXES})
endif()
