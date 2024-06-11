cmake_minimum_required(VERSION 3.10.1)

project("shared-texture")

add_library(
        shared-texture SHARED
        SharedTexture.cpp
        JSharedTexture.cpp
        RenderingPlugin.cpp
        RenderAPI.cpp
        RenderAPI_Vulkan.cpp
        RenderAPI_OpenGLCoreES.cpp
)

find_library(android-lib android)
find_library(log-lib log)

set(INCLUDE_DIRECTORIES
        ${PROJECT_SOURCE_DIR}/../Unity
        ${PROJECT_SOURCE_DIR}/../LogUtil
)

include_directories("${INCLUDE_DIRECTORIES}")

target_link_libraries(
        shared-texture
        GLESv2
        EGL
        jnigraphics
        ${android-lib}
        ${log-lib}
)