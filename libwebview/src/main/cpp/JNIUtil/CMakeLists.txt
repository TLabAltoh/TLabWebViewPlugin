cmake_minimum_required(VERSION 3.10.1)

project("jni-util")

add_library(
        jni-util SHARED
        JNIUtil.cpp
)

find_library(android-lib android)
find_library(log-lib log)

set(INCLUDE_DIRECTORIES
        ${PROJECT_SOURCE_DIR}/../Unity
        ${PROJECT_SOURCE_DIR}/../LogUtil
)

include_directories("${INCLUDE_DIRECTORIES}")

target_link_libraries(
        jni-util
        GLESv2
        ${android-lib}
        ${log-lib}
)