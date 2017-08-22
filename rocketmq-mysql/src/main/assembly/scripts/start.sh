#!/usr/bin/env bash

binPath=$(cd "$(dirname "$0")"; pwd);
cd $binPath
cd ..
parentPath=`pwd`
libPath=$parentPath/lib/


function exportClassPath(){
    jarFileList=`find "$libPath" -name *.jar |awk -F'/' '{print $(NF)}' 2>>/dev/null`
    CLASSPATH=".:$binPath";
    for jarItem in $jarFileList
      do
        CLASSPATH="$CLASSPATH:$libPath$jarItem"
    done
    CLASSPATH=$CLASSPATH:./conf
    export CLASSPATH
}
ulimit -n 65535
exportClassPath

java -server -Xms512m -Xmx512m -Xss2m -XX:NewRatio=2  -XX:+UseGCOverheadLimit -XX:-UseParallelGC -XX:ParallelGCThreads=24 org.apache.rocketmq.mysql.Replicator
