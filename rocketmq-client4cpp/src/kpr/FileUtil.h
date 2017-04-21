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

#ifndef __KPR_FILEUTIL_H_
#define __KPR_FILEUTIL_H_

#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <fnmatch.h>


#include <string>
#include <vector>


namespace kpr
{

class FileUtil
{
public:

    static std::ifstream::pos_type getFileSize(const std::string& sFullFileName);

    static bool isAbsolute(const std::string& sFullFileName);

    static bool isFileExist(const std::string& sFullFileName, mode_t iFileType = S_IFREG);

    static bool isFileExistEx(const std::string& sFullFileName, mode_t iFileType = S_IFREG);

    static std::string simplifyDirectory(const std::string& path);

    static bool makeDir(const std::string& sDirectoryPath, mode_t iFlag = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH |  S_IXOTH);

    static bool makeDirRecursive(const std::string& sDirectoryPath, mode_t iFlag = S_IRUSR | S_IWUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH |  S_IXOTH);

    static int setExecutable(const std::string& sFullFileName, bool canExecutable);

    static bool canExecutable(const std::string& sFullFileName);

    static int removeFile(const std::string& sFullFileName, bool bRecursive);

    static std::string load2str(const std::string& sFullFileName);

    static void save2file(const std::string& sFullFileName, const std::string& sFileData);

    static int save2file(const std::string& sFullFileName, const char* sFileData, size_t length);

    static std::string getExePath();

    static std::string extractFileName(const std::string& sFullFileName);

    static std::string extractFilePath(const std::string& sFullFileName);

    static std::string extractFileExt(const std::string& sFullFileName);

    static std::string excludeFileExt(const std::string& sFullFileName);

    static std::string replaceFileExt(const std::string& sFullFileName, const std::string& sExt);

    static std::string extractUrlFilePath(const std::string& sUrl);

    typedef int (*FILE_SELECT)(const dirent*);

    static size_t scanDir(const std::string& sFilePath, std::vector<std::string>& vtMatchFiles, FILE_SELECT f = NULL, int iMaxSize = 0);

    static void listDirectory(const std::string& path, std::vector<std::string>& files, bool bRecursive);

    static void copyFile(const std::string& sExistFile, const std::string& sNewFile, bool bRemove = false);
};

}
#endif // __FILE_UTIL_H_
