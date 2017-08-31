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
# This module defines
#  JSONCPP_INCLUDE_DIR, where to find header, etc.
#  JSONCPP_LIBRARY, the libraries needed to use jsoncpp.
#  JSONCPP_FOUND, If false, do not try to use jsoncpp.
#  JSONCPP_INCLUDE_PREFIX, include prefix for jsoncpp.
#  jsoncpp_lib_static imported library.

# only look in default directories
find_path(
	JSONCPP_INCLUDE_DIR
	NAMES json/json.h jsoncpp/json/json.h
    PATHS /usr/include/jsoncp /usr/local/include/jsoncpp 
    DOC "jsoncpp include dir"
)

find_library(
    JSONCPP_LIBRARY
    NAMES jsoncpp
    PATHS /usr/lib /usr/local/lib
    DOC "jsoncpp library"
)

add_library(jsoncpp_lib_static UNKNOWN IMPORTED)
set_target_properties(
	jsoncpp_lib_static
	PROPERTIES
	IMPORTED_LOCATION "${JSONCPP_LIBRARY}"
	INTERFACE_INCLUDE_DIRECTORIES "${JSONCPP_INCLUDE_DIR}"
)

# debug library on windows
# same naming convention as in qt (appending debug library with d)
# boost is using the same "hack" as us with "optimized" and "debug"
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
	find_library(
		JSONCPP_LIBRARY_DEBUG
		NAMES jsoncppd
		DOC "jsoncpp debug library"
	)

	set_target_properties(
		jsoncpp_lib_static
		PROPERTIES
		IMPORTED_LOCATION_DEBUG "${JSONCPP_LIBRARY_DEBUG}"
	)
	set(JSONCPP_LIBRARY optimized ${JSONCPP_LIBRARY} debug ${JSONCPP_LIBRARY_DEBUG})

endif()

# find JSONCPP_INCLUDE_PREFIX
find_path(
    JSONCPP_INCLUDE_PREFIX
    NAMES json.h
    PATH_SUFFIXES jsoncpp/json json
)

if (${JSONCPP_INCLUDE_PREFIX} MATCHES "jsoncpp")
    set(JSONCPP_INCLUDE_PREFIX "jsoncpp/json")
else()
    set(JSONCPP_INCLUDE_PREFIX "json")
endif()



# handle the QUIETLY and REQUIRED arguments and set JSONCPP_FOUND to TRUE
# if all listed variables are TRUE, hide their existence from configuration view
include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(jsoncpp DEFAULT_MSG JSONCPP_INCLUDE_DIR JSONCPP_LIBRARY)
mark_as_advanced (JSONCPP_INCLUDE_DIR JSONCPP_LIBRARY)
