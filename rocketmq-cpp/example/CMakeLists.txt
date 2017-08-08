project(example)

set(EXECUTABLE_OUTPUT_PATH ${CMAKE_SOURCE_DIR}/bin)

include_directories(${CMAKE_SOURCE_DIR}/include)
link_directories(${CMAKE_SOURCE_DIR}/bin)

# subdirs
file(GLOB children ${CMAKE_SOURCE_DIR}/src/*)
FOREACH(child ${children})
	IF(IS_DIRECTORY ${child})
        include_directories(${child})
	ENDIF()
ENDFOREACH()

# libs_directories
file(GLOB LIB_DIRS ${CMAKE_SOURCE_DIR}/libs/*)
foreach(dir ${LIB_DIRS})
    if(IS_DIRECTORY ${dir})
        include_directories(${dir}/include)
    endif()
endforeach()

# librocketmq.a
# find_library(ROCKETMQ_LIBRARY NAMES rocketmq
# 			 PATHS ${CMAKE_SOURCE_DIR}/bin)

file(GLOB files "*.cpp")
foreach(file ${files})
    get_filename_component(basename ${file} NAME_WE)
    # 指定生成目标
    add_executable(${basename} ${file})
    target_link_libraries (${basename}  rocketmq_static ${deplibs})
endforeach()
