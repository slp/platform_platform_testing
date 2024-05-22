cmake_minimum_required(VERSION 3.22.1)

project("ndkTest")

add_executable(
        ndkTest
        src/native-sample.cpp
)

find_library( # Sets the name of the path variable.
        log-lib
        log)
