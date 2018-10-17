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

# source files
project(rocketmq4cpp)

file(GLOB_RECURSE SRC_FILES   ${CMAKE_SOURCE_DIR}/src/*)
list(REMOVE_ITEM  SRC_FILES   ${CMAKE_SOURCE_DIR}/src/dllmain.cpp)

# subdirs
SET(SUB_DIRS)
file(GLOB children ${CMAKE_SOURCE_DIR}/src/*)
FOREACH(child ${children})
	IF(IS_DIRECTORY ${child})
	    LIST(APPEND SUB_DIRS ${child})
	ENDIF()
ENDFOREACH()
LIST(APPEND SUB_DIRS ${CMAKE_SOURCE_DIR}/src)

include_directories(${CMAKE_SOURCE_DIR}/include)
include_directories(${SUB_DIRS})

# libs_directories
file(GLOB LIB_DIRS ${CMAKE_SOURCE_DIR}/libs/*)
foreach(dir ${LIB_DIRS})
    if(IS_DIRECTORY ${dir})
        set(CMAKE_PREFIX_PATH ${CMAKE_PREFIX_PATH};${dir})
        include_directories(${dir}/include)
    endif()
endforeach()

# static
add_library(rocketmq_static STATIC ${SRC_FILES})
set_target_properties(rocketmq_static PROPERTIES OUTPUT_NAME "rocketmq")
add_dependencies(rocketmq_static Signature)
target_link_libraries(rocketmq_static ${deplibs})
target_link_libraries(rocketmq_static Signature)

# shared
set(CMAKE_SHARED_LINKER_FLAGS "-DBOOST_ALL_DYN_LINK -shared ")
add_library(rocketmq_shared SHARED ${SRC_FILES})
set_target_properties(rocketmq_shared PROPERTIES OUTPUT_NAME "rocketmq")
add_dependencies(rocketmq_shared Signature)
target_link_libraries(rocketmq_shared ${deplibs})
target_link_libraries(rocketmq_shared  Signature)
target_link_libraries(rocketmq_shared  jsoncpp)
target_link_libraries(rocketmq_shared  ${LIBEVENT_LIBRARIES})
target_link_libraries(rocketmq_shared  ${Boost_LIBRARIES})


# install
install (TARGETS   rocketmq_static             DESTINATION lib)
install (TARGETS   rocketmq_shared             DESTINATION lib)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/include/ DESTINATION include/rocketmq)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/doc/     DESTINATION doc)
