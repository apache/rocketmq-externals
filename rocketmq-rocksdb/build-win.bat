/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

:: install git, java 8, maven, visual studio community 15 (2017)

set MSBUILD=C:\Program Files\Microsoft Visual Studio\2022\Community\MSBuild\Current\Bin\MSBuild.exe
set THIRDPARTY_HOME=D:\rocketmq-rocksdb

if exist build rd /s /q build
if exist librocksdbjni-win64.dll del librocksdbjni-win64.dll
mkdir build && cd build


cmake -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_STANDARD=20 -DJNI=1 -DSNAPPY=1 -DLZ4=1 -DZLIB=1 -DZSTD=1 -DXPRESS=1 ..
"%MSBUILD%" rocksdb.sln /p:Configuration=Release

cd ..

copy build\java\Release\rocksdbjni-shared.dll librocksdbjni-win64.dll
echo Result is in librocksdbjni-win64.dll
