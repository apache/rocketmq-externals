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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

namespace RocketMQ.NetClient.Interop
{
    public enum LogLevel
    {
        None = 0,
        
        Fatal = 1,
        
        Error = 2,
        
        Warn = 3,
        
        Info = 4,
        
        Debug = 5,
        
        Trace = 6,
        
        LevelNum = 7
    }
    public enum CLogLevel
    {
        E_LOG_LEVEL_FATAL = 1,

        E_LOG_LEVEL_ERROR = 2,

        E_LOG_LEVEL_WARN = 3,

        E_LOG_LEVEL_INFO = 4,

        E_LOG_LEVEL_DEBUG = 5,

        E_LOG_LEVEL_TRACE = 6,

        E_LOG_LEVEL_LEVEL_NUM = 7
    }
}