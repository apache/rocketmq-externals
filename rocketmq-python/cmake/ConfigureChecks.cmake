
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include(CheckIncludeFiles)
include(CheckFunctionExists)
include(CheckLibraryExists)
include(CheckSymbolExists)
include(CheckTypeSize)
include(CheckCSourceCompiles)
include(CheckCXXSourceCompiles)

check_include_files(dlfcn.h  HAVE_DLFCN_H )

check_include_files(errno.h       HAVE_ERRNO_H )
check_include_files(iconv.h       HAVE_ICONV_H )
check_include_files(limits.h      HAVE_LIMITS_H )
check_include_files(sys/types.h   HAVE_SYS_TYPES_H )
check_include_files("sys/types.h;sys/socket.h"  HAVE_SYS_SOCKET_H )
check_include_files(sys/syscall.h HAVE_SYS_SYSCALL_H )
check_include_files("sys/types.h;sys/time.h"    HAVE_SYS_TIME_H )
check_include_files("sys/types.h;sys/timeb.h"   HAVE_SYS_TIMEB_H )
check_include_files("sys/types.h;sys/stat.h"    HAVE_SYS_STAT_H )
check_include_files(sys/file.h    HAVE_SYS_FILE_H )
check_include_files(syslog.h      HAVE_SYSLOG_H )
check_include_files(arpa/inet.h   HAVE_ARPA_INET_H )
check_include_files(netinet/in.h  HAVE_NETINET_IN_H )
check_include_files("sys/types.h;netinet/tcp.h" HAVE_NETINET_TCP_H )
check_include_files(netdb.h       HAVE_NETDB_H )
check_include_files(unistd.h      HAVE_UNISTD_H )
check_include_files(fcntl.h       HAVE_FCNTL_H )
check_include_files(stdio.h       HAVE_STDIO_H )
check_include_files(stdarg.h      HAVE_STDARG_H )
check_include_files(stdlib.h      HAVE_STDLIB_H )
check_include_files(time.h        HAVE_TIME_H )
check_include_files(wchar.h       HAVE_WCHAR_H )
check_include_files(poll.h        HAVE_POLL_H )


check_include_files(inttypes.h    HAVE_INTTYPES_H )
check_include_files(memory.h      HAVE_MEMORY_H )
check_include_files(stdint.h      HAVE_STDINT_H )
check_include_files(strings.h     HAVE_STRINGS_H )
check_include_files(string.h      HAVE_STRING_H )


check_include_files("stdlib.h;stdio.h;stdarg.h;string.h;float.h" STDC_HEADERS )

find_library(LIBADVAPI32 advapi32)
find_library(LIBKERNEL32 kernel32)
find_library(LIBNSL      nsl)
find_library(LIBRT       rt)
find_library(LIBICONV iconv)
find_library(LIBPOSIX4 posix4)
find_library(LIBCPOSIX cposix)
find_library(LIBSOCKET socket)
find_library(LIBWS2_32 ws2_32)

check_function_exists(gmtime_r      HAVE_GMTIME_R )
check_function_exists(localtime_r   HAVE_LOCALTIME_R )
check_function_exists(gettimeofday  HAVE_GETTIMEOFDAY )
check_function_exists(getpid        HAVE_GETPID )
check_function_exists(poll          HAVE_POLL )
check_function_exists(pipe          HAVE_PIPE )
check_function_exists(pipe2         HAVE_PIPE2 )
check_function_exists(ftime         HAVE_FTIME )
check_function_exists(stat          HAVE_STAT )
check_function_exists(lstat         HAVE_LSTAT )
check_function_exists(fcntl         HAVE_FCNTL )
check_function_exists(lockf         HAVE_FLOCK )
check_function_exists(flock         HAVE_LOCKF )
check_function_exists(htons         HAVE_HTONS )
check_function_exists(ntohs         HAVE_NTOHS )
check_function_exists(htonl         HAVE_HTONL )
check_function_exists(ntohl         HAVE_NTOHL )
check_function_exists(shutdown      HAVE_SHUTDOWN )
check_function_exists(vsnprintf     HAVE_VSNPRINTF )
check_function_exists(_vsnprintf    HAVE__VSNPRINTF )
check_function_exists(vsprintf_s    HAVE_VSPRINTF_S )
check_function_exists(vswprintf_s   HAVE_VSWPRINTF_S )
check_function_exists(vfprintf_s    HAVE_VFPRINTF_S )
check_function_exists(vfwprintf_s   HAVE_VFWPRINTF_S )
check_function_exists(_vsnprintf_s  HAVE__VSNPRINTF_S )
check_function_exists(_vsnwprintf_s HAVE__VSNWPRINTF_S )
check_function_exists(mbstowcs      HAVE_MBSTOWCS )
check_function_exists(wcstombs      HAVE_WCSTOMBS )


check_symbol_exists(ENAMETOOLONG          errno.h       HAVE_ENAMETOOLONG )
check_symbol_exists(SYS_gettid            sys/syscall.h HAVE_GETTID )
check_symbol_exists(__FUNCTION__          ""            HAVE_FUNCTION_MACRO )
check_symbol_exists(__PRETTY_FUNCTION__   ""            HAVE_PRETTY_FUNCTION_MACRO )
check_symbol_exists(__func__              ""            HAVE_FUNC_SYMBOL )

check_c_source_compiles("#include <stdlib.h> \n int main() { int x = 1; int y = __sync_add_and_fetch (&x, 1); return y;}"
                        HAVE___SYNC_ADD_AND_FETCH )

check_c_source_compiles("#include <stdlib.h> \n int main() { int x = 1; int y = __sync_sub_and_fetch (&x, 1); return y;}"
                        HAVE___SYNC_SUB_AND_FETCH )

check_c_source_compiles("#include <stdio.h>\n #define MACRO(buf, args...) (sprintf (buf, \"%d\", args))\n int main() {char a[10]; MACRO(a, 1); return 0; }"
                        HAVE_GNU_VARIADIC_MACROS )

check_c_source_compiles("#include <stdio.h>\n #define MACRO(buf, ...) (sprintf (buf, \"%d\",  __VA_ARGS__))\n int main() {char a[10]; MACRO(a, 1); return 0; }"
                        HAVE_C99_VARIADIC_MACROS )


# clock_gettime() needs -lrt here
# TODO AC says this exists
if (LIBRT)
  check_library_exists("${LIBRT}" clock_gettime ""
    HAVE_CLOCK_GETTIME )
  check_library_exists("${LIBRT}" clock_nanosleep ""
    HAVE_CLOCK_NANOSLEEP )
  check_library_exists("${LIBRT}" nanosleep ""
    HAVE_NANOSLEEP )
else ()
  check_function_exists(clock_gettime HAVE_CLOCK_GETTIME )
  check_function_exists(clock_nanosleep HAVE_CLOCK_NANOSLEEP )
  check_function_exists(nanosleep HAVE_NANOSLEEP )
endif ()

# iconv functions may require iconv library (on OS X for example)
if(WITH_ICONV)
  if(LIBICONV)
    check_library_exists("${LIBICONV}" iconv_open  "" HAVE_ICONV_OPEN )
    check_library_exists("${LIBICONV}" iconv_close "" HAVE_ICONV_CLOSE )
    check_library_exists("${LIBICONV}" iconv       "" HAVE_ICONV )
  else()
    check_function_exists(iconv_open  HAVE_ICONV_OPEN )
    check_function_exists(iconv_close HAVE_ICONV_CLOSE )
    check_function_exists(iconv       HAVE_ICONV )
  endif()
endif()

check_function_exists(gethostbyname_r HAVE_GETHOSTBYNAME_R) # TODO more complicated test in AC
check_function_exists(getaddrinfo     HAVE_GETADDRINFO ) # TODO more complicated test in AC


# check for declspec stuff
if(NOT DEFINED LOG4CPLUS_DECLSPEC_EXPORT)
  check_c_source_compiles(
    "#if defined (__GNUC__) && (__GNUC__ < 4 || (__GNUC__ == 4 && __GNUC_MINOR__ <= 1))
     # error Please fail.
     #endif

     __attribute__((visibility(\"default\"))) int x = 0;
     __attribute__((visibility(\"default\"))) int foo();
     int foo() { return 0; }
     __attribute__((visibility(\"default\"))) int bar() { return x; }
     __attribute__((visibility(\"hidden\"))) int baz() { return 1; }

     int main(void) { return 0; }"
   HAVE_ATTRIBUTE_VISIBILITY
  )
  if(HAVE_ATTRIBUTE_VISIBILITY)
    set(LOG4CPLUS_DECLSPEC_EXPORT "__attribute__ ((visibility(\"default\")))" )
    set(LOG4CPLUS_DECLSPEC_IMPORT "__attribute__ ((visibility(\"default\")))" )
    set(LOG4CPLUS_DECLSPEC_PRIVATE "__attribute__ ((visibility(\"hidden\")))" )
  endif()
endif()

if(NOT DEFINED LOG4CPLUS_DECLSPEC_EXPORT)
  check_c_source_compiles(
    "#if defined (__clang__)
     // Here the problem is that Clang only warns that it does not support
     // __declspec(dllexport) but still compiles the executable.
     # error Please fail.
     #endif

     __declspec(dllexport) int x = 0;
     __declspec(dllexport) int foo ();
     int foo () { return 0; }
     __declspec(dllexport) int bar () { return x; }

     int main(void) { return 0; }"
    HAVE_DECLSPEC_DLLEXPORT
  )
  if(HAVE_DECLSPEC_DLLEXPORT)
    set(LOG4CPLUS_DECLSPEC_EXPORT "__declspec(dllexport)" )
    set(LOG4CPLUS_DECLSPEC_IMPORT "__declspec(dllimport)" )
    set(LOG4CPLUS_DECLSPEC_PRIVATE "" )
  endif()
endif()

if(NOT DEFINED LOG4CPLUS_DECLSPEC_EXPORT)
  check_c_source_compiles(
    "__global int x = 0;
     __global int foo();
     int foo() { return 0; }
     __global int bar() { return x; }
     __hidden int baz() { return 1; }

     int main(void) { return 0; }"
    HAVE_GLOBAL_AND_HIDDEN
  )
  if(HAVE_GLOBAL_AND_HIDDEN)
    set(LOG4CPLUS_DECLSPEC_EXPORT "__global" )
    set(LOG4CPLUS_DECLSPEC_IMPORT "__global" )
    set(LOG4CPLUS_DECLSPEC_PRIVATE "__hidden" )
  endif()
endif()

if(NOT DEFINED LOG4CPLUS_DECLSPEC_EXPORT OR NOT ENABLE_SYMBOLS_VISIBILITY)
  set(LOG4CPLUS_DECLSPEC_EXPORT "")
  set(LOG4CPLUS_DECLSPEC_IMPORT "")
  set(LOG4CPLUS_DECLSPEC_PRIVATE "")
endif()

# check for thread-local stuff
if(NOT DEFINED LOG4CPLUS_HAVE_TLS_SUPPORT)
  # TODO: requires special compiler switch on GCC and Clang
  # Currently it is assumed that they are provided in
  # CMAKE_CXX_FLAGS
  set(CMAKE_REQUIRED_FLAGS "${CMAKE_CXX_FLAGS}")
  check_cxx_source_compiles(
    "extern thread_local int x;
     thread_local int * ptr = 0;
     int foo() { ptr = &x; return x; }
     thread_local int x = 1;

     int main()
     {
         x = 2;
         foo();
         return 0;
     }"
    HAVE_CXX11_THREAD_LOCAL
  )
  set(CMAKE_REQUIRED_FLAGS "")
  if(HAVE_CXX11_THREAD_LOCAL)
    set(LOG4CPLUS_HAVE_TLS_SUPPORT 1)
    set(LOG4CPLUS_THREAD_LOCAL_VAR "thread_local")
  endif()
endif()

if(NOT DEFINED LOG4CPLUS_HAVE_TLS_SUPPORT)
  check_cxx_source_compiles(
    "#if defined (__NetBSD__)
     #include <sys/param.h>
     #if ! __NetBSD_Prereq__(5,1,0)
     #error NetBSD __thread support does not work before 5.1.0. It is missing __tls_get_addr.
     #endif
     #endif

     extern __thread int x;
     __thread int * ptr = 0;
     int foo() { ptr = &x; return x; }
     __thread int x = 1;

     int main()
     {
         x = 2;
         foo();
         return 0;
     }"
    HAVE_GCC_THREAD_EXTENSION
  )
  if(HAVE_GCC_THREAD_EXTENSION)
    set(LOG4CPLUS_HAVE_TLS_SUPPORT 1)
    set(LOG4CPLUS_THREAD_LOCAL_VAR "__thread")
  endif()
endif()

if(NOT DEFINED LOG4CPLUS_HAVE_TLS_SUPPORT)
  check_cxx_source_compiles(
    "#if defined (__GNUC__)
     #error Please fail.
     #endif

     extern __declspec(thread) int x;
     __declspec(thread) int * ptr = 0;
     int foo() { ptr = &x; return x; }
     __declspec(thread) int x = 1;

     int main()
     {
         x = 2;
         foo();
         return 0;
     }"
    HAVE_DECLSPEC_THREAD
  )
  if(HAVE_DECLSPEC_THREAD)
    set(LOG4CPLUS_HAVE_TLS_SUPPORT 1)
    set(LOG4CPLUS_THREAD_LOCAL_VAR "__declspec(thread)")
  endif()
endif()

# check for c++11 atomic stuff
# TODO: requires special compiler switch on GCC and Clang
# Currently it is assumed that they are provided in
# CMAKE_CXX_FLAGS
set(CMAKE_REQUIRED_FLAGS "${CMAKE_CXX_FLAGS}")
check_cxx_source_compiles(
  "#include <atomic>

   template<typename T>
   void test_atomic()
   {
       std::atomic<T> x(0);
       std::atomic_fetch_add_explicit(&x, static_cast<T>(1), std::memory_order_acquire);
       std::atomic_fetch_sub_explicit(&x, static_cast<T>(1), std::memory_order_release);
   }

   int main()
   {
       test_atomic<int>();
       test_atomic<unsigned int>();
       test_atomic<long>();
       test_atomic<unsigned long>();
       std::atomic_thread_fence(std::memory_order_acquire);
       return 0;
   }"
  LOG4CPLUS_HAVE_CXX11_ATOMICS
)
set(CMAKE_REQUIRED_FLAGS "")

set(CMAKE_EXTRA_INCLUDE_FILES sys/socket.h)
check_type_size(socklen_t _SOCKLEN_SIZE)
if (_SOCKLEN_SIZE)
  set(socklen_t)
else()
  set(socklen_t TRUE)
endif()

macro(PATH_TO_HAVE _pathVar )
  if (${_pathVar})
    set(HAVE_${_pathVar} TRUE)
  else ()
    set(HAVE_${_pathVar} FALSE)
  endif ()
endmacro()


path_to_have(LIBADVAPI32)
path_to_have(LIBKERNEL32)
path_to_have(LIBNSL)
path_to_have(LIBRT)
path_to_have(LIBPOSIX4)
path_to_have(LIBCPOSIX)
path_to_have(LIBSOCKET)
path_to_have(LIBWS2_32)



