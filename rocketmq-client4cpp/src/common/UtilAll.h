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
#ifndef __UTILALL_H__
#define __UTILALL_H__

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <time.h>

#include <string>
#include <sstream>
#include <vector>
#include <list>
#include <set>
#include <map>

#include "RocketMQClient.h"
#include "zlib.h"
#include "json/value.h"
#include "json/writer.h"

namespace rmq
{
    const std::string WHITESPACE = " \t\r\n";
    const int CHUNK = 8192;
	const std::string yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    const std::string yyyy_MM_dd_HH_mm_ss_SSS = "yyyy-MM-dd#HH:mm:ss:SSS";
    const std::string yyyyMMddHHmmss = "yyyyMMddHHmmss";

    class UtilAll
    {
    public:
        static pid_t getPid()
        {
            static __thread pid_t pid = 0;
            if (!pid || pid != getpid())
            {
                pid = getpid();
            }
            return pid;
        }

        static pid_t getTid()
        {
            static __thread pid_t pid = 0;
            static __thread pid_t tid = 0;
            if (!pid || !tid || pid != getpid())
            {
                pid = getpid();
                tid = syscall(__NR_gettid);
            }
            return tid;
        }

        static int Split(std::vector<std::string>& out, const std::string& in, const std::string& delimiter)
        {
            std::string::size_type left = 0;
            for (size_t i = 1; i < in.size(); i++)
            {
                std::string::size_type right = in.find(delimiter, left);

                if (right == std::string::npos)
                {
                    break;
                }

                out.push_back(in.substr(left, right - left));

                left = right + delimiter.length();
            }

            out.push_back(in.substr(left));

            return out.size();
        }

        static int Split(std::vector<std::string>& out, const std::string& in, const char delimiter)
        {
            std::string::size_type left = 0;
            for (size_t i = 1; i < in.size(); i++)
            {
                std::string::size_type right = in.find(delimiter, left);

                if (right == std::string::npos)
                {
                    break;
                }

                out.push_back(in.substr(left, right - left));

                left = right + 1;
            }

            out.push_back(in.substr(left));

            return out.size();
        }

        static std::string Trim(const std::string& str)
        {
            if (str.empty())
            {
                return str;
            }

            std::string::size_type left = str.find_first_not_of(WHITESPACE);

            if (left == std::string::npos)
            {
                return "";
            }

            std::string::size_type right = str.find_last_not_of(WHITESPACE);

            if (right == std::string::npos)
            {
                return str.substr(left);
            }

            return str.substr(left, right + 1 - left);
        }

        static bool isBlank(const std::string& str)
        {
            if (str.empty())
            {
                return true;
            }

            std::string::size_type left = str.find_first_not_of(WHITESPACE);

            if (left == std::string::npos)
            {
                return true;
            }

            return false;
        }

        static int availableProcessors()
        {
            return 4;
        }


        static int hashCode(const char* pData, int len)
        {
            int h = 0;
            if (pData != NULL && len > 0)
            {
                unsigned char c;
                for (int i = 0; i < len; i++)
                {
                    c = (unsigned char)pData[i];
                    h = 31 * h + c;
                }
            }

            return h;
        }

        static int hashCode(const std::string& s)
        {
            return hashCode(s.c_str(), s.length());
        }

        static int hashCode(const char* pData)
        {
            return hashCode(std::string(pData));
        }

        static int hashCode(char x)
        {
            return x;
        }

        static int hashCode(unsigned char x)
        {
            return x;
        }

        static int hashCode(short x)
        {
            return x;
        }

        static int hashCode(unsigned short x)
        {
            return x;
        }

        static int hashCode(int x)
        {
            return x;
        }

        static int hashCode(unsigned int x)
        {
            return x;
        }

        static int hashCode(long x)
        {
            return x;
        }

        static int hashCode(unsigned long x)
        {
            return x;
        }

        template <typename T>
        static int hashCode(const std::vector<T>& v)
        {
            int h = 0;
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                h += hashCode(*it);
                ++it;
            }
            return h;
        }

        template <typename T>
        static int hashCode(const std::set<T>& v)
        {
            int h = 0;
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                h += hashCode(*it);
                ++it;
            }
            return h;
        }

		static std::string toString(Json::Value& json)
		{
			Json::FastWriter fastWriter;
			return fastWriter.write(json);
		}

        template<typename T>
        static std::string toString(const T& v)
        {
            std::ostringstream ss;
            ss << v;
            return ss.str();
        }

        template<typename T>
        static std::string toString(const std::vector<T>& v)
        {
            std::string s;
            s.append("[");
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                s.append(toString(*it));
                s.append(",");
                ++it;
            }
			if (s.size() > 1)
			{
				s.erase(s.size() - 1, 1);
			}
            s.append("]");
            return s;
        }


        template <typename T>
        static std::string toString(const std::list<T>& v)
        {
            std::string s;
            s.append("[");
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                s.append(toString(*it));
                s.append(",");
                ++it;
            }
			if (s.size() > 1)
			{
				s.erase(s.size() - 1, 1);
			}
            s.append("]");

            return s;
        }

        template <typename T>
        static std::string toString(const std::set<T>& v)
        {
            std::string s;
            s.append("[");
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                s.append(toString(*it));
                s.append(",");
                ++it;
            }
			if (s.size() > 1)
			{
				s.erase(s.size() - 1, 1);
			}
            s.append("]");
            return s;
        }

        template<typename K, typename V, typename D, typename A>
        static std::string toString(const std::map<K, V, D, A>& v)
        {
            std::string s;
            s.append("{");
            typeof(v.begin()) it = v.begin();
            while (it != v.end())
            {
                s.append(toString(it->first));
                s.append("=");
                s.append(toString(it->second));
                s.append(",");
                ++it;
            }
			if (s.size() > 1)
			{
				s.erase(s.size() - 1, 1);
			}
            s.append("}");
            return s;
        }

        template<typename out_type, typename in_type>
        static out_type convert(const in_type& t)
        {
            out_type result;
            std::stringstream stream;
            stream << t;
            stream >> result;
            return result;
        }

        static bool compress(const char* pIn, int inLen, unsigned char** pOut, int* pOutLen, int level)
        {
            int ret, flush;
            int have;
            z_stream strm;
            unsigned char out[CHUNK];

            /* allocate deflate state */
            strm.zalloc = Z_NULL;
            strm.zfree = Z_NULL;
            strm.opaque = Z_NULL;
            ret = deflateInit(&strm, level);
            if (ret != Z_OK)
            {
                return false;
            }

            int outBufferLen = inLen;
            unsigned char* outData = (unsigned char*)malloc(outBufferLen);
            int left = inLen;
            int used = 0;
            int outDataLen = 0;

            /* compress until end of buffer */
            do
            {
                strm.avail_in = left > CHUNK ? CHUNK : left;
                flush = left <= CHUNK ? Z_FINISH : Z_NO_FLUSH;
                strm.next_in = (unsigned char*)pIn + used;
                used += strm.avail_in;
                left -= strm.avail_in;

                /* run deflate() on input until output buffer not full, finish
                compression if all of source has been read in */
                do
                {
                    strm.avail_out = CHUNK;
                    strm.next_out = out;
                    ret = deflate(&strm, flush);    /* no bad return value */
                    assert(ret != Z_STREAM_ERROR);  /* state not clobbered */
                    have = CHUNK - strm.avail_out;

                    if (outDataLen + have > outBufferLen)
                    {
                        outBufferLen = outDataLen + have;
                        outBufferLen <<= 1;
                        unsigned char* tmp = (unsigned char*)realloc(outData, outBufferLen);
                        if (!tmp)
                        {
                            free(outData);
                            return false;
                        }

                        outData = tmp;
                    }

                    memcpy(outData + outDataLen, out, have);
                    outDataLen += have;

                }
                while (strm.avail_out == 0);
                assert(strm.avail_in == 0);     /* all input will be used */

                /* done when last data in file processed */
            }
            while (flush != Z_FINISH);
            assert(ret == Z_STREAM_END);        /* stream will be complete */

            *pOutLen = outDataLen;
            *pOut = outData;

            /* clean up and return */
            (void)deflateEnd(&strm);
            return true;
        }

        static bool decompress(const char* pIn, int inLen, unsigned char** pOut, int* pOutLen)
        {
            int ret;
            int have;
            z_stream strm;

            unsigned char out[CHUNK];

            /* allocate inflate state */
            strm.zalloc = Z_NULL;
            strm.zfree = Z_NULL;
            strm.opaque = Z_NULL;
            strm.avail_in = 0;
            strm.next_in = Z_NULL;
            ret = inflateInit(&strm);
            if (ret != Z_OK)
            {
                return false;
            }

            int outBufferLen = inLen << 2;
            unsigned char* outData = (unsigned char*)malloc(outBufferLen);

            int left = inLen;
            int used = 0;
            int outDataLen = 0;

            /* decompress until deflate stream ends or end of buffer */
            do
            {
                strm.avail_in = left > CHUNK ? CHUNK : left;
                if (strm.avail_in <= 0)
                {
                    break;
                }

                strm.next_in = (unsigned char*)pIn + used;
                used += strm.avail_in;
                left -= strm.avail_in;

                /* run inflate() on input until output buffer not full */
                do
                {
                    strm.avail_out = CHUNK;
                    strm.next_out = out;
                    ret = inflate(&strm, Z_NO_FLUSH);
                    assert(ret != Z_STREAM_ERROR);  /* state not clobbered */
                    switch (ret)
                    {
                        case Z_NEED_DICT:
                            ret = Z_DATA_ERROR;     /* and fall through */
                        case Z_DATA_ERROR:
                        case Z_MEM_ERROR:
                            (void)inflateEnd(&strm);
                            free(outData);
                            return false;
                    }
                    have = CHUNK - strm.avail_out;

                    if (outDataLen + have > outBufferLen)
                    {
                        outBufferLen = outDataLen + have;
                        outBufferLen <<= 1;
                        unsigned char* tmp = (unsigned char*)realloc(outData, outBufferLen);
                        if (!tmp)
                        {
                            free(outData);
                            return false;
                        }

                        outData = tmp;
                    }

                    memcpy(outData + outDataLen, out, have);
                    outDataLen += have;

                }
                while (strm.avail_out == 0);

                /* done when inflate() says it's done */
            }
            while (ret != Z_STREAM_END);

            /* clean up and return */
            (void)inflateEnd(&strm);

            if (ret == Z_STREAM_END)
            {
                *pOutLen = outDataLen;
                *pOut = outData;

                return true;
            }
            else
            {
                free(outData);

                return false;
            }
        }

        static unsigned long long hexstr2ull(const char* str)
        {
            char* end;
            return strtoull(str, &end, 16);
        }

		static long long str2ll(const char *str)
		{
			return atoll(str);
		}


		static std::string tm2str(const time_t& t, const std::string& sFormat)
		{
			struct tm stTm;
			localtime_r(&t, &stTm);

			char sTimeString[255] = "\0";
			strftime(sTimeString, sizeof(sTimeString), sFormat.c_str(), &stTm);

			return std::string(sTimeString);
		}

		static std::string now2str(const std::string& sFormat)
		{
			time_t t = time(NULL);
			return tm2str(t, sFormat.c_str());
		}

		static std::string now2str()
		{
			return now2str("%Y-%m-%d %H:%M:%S");
		}

		static int64_t now2ms()
		{
			struct timeval tv;
			gettimeofday(&tv, NULL);
			return tv.tv_sec * (int64_t)1000 + tv.tv_usec / 1000;
		}

		static int64_t now2us()
		{
			struct timeval tv;
			gettimeofday(&tv, NULL);
			return tv.tv_sec * (int64_t)1000*1000 + tv.tv_usec;
		}

		static int str2tm(const std::string &sString, const std::string &sFormat, struct tm &stTm)
		{
			char *p = strptime(sString.c_str(), sFormat.c_str(), &stTm);
    		return (p != NULL) ? 0 : -1;
		}

		static time_t str2tm(const std::string &sString, const std::string &sFormat)
		{
			struct tm stTm;
			if (str2tm(sString, sFormat, stTm) == 0)
			{
				time_t t = mktime(&stTm);
				return t;
			}
			else
			{
				return -1;
			}
		}
    };
}

#endif
