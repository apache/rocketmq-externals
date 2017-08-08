#!/bin/bash


TMPFILE=".clang_format_file.tmp"
FORMAT="{BasedOnStyle: Google,IndentWidth: 2,ColumnLimit: 80}"

function Usage
{
    echo "Usage: $0 want-format-file|want-format-dir ..."
    #echo "Currently only format a file or dir at a time"
}

#Setp1 check clang-format support
if ! which clang-format &>/dev/null; then
    echo -e "\033[32m !!!!!!please install clang-format  \033[0m"
    exit 1
fi


#Setp2 check weather incoming format file
if [ ! $# -ge 1 ];then
    Usage
    exit 1
fi

for dest in "$@"
do
  if [ ! -e $dest ]; then
    echo -e "\033[32m $dest not exists,please check this file weather exists \033[0m"
  fi
done


#Setp3 get filelist
for dest in $*
do
  if [ -f $dest ];then
      files="$files $dest"
  elif [ -d $dest ];then
      files="$files `ls $dest/*.cpp $dest/*.h $dest/*.cc 2>/dev/null`"
  else
      echo -e "\033[32m $dest sorry current $0 only support regular file or dir \033[0m"
  fi
done

#Setp4 use clang-format format dest file
for file in $files
do
    echo $file
    clang-format -style="$FORMAT" $file > $TMPFILE

    if [ -e $TMPFILE ];then
        filesize=`wc -c $TMPFILE |cut -d " " -f1`
        if [ $filesize -eq 0 ];then
            echo -e "\033[32m formt file error,May be because of the size of the source file is 0, or format program error \033[0m"
            exit 1
        fi
    fi

    #Setp4 replace source file
    mv -f $TMPFILE $file
done
