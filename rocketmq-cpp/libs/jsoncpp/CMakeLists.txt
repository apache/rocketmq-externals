#<!***************************************************************************
#<!created:  2015-10-23 17:15:28
#<!filename: CMakeLists.txt
#<!author:   自修 yjlfuture@163.com(jianlin.yjl@alibaba-inc.com)
#<!breaf:
#<!***************************************************************************
project(mq_json)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
set(LIBRARY_OUTPUT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/lib)

aux_source_directory(src/  DIR_LIB_SRCS)

add_library(mq_json STATIC ${DIR_LIB_SRCS})
target_link_libraries(mq_json ${deplibs})
set_target_properties(mq_json PROPERTIES OUTPUT_NAME "mq_json")
