# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

PROJECT(check-leak-native)
SET(${PROJECT_NAME}_MAJOR_VERSION 1)
SET(${PROJECT_NAME}_MINOR_VERSION 0)
SET(${PROJECT_NAME}_PATCH_LEVEL 0)

FIND_PACKAGE(Java)
FIND_PACKAGE(JNI)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

# You may want to adjust this next line for debugging. The -O3 is removed by default, since it would make debugging
# harder. Nonetheless, it can still be added by passing CMAKE_USER_C_FLAGS
# Also note that define the C99 as the minimum supported standard so the code can be compiled with older GCC versions
# (circa 4.4)
set(CMAKE_C_FLAGS_DEBUG "-Wall -std=gnu11 -z execstack -fdump-tree-all -Wall -pg -g ${CMAKE_USER_C_FLAGS}")
set(CMAKE_C_FLAGS "-O3 -std=gnu11 -Wall ${CMAKE_USER_C_FLAGS}")



set(CHECKLEAK_LIB_NAME checkleak-64)

message(${CMAKE_SYSTEM_NAME})

if (CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    if (CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
        set(CHECKLEAK_LIB_DIR platforms-lib/darwin-aarch64)
    else()
        set(CHECKLEAK_LIB_DIR platforms-lib/darwin-${CMAKE_SYSTEM_PROCESSOR})
    endif()
    set(CHECKLEAK_LIB_NAME checkleak)
else()
    if (CMAKE_SIZEOF_VOID_P EQUAL 4)
        set(CHECKLEAK_LIB_NAME checkleak-32)
    endif()
    if (CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64")
        set(CHECKLEAK_LIB_DIR platforms-lib/linux-amd64)
        set(CHECKLEAK_LIB_NAME checkleak)
    else()
        set(CHECKLEAK_LIB_DIR platforms-lib/linux-${CMAKE_SYSTEM_PROCESSOR})
    endif()
endif()

message ("variable is ${CMAKE_SYSTEM_NAME}")

find_library(LIBAIO_LIB NAMES aio)

INCLUDE_DIRECTORIES(. ${JNI_INCLUDE_DIRS})

ADD_LIBRARY(check-leak-native SHARED agent.c io_github_checkleak_core_CheckLeak.h)

set_target_properties(check-leak-native PROPERTIES
              LIBRARY_OUTPUT_DIRECTORY ../resources/${CHECKLEAK_LIB_DIR}
              LIBRARY_OUTPUT_NAME ${CHECKLEAK_LIB_NAME})
message(STATUS "Setting up library as ${CHECKLEAK_LIB_NAME} based on current architecture")
