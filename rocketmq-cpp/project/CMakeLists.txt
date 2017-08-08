#<!***************************************************************************
#<!created:  2015-10-23 17:14:52
#<!filename: CMakeLists.txt
#<!author:   自修 yjlfuture@163.com(jianlin.yjl@alibaba-inc.com)
#<!breaf:
#<!***************************************************************************
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

# include_directories
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
add_dependencies(rocketmq_static mq_json mq_juce mq_log4cplus)
target_link_libraries(rocketmq_static ${deplibs})
target_link_libraries(rocketmq_static mq_json mq_juce mq_log4cplus)

# shared
set(CMAKE_SHARED_LINKER_FLAGS "-fPIC -shared")
add_library(rocketmq_shared SHARED ${SRC_FILES})
set_target_properties(rocketmq_shared PROPERTIES OUTPUT_NAME "rocketmq")
add_dependencies(rocketmq_shared mq_json mq_juce mq_log4cplus)
target_link_libraries(rocketmq_shared ${deplibs})
target_link_libraries(rocketmq_shared mq_json mq_juce mq_log4cplus)

# install
install (TARGETS   rocketmq_static             DESTINATION bin)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/include/ DESTINATION include)
install (DIRECTORY ${CMAKE_SOURCE_DIR}/doc/     DESTINATION doc)

