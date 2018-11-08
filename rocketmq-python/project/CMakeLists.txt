#/*
#* Licensed to the Apache Software Foundation (ASF) under one or more
#* contributor license agreements.  See the NOTICE file distributed with
#* this work for additional information regarding copyright ownership.
#* The ASF licenses this file to You under the Apache License, Version 2.0
#* (the "License"); you may not use this file except in compliance with
#* the License.  You may obtain a copy of the License at
#*
#*     http://www.apache.org/licenses/LICENSE-2.0
#*
#* Unless required by applicable law or agreed to in writing, software
#* distributed under the License is distributed on an "AS IS" BASIS,
#* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#* See the License for the specific language governing permissions and
#* limitations under the License.
#*/
# source files
project(rocketmqclientpython)

file(GLOB_RECURSE SRC_FILES   ${CMAKE_SOURCE_DIR}/src/*)

# subdirs
SET(SUB_DIRS)
file(GLOB children ${CMAKE_SOURCE_DIR}/src/*)
FOREACH(child ${children})
	IF(IS_DIRECTORY ${child})
	    LIST(APPEND SUB_DIRS ${child})
	ENDIF()
ENDFOREACH()
LIST(APPEND SUB_DIRS ${CMAKE_SOURCE_DIR}/src)

# include_directories
include_directories(${CMAKE_SOURCE_DIR}/include)
include_directories(${SUB_DIRS})

# static
add_library(rocketmqclientpython_static STATIC ${SRC_FILES})
set_target_properties(rocketmqclientpython_static PROPERTIES OUTPUT_NAME "rocketmqclientpython")
#add_dependencies(rocketmqclientpython_static)
target_link_libraries(rocketmqclientpython_static ${deplibs})
target_link_libraries(rocketmqclientpython_static)

# shared
set(CMAKE_SHARED_LINKER_FLAGS "-fPIC -shared")
add_library(rocketmqclientpython_shared SHARED ${SRC_FILES})
set_target_properties(rocketmqclientpython_shared PROPERTIES OUTPUT_NAME "rocketmqclientpython")
#add_dependencies(rocketmqclientpython_shared)
target_link_libraries(rocketmqclientpython_shared ${deplibs})
target_link_libraries(rocketmqclientpython_shared)

# install
install (TARGETS   rocketmqclientpython_static             DESTINATION bin)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/include/ DESTINATION include)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/doc/     DESTINATION doc)

