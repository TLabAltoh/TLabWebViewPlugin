cmake_minimum_required(VERSION 3.10.1)

project("project-root")

set(CMAKE_CXX_STANDARD 17)

add_subdirectory(native)
add_subdirectory(glutil)
add_subdirectory(SharedTexture)