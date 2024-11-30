cmake_minimum_required(VERSION 3.10.1)

project("glutil")

add_library(
        glutil SHARED
        glutil.cpp
)

find_library(android-lib android)
find_library(log-lib log)

set(INCLUDE_DIRECTORIES
        ${PROJECT_SOURCE_DIR}/../LogUtil
)

include_directories("${INCLUDE_DIRECTORIES}")

target_link_libraries(
        glutil
        GLESv3
        jnigraphics
        ${android-lib}
        ${log-lib}
)
