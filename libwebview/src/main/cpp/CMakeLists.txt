cmake_minimum_required(VERSION 3.10.1)

project("shared-texture")

add_library(
        shared-texture SHARED
        SharedTexture.cpp
        JSharedTexture.cpp
)

add_library(
        jni-util SHARED
        JNIUtil.cpp
)

find_library(android-lib android)
find_library(log-lib log)

include_directories(Unity)

target_link_libraries(
        shared-texture
        GLESv2
        EGL
        jnigraphics
        ${android-lib}
        ${log-lib}
)

target_link_libraries(
        jni-util
        GLESv2
        ${android-lib}
        ${log-lib}
)