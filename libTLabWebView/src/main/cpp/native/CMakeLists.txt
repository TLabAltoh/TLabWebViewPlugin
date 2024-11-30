cmake_minimum_required(VERSION 3.10.1)

project("jni-libTLabWebView")

add_library(
        jni-libTLabWebView SHARED
        native.cpp
)

find_library(android-lib android)
find_library(log-lib log)

set(INCLUDE_DIRECTORIES
        ${PROJECT_SOURCE_DIR}/../Unity
        ${PROJECT_SOURCE_DIR}/../LogUtil
)

include_directories("${INCLUDE_DIRECTORIES}")

target_link_libraries(
        jni-libTLabWebView
        GLESv2
        ${android-lib}
        ${log-lib}
)