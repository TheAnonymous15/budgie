cmake_minimum_required(VERSION 3.22.1)
project("budgie_llama")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_library(log-lib log)

# Create shared library for JNI bindings
add_library(budgie_llama SHARED
    llama_jni.cpp
)

# Link against pre-built llama library
target_link_libraries(budgie_llama
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-base.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-cpu.so
    ${log-lib}
)

# Include directories
target_include_directories(budgie_llama PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

