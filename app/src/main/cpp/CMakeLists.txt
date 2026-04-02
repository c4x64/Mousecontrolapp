cmake_minimum_required(VERSION 3.22.1)
project("mouselock")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -flto")

add_library(mouselock SHARED native-lib.cpp)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(mouselock
        ${log-lib}
        ${android-lib})