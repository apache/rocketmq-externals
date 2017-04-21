/**
* Copyright (C) 2013 suwenkuang ,hooligan_520@qq.com
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

#include <string.h>
#include <time.h>
#include <errno.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <cassert>
#include <cstdio>
#include <string>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <algorithm>
#include <map>
#include <stack>
#include <vector>

#include "FileUtil.h"
#include "KPRUtil.h"
#include "Exception.h"


namespace kpr
{

std::ifstream::pos_type FileUtil::getFileSize(const std::string& sFullFileName)
{
    std::ifstream ifs(sFullFileName.c_str());
    ifs.seekg(0, std::ios_base::end);
    return ifs.tellg();
}

bool FileUtil::isAbsolute(const std::string& sFullFileName)
{
    if (sFullFileName.empty())
    {
        return false;
    }

    unsigned i = 0;
    while (isspace(sFullFileName[i]))
    {
        ++i;
    }

    return sFullFileName[i] == '/';
}

bool FileUtil::isFileExist(const std::string& sFullFileName, mode_t iFileType)
{
    struct stat f_stat;

    if (lstat(sFullFileName.c_str(), &f_stat) == -1)
    {
        return false;
    }

    if (!(f_stat.st_mode & iFileType))
    {
        return false;
    }

    return true;
}

bool FileUtil::isFileExistEx(const std::string& sFullFileName, mode_t iFileType)
{
    struct stat f_stat;

    if (stat(sFullFileName.c_str(), &f_stat) == -1)
    {
        return false;
    }

    if (!(f_stat.st_mode & iFileType))
    {
        return false;
    }

    return true;
}

bool FileUtil::makeDir(const std::string& sDirectoryPath, mode_t iFlag)
{
    int iRetCode = mkdir(sDirectoryPath.c_str(), iFlag);
    if (iRetCode < 0 && errno == EEXIST)
    {
        return isFileExistEx(sDirectoryPath, S_IFDIR);
    }

    return iRetCode == 0;
}

bool FileUtil::makeDirRecursive(const std::string& sDirectoryPath, mode_t iFlag)
{
    std::string simple = simplifyDirectory(sDirectoryPath);

    std::string::size_type pos = 0;
    for (; pos != std::string::npos;)
    {
        pos = simple.find("/", pos + 1);
        std::string s;
        if (pos == std::string::npos)
        {
            s = simple.substr(0, simple.size());
            return makeDir(s.c_str(), iFlag);
        }
        else
        {
            s = simple.substr(0, pos);
            if (!makeDir(s.c_str(), iFlag))
            {
                return false;
            }
        }
    }
    return true;
}

int FileUtil::setExecutable(const std::string& sFullFileName, bool canExecutable)
{
    struct stat f_stat;

    if (stat(sFullFileName.c_str(), &f_stat) == -1)
    {
        return -1;
    }

    return chmod(sFullFileName.c_str(), canExecutable ? f_stat.st_mode | S_IXUSR : f_stat.st_mode & ~S_IXUSR);
}

bool FileUtil::canExecutable(const std::string& sFullFileName)
{
    struct stat f_stat;

    if (stat(sFullFileName.c_str(), &f_stat) == -1)
    {
        return false;
    }

    return f_stat.st_mode & S_IXUSR;
}

int FileUtil::removeFile(const std::string& sFullFileName, bool bRecursive)
{
    std::string path = simplifyDirectory(sFullFileName);

    if (isFileExist(path, S_IFDIR))
    {
        if (bRecursive)
        {
            std::vector<std::string> files;
            listDirectory(path, files, false);
            for (size_t i = 0; i < files.size(); i++)
            {
                removeFile(files[i], bRecursive);
            }

            if (path != "/")
            {
                if (::rmdir(path.c_str()) == -1)
                {
                    return -1;
                }
                return 0;
            }
        }
        else
        {
            if (::rmdir(path.c_str()) == -1)
            {
                return -1;
            }
        }
    }
    else
    {
        if (::remove(path.c_str()) == -1)
        {
            return -1;
        }
    }

    return 0;
}

std::string FileUtil::simplifyDirectory(const std::string& path)
{
    std::string result = path;

    std::string::size_type pos;

    pos = 0;
    while ((pos = result.find("//", pos)) != std::string::npos)
    {
        result.erase(pos, 1);
    }

    pos = 0;
    while ((pos = result.find("/./", pos)) != std::string::npos)
    {
        result.erase(pos, 2);
    }

    while (result.substr(0, 4) == "/../")
    {
        result.erase(0, 3);
    }

    if (result == "/.")
    {
        return result.substr(0, result.size() - 1);
    }

    if (result.size() >= 2 && result.substr(result.size() - 2, 2) == "/.")
    {
        result.erase(result.size() - 2, 2);
    }

    if (result == "/")
    {
        return result;
    }

    if (result.size() >= 1 && result[result.size() - 1] == '/')
    {
        result.erase(result.size() - 1);
    }

    if (result == "/..")
    {
        result = "/";
    }

    return result;
}

std::string FileUtil::load2str(const std::string& sFullFileName)
{
    std::ifstream ifs(sFullFileName.c_str());
    if (!ifs)
    {
        return "";
    }
    return std::string(std::istreambuf_iterator<char>(ifs), std::istreambuf_iterator<char>());
}

void FileUtil::save2file(const std::string& sFullFileName, const std::string& sFileData)
{
    std::ofstream ofs((sFullFileName).c_str());
    ofs << sFileData;
    ofs.close();
}

int FileUtil::save2file(const std::string& sFullFileName, const char* sFileData, size_t length)
{
    FILE* fp = fopen(sFullFileName.c_str(), "wb");
    if (fp == NULL)
    {
        return -1;
    }

    size_t ret = fwrite((void*)sFileData, 1, length, fp);
    fclose(fp);

    if (ret == length)
    {
        return 0;
    }
    return -1;
}

std::string FileUtil::getExePath()
{
    std::string proc = "/proc/self/exe";
    char buf[2048] = "\0";

    int bufsize = sizeof(buf) / sizeof(char);

    int count = readlink(proc.c_str(), buf, bufsize);

    if (count < 0)
    {
        THROW_EXCEPTION(FileUtilException, "could not get exe path error", errno);
    }

    count = (count >= bufsize) ? (bufsize - 1) : count;

    buf[count] = '\0';
    return buf;
}

std::string FileUtil::extractFileName(const std::string& sFullFileName)
{
    if (sFullFileName.length() <= 0)
    {
        return "";
    }

    std::string::size_type pos = sFullFileName.rfind('/');
    if (pos == std::string::npos)
    {
        return sFullFileName;
    }

    return sFullFileName.substr(pos + 1);
}

std::string FileUtil::extractFilePath(const std::string& sFullFileName)
{
    if (sFullFileName.length() <= 0)
    {
        return "./";
    }

    std::string::size_type pos = 0;

    for (pos = sFullFileName.length(); pos != 0 ; --pos)
    {
        if (sFullFileName[pos - 1] == '/')
        {
            return sFullFileName.substr(0, pos);
        }
    }

    return "./";
}

std::string FileUtil::extractFileExt(const std::string& sFullFileName)
{
    std::string::size_type pos;
    if ((pos = sFullFileName.rfind('.')) == std::string::npos)
    {
        return std::string("");
    }

    return sFullFileName.substr(pos + 1);
}

std::string FileUtil::excludeFileExt(const std::string& sFullFileName)
{
    std::string::size_type pos;
    if ((pos = sFullFileName.rfind('.')) == std::string::npos)
    {
        return sFullFileName;
    }

    return sFullFileName.substr(0, pos);
}

std::string FileUtil::replaceFileExt(const std::string& sFullFileName, const std::string& sExt)
{
    return excludeFileExt(sFullFileName) + "." + sExt;
}

std::string FileUtil::extractUrlFilePath(const std::string& sUrl)
{
    std::string sLowerUrl = KPRUtil::lower(sUrl);
    std::string::size_type pos = sLowerUrl.find("http://");

    if (pos == 0)
    {
        pos += strlen("http://");
    }
    else if (pos == std::string::npos)
    {
        pos = 0;
    }

    for (; pos < sUrl.length(); ++pos)
    {
        if (sUrl[pos] == '/')
        {
            if (pos < sUrl.length() - 1)
            {
                pos++;
                break;
            }
            else
            {
                return "";
            }
        }
    }

    if (pos == std::string::npos || pos == sUrl.length())
    {
        pos = 0;
    }

    return sUrl.substr(pos);
}

size_t FileUtil::scanDir(const std::string& sFilePath, std::vector<std::string>& vtMatchFiles, FILE_SELECT f, int iMaxSize)
{
    vtMatchFiles.clear();

    struct dirent** namelist;
    int n = scandir(sFilePath.c_str(), &namelist, f, alphasort);

    if (n < 0)
    {
        return 0;
    }
    else
    {
        while (n--)
        {
            if (iMaxSize > 0 && vtMatchFiles.size() >= (size_t)iMaxSize)
            {
                free(namelist[n]);
                break;
            }
            else
            {
                vtMatchFiles.push_back(namelist[n]->d_name);
                free(namelist[n]);
            }
        }
        free(namelist);
    }

    return vtMatchFiles.size();
}

void FileUtil::listDirectory(const std::string& path, std::vector<std::string>& files, bool bRecursive)
{
    std::vector<std::string> tf;
    scanDir(path, tf, 0, 0);

    for (size_t i = 0; i < tf.size(); i++)
    {
        if (tf[i] == "." || tf[i] == "..")
        {
            continue;
        }

        std::string s = path + "/" + tf[i];

        if (isFileExist(s, S_IFDIR))
        {
            files.push_back(simplifyDirectory(s));
            if (bRecursive)
            {
                listDirectory(s, files, bRecursive);
            }
        }
        else
        {
            files.push_back(simplifyDirectory(s));
        }
    }
}

void FileUtil::copyFile(const std::string& sExistFile, const std::string& sNewFile, bool bRemove)
{
    if (FileUtil::isFileExist(sExistFile, S_IFDIR))
    {
        FileUtil::makeDir(sNewFile);
        std::vector<std::string> tf;
        FileUtil::scanDir(sExistFile, tf, 0, 0);
        for (size_t i = 0; i < tf.size(); i++)
        {
            if (tf[i] == "." || tf[i] == "..")
            {
                continue;
            }
            std::string s = sExistFile + "/" + tf[i];
            std::string d = sNewFile + "/" + tf[i];
            copyFile(s, d, bRemove);
        }
    }
    else
    {
        if (bRemove)
        {
            std::remove(sNewFile.c_str());
        }
        std::ifstream fin(sExistFile.c_str());
        if (!fin)
        {
            THROW_EXCEPTION(FileUtilException, "[FileUtil::copyFile] infile open fail", errno);
        }
        std::ofstream fout(sNewFile.c_str());
        if (!fout)
        {
            THROW_EXCEPTION(FileUtilException, "[FileUtil::copyFile] newfile open fail", errno);
        }
        struct stat f_stat;
        if (stat(sExistFile.c_str(), &f_stat) == -1)
        {
            THROW_EXCEPTION(FileUtilException, "[FileUtil::copyFile] infile stat fail", errno);
        }
        chmod(sNewFile.c_str(), f_stat.st_mode);
        fout << fin.rdbuf();
        fin.close();
        fout.close();

    }
}

}

