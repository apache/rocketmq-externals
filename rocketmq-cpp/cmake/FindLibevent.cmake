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

# - Try to find libevent
#.rst
# FindLibevent
# ------------
#
# Find Libevent include directories and libraries. Invoke as::
#
#   find_package(Libevent
#     [version] [EXACT]   # Minimum or exact version
#     [REQUIRED]          # Fail if Libevent is not found
#     [COMPONENT <C>...]) # Libraries to look for
#
# Valid components are one or more of:: libevent core extra pthreads openssl.
# Note that 'libevent' contains both core and extra. You must specify one of
# them for the other components.
#
# This module will define the following variables::
#
#  LIBEVENT_FOUND        - True if headers and requested libraries were found
#  LIBEVENT_INCLUDE_DIRS - Libevent include directories
#  LIBEVENT_LIBRARIES    - Libevent libraries to be linked
#  LIBEVENT_<C>_FOUND    - Component <C> was found (<C> is uppercase)
#  LIBEVENT_<C>_LIBRARY  - Library to be linked for Libevent component <C>.

find_package(PkgConfig QUIET)
pkg_check_modules(PC_LIBEVENT QUIET libevent)

# Look for the Libevent 2.0 or 1.4 headers
find_path(LIBEVENT_INCLUDE_DIR
  NAMES
    event2/event-config.h
    event-config.h
  PATHS /usr/include /usr/local/include
  HINTS
    ${PC_LIBEVENT_INCLUDE_DIRS}
)

if(LIBEVENT_INCLUDE_DIR)
  set(_version_regex "^#define[ \t]+_EVENT_VERSION[ \t]+\"([^\"]+)\".*")
  if(EXISTS "${LIBEVENT_INCLUDE_DIR}/event2/event-config.h")
    # Libevent 2.0
    file(STRINGS "${LIBEVENT_INCLUDE_DIR}/event2/event-config.h"
      LIBEVENT_VERSION REGEX "${_version_regex}")
  else()
    # Libevent 1.4
    file(STRINGS "${LIBEVENT_INCLUDE_DIR}/event-config.h"
      LIBEVENT_VERSION REGEX "${_version_regex}")
  endif()
  string(REGEX REPLACE "${_version_regex}" "\\1"
    LIBEVENT_VERSION "${LIBEVENT_VERSION}")
  unset(_version_regex)
endif()

set(_LIBEVENT_REQUIRED_VARS)
foreach(COMPONENT ${Libevent_FIND_COMPONENTS})
  set(_LIBEVENT_LIBNAME libevent)
  # Note: compare two variables to avoid a CMP0054 policy warning
  if(COMPONENT STREQUAL _LIBEVENT_LIBNAME)
    set(_LIBEVENT_LIBNAME event)
  else()
    set(_LIBEVENT_LIBNAME "event_${COMPONENT}")
  endif()
  string(TOUPPER "${COMPONENT}" COMPONENT_UPPER)
  find_library(LIBEVENT_${COMPONENT_UPPER}_LIBRARY
    NAMES ${_LIBEVENT_LIBNAME}
    PATHS /usr/lib /usr/local/lib
    HINTS ${PC_LIBEVENT_LIBRARY_DIRS}
  )
  if(LIBEVENT_${COMPONENT_UPPER}_LIBRARY)
    set(Libevent_${COMPONENT}_FOUND 1)
  endif()
  list(APPEND _LIBEVENT_REQUIRED_VARS LIBEVENT_${COMPONENT_UPPER}_LIBRARY)
endforeach()
unset(_LIBEVENT_LIBNAME)

include(FindPackageHandleStandardArgs)
# handle the QUIETLY and REQUIRED arguments and set LIBEVENT_FOUND to TRUE
# if all listed variables are TRUE and the requested version matches.
find_package_handle_standard_args(Libevent REQUIRED_VARS
                                  ${_LIBEVENT_REQUIRED_VARS}
                                  LIBEVENT_INCLUDE_DIR
                                  VERSION_VAR LIBEVENT_VERSION
                                  HANDLE_COMPONENTS)

if(LIBEVENT_FOUND)
  set(LIBEVENT_INCLUDE_DIRS  ${LIBEVENT_INCLUDE_DIR})
  set(LIBEVENT_LIBRARIES)
  foreach(COMPONENT ${Libevent_FIND_COMPONENTS})
    string(TOUPPER "${COMPONENT}" COMPONENT_UPPER)
    list(APPEND LIBEVENT_LIBRARIES ${LIBEVENT_${COMPONENT_UPPER}_LIBRARY})
    set(LIBEVENT_${COMPONENT_UPPER}_FOUND ${Libevent_${COMPONENT}_FOUND})
  endforeach()
endif()

mark_as_advanced(LIBEVENT_INCLUDE_DIR ${_LIBEVENT_REQUIRED_VARS})
unset(_LIBEVENT_REQUIRED_VARS)
