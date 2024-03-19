# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("amprack")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
# libsndfile static
add_library(libsndfile STATIC IMPORTED)
set_target_properties( # Specifies the target library.
        libsndfile

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libsndfile.a )
add_library(libopus STATIC IMPORTED)
set_target_properties( # Specifies the target library.
        libopus

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopus.a )

add_library(libopusenc STATIC IMPORTED)
add_library(libap4 STATIC IMPORTED)

set_target_properties( # Specifies the target library.
        libopusenc

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopusenc.a )

set_target_properties( # Specifies the target library.
        libap4

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libap4.a )

add_library(libmp3lame STATIC IMPORTED)
add_library(libfaac STATIC IMPORTED)
set_target_properties( # Specifies the target library.
        libmp3lame

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmp3lame.a )

set_target_properties( # Specifies the target library.
        libfaac

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libfaac.a )


add_library( # Sets the name of the library.
        amprack

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        vringbuffer.c
        ringbuffer.c
        native-lib.cpp
        Engine.cpp
        Export.cpp
        upwaker.c
        FileWriter.cpp
        FullDuplexStream.cpp
        SharedLibrary.cpp
        Plugin.cpp
        PluginControl.cpp
        Plugin2.cpp
        Meter.cpp
        LockFreeQueue.cpp
        MP4.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib
        mediandk
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
find_package (oboe REQUIRED CONFIG)

target_link_libraries( # Specifies the target library.
        amprack
        oboe::oboe
        libsndfile
        libopusenc
        libopus
        libmp3lame
        libfaac
        mediandk
        android
        OpenMAXAL
        log

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

target_include_directories(amprack PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
add_library(camera_textureview SHARED
        android_main.cpp
        camera_manager.cpp
        camera_engine.cpp
        camera_listeners.cpp
        camera_utils.cpp)

# add include path
target_include_directories(camera_textureview PRIVATE ${COMMON_SOURCE_DIR})

# add lib dependencies
target_link_libraries(camera_textureview dl android log m camera2ndk mediandk)
