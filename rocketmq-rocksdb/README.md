# RocketMQ-RocksDB Release Process

## Summary

RocketMQ-RocksDB was introduced in RIP-66, and this document mainly introduces how to release related JAR.

rocketmq-rocksdb releases are a fat jar file that contain the following binaries:
* .so files for linux32 (glibc and musl-libc)
* .so files for linux64 (glibc and musl-libc)
* .so files for linux [aarch64](https://en.wikipedia.org/wiki/AArch64) (glibc and musl-libc)
* .so files for linux [ppc64le](https://en.wikipedia.org/wiki/Ppc64le) (glibc and musl-libc)
* .jnilib file for Mac OSX
* .dll for Windows x64

To build the binaries for a rocketmq-rocksdb release, follow the steps below.

## Git Clone RocksDB
git clone https://github.com/facebook/rocksdb.git

## File copy
* copy RemoveConsumeQueueCompactionFilter.java    to  rocksdb/java/src/main/java/org/rocksdb/ 
* copy remove_consumequeue_compactionfilterjni.cc to  rocksdb/java/rocksjni/ 
* copy remove_consumequeue_compactionfilter.h     to  rocksdb/utilities/compaction_filters/ 
* copy remove_consumequeue_compactionfilter.cc    to  rocksdb/utilities/compaction_filters/

## File replace
* replace rocksdb/thirdparty.inc with thirdparty.inc with 

## File edit
* add the string  "rocksjni/remove_consumequeue_compactionfilterjni.cc"                            to the file rocksdb/java/CMakeLists.txt 
* add the string  "src/main/java/org/rocksdb/RemoveConsumeQueueCompactionFilter.java"              to the file rocksdb/java/CMakeLists.txt 
* add the string  "org.rocksdb.RemoveConsumeQueueCompactionFilter"                                 to the file rocksdb/java/CMakeLists.txt
* add the string  "org.rocksdb.RemoveConsumeQueueCompactionFilter\"                                to the file rocksdb/java/Makefile 
* add the string  "utilities/compaction_filters/remove_consumequeue_compactionfilter.cc    \"      to the file rocksdb/src.mk 
* add the string  "java/rocksjni/remove_consumequeue_compactionfilterjni.cc      \"                to the file rocksdb/src.mk 
* add the string  '"utilities/compaction_filters/remove_consumequeue_compactionfilter.cc",'        to the file rocksdb/TARGETS

## File edit [optional] 
Some unexpected error may occur in compiling, you may change the warning level of compiling
* change 'set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /WX")'      in file rocksdb/CMakeLists.txt to   'set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")'
* change 'set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror")'  in file rocksdb/CMakeLists.txt to   'set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")'

## File edit [important] 
it is important to change the DEBUG_LEVEL to 0 for releasing a official version
* change 'DEBUG_LEVEL?=1' to 'DEBUG_LEVEL?=0' in file rocksdb/Makefile


## Build for linux and mac
Both mac compilation and linux compilation are performed on the mac system, but we finish compilation on linux by using the docker image provided by rocksdb.
There is one more thing to note here, the docker images downloaded in the Makefile execution script may be out of date due to its jdk version, resulting in failure in the end.
For example, the docker image evolvedbinary/rocksjava:centos6_x86-be was last updated 4 years ago, and the system image uses jdk1.7, while the minimum jdk required for the current rocksdb compilation is 1.8.

Fortunately, rocksdb has its own docker warehouse, which can build a local image by itself, which can avoid downloading images that have not been updated for a long time from the official website of rocksdb. 
The warehouse address is as follows: https://github.com/evolvedbinary/docker-rocksjava

              git clone https://github.com/evolvedbinary/docker-rocksjava
 
You can compile the docker images required in the Makefile by yourself, and the compilation script is in the README file.
In addition, it may be necessary to modify the url in the Dockerfile of some operating systems,

            http://mirrors.aliyun.com/alpine/v3.13/main" > /etc/apk/repositories \
                && echo "http://mirrors.aliyun.com/alpine/v3.13/community" >> /etc/apk/repositories \
                && apk update \
                && apk update

It is very important to make the linux docker image by yourself. 
If you let the RocksDB compilation script download the linux compilation environment, not only will the process be very slow, but the downloaded linux compilation environment may even be out of date.
So please carefully read the readme of the docker-rocksjava warehouse and make the linux docker image by yourself. 
The linux image we need is as follows:

               apline3_x86-be
               apline3_x64-be
               centos6_x64-be
               centos6_x86-be
               centos7_ppc64le-be

After all linux docker images are ready, run this make command from RocksDB's root source directory:

    make jclean clean rocksdbjavastaticreleasedocker

This command will build RocksDB natively on OSX, and will then spin up docker containers to build RocksDB for 32-bit and 64-bit Linux with glibc, and 32-bit and 64-bit Linux with musl libc.

You can find all native binaries and JARs in the java/target directory upon completion:

    librocksdbjni-linux32.so
    librocksdbjni-linux64.so
    librocksdbjni-linux64-musl.so
    librocksdbjni-linux32-musl.so
    librocksdbjni-osx.jnilib
    rocksdbjni-x.y.z-javadoc.jar
    rocksdbjni-x.y.z-linux32.jar
    rocksdbjni-x.y.z-linux64.jar
    rocksdbjni-x.y.z-linux64-musl.jar
    rocksdbjni-x.y.z-linux32-musl.jar
    rocksdbjni-x.y.z-osx.jar
    rocksdbjni-x.y.z-sources.jar
    rocksdbjni-x.y.z.jar

Where x.y.z is the built version number of RocksDB.

And what we need is:
           
              rocksdbjni-x.y.z-javadoc.jar
              rocksdbjni-x.y.z-sources.jar
              rocksdbjni-x.y.z.jar


## Build for Windows
The Windows system needs to be compiled independently, please follow the steps below.
Need to install:
1. git
2. jdk 1.8
3. maven
4. visual studio
5. msbuild

Unlike linux or mac systems, the compilation script will automatically download and compile several compression algorithm warehouses like snappy, lz4. 
In the Windows environment, you need to download and compile by yourself. For details, please refer to the document:
 
              http://rocksdb.org.cn/doc/Building-on-Windows.html
 
Finally, run this make command from RocksDB's root source directory:

                    set THIRDPARTY_HOME=[please input RocksDB's root source directory]
                    set MSBUILD=C:\Program Files\Microsoft Visual Studio\2022\Community\MSBuild\Current\Bin\MSBuild.exe
                    cmake -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_STANDARD=20 -DJNI=1 -DSNAPPY=1 -DLZ4=1 -DZLIB=1 -DZSTD=1 -DXPRESS=1 ..
                    "%MSBUILD%" rocksdb.sln /p:Configuration=Release

The resulting native binary will be built and available at `build\java\Release\rocksdbjni-shared.dll`. You can also find it under project folder with name `librocksdbjni-win64.dll`.


## Copy `librocksdbjni-win64.dll` to `rocksdbjni-x.y.z.jar`

    jar -uf rocksdbjni-x.y.z.jar librocksdbjni-win64.dll

Finally, we use the following file to release an official rocksdb release for rocketmq:

              rocksdbjni-x.y.z-javadoc.jar
              rocksdbjni-x.y.z-sources.jar
              rocksdbjni-x.y.z.jar

## Release

```shell

mvn -X --settings settings.xml  gpg:sign-and-deploy-file -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -DrepositoryId=apache.releases.https  -DpomFile=pom.xml -Dfile=rocksdbjni-8.4.0.jar -Dclassifier=  -Dgpg.keytype=RSA -Dgpg.passphrase=xxxxx

mvn -X --settings settings.xml  gpg:sign-and-deploy-file -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -DrepositoryId=apache.releases.https  -DpomFile=pom.xml -Dfile=rocksdbjni-8.4.0-javadoc.jar -Dclassifier=javadoc  -Dgpg.keytype=RSA -Dgpg.passphrase=xxxxx

mvn -X --settings settings.xml  gpg:sign-and-deploy-file -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 -DrepositoryId=apache.releases.https  -DpomFile=pom.xml -Dfile=rocksdbjni-8.4.0-sources.jar -Dclassifier=sources  -Dgpg.keytype=RSA -Dgpg.passphrase=xxxxx

```