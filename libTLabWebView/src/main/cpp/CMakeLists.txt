cmake_minimum_required(VERSION 3.10.1)

project("project-root")

set(CMAKE_CXX_STANDARD 17)

add_subdirectory(native)
add_subdirectory(glutil)
add_subdirectory(SharedTexture)

target_link_options("jni-libTLabWebView" PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options("glutil" PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options("shared-texture" PRIVATE "-Wl,-z,max-page-size=16384")