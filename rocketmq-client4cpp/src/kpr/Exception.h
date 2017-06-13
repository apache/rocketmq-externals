/**
 * Copyright (C) 2013 kangliqiang ,kangliq@163.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __KPR_EXCEPTION_H
#define __KPR_EXCEPTION_H

#include "KPRTypes.h"
#include <exception>
#include <string>
#include <sstream>

namespace kpr
{
class Exception : public std::exception
{
public:
    Exception(const char* msg, int error, const char* file, int line)throw()
        : m_error(error), m_line(line), m_msg(msg), m_file(file)
    {
        try
        {
            std::stringstream ss;
            ss << "[" << file << ":" << line << "]|error: " << error << "|msg:" << msg;
            m_msg = ss.str();
        }
        catch (...)
        {
        }
    }

    virtual ~Exception()throw()
    {
    }

    const char* what() const throw()
    {
        return m_msg.c_str();
    }

    int GetError() const throw()
    {
        return m_error;
    }

    virtual const char* GetType() const throw()
    {
        return "Exception";
    }

protected:
    int m_error;
    int m_line;
    std::string m_msg;
    std::string m_file;
};
}

inline std::ostream& operator<<(std::ostream& os, const kpr::Exception& e)
{
    os << "Type:" << e.GetType() <<  e.what();
    return os;
}

#define DEFINE_EXCEPTION(name) \
    class name : public kpr::Exception \
    {\
    public:\
        name(const char* msg, int error,const char* file,int line) throw ()\
            : Exception(msg,error,file,line) {}\
        virtual const char* GetType() const throw()\
        {\
            return #name;\
        }\
    };

namespace kpr
{
DEFINE_EXCEPTION(SystemCallException);
DEFINE_EXCEPTION(NotImplementException);
DEFINE_EXCEPTION(InterruptedException);
DEFINE_EXCEPTION(FileUtilException);
DEFINE_EXCEPTION(RefHandleNullException);

};

#define THROW_EXCEPTION(e,msg,err) throw e(msg,err,__FILE__,__LINE__);

#endif
