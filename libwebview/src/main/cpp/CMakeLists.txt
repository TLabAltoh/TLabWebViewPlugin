# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.10.1)

# Note
# You have to pass ninja.exe on the PATH or you'll get an error!
# download link : https://github.com/ninja-build/ninja

project("shared-texture")

add_library(
        shared-texture SHARED
        SharedTexture.cpp
        JSharedTexture.cpp
        )

find_library(android-lib android)
find_library(log-lib log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(
        shared-texture
        GLESv2
        EGL
        jnigraphics
        ${android-lib}
        ${log-lib}
        )