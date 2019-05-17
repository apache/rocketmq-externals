#!/bin/sh

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

function checkVersion()        
{
    echo "Stage version = $1"
	echo $1 |grep -E "^[0-9]+\.[0-9]+\.[0-9]+" > /dev/null
    if [ $? = 0 ]; then
        return 1
    fi
            
	echo "Version $1 illegal, it should be X.X.X format(e.g. 4.5.0), please check released versions in 'https://dist.apache.org/repos/dist/release/rocketmq/'"
    return 0
} 

CURRENT_DIR="$(cd "$(dirname "$0")"; pwd)"

[ ! -d "$STAGE_DIR" ] &&  STAGE_DIR=$CURRENT_DIR/stages
mkdir -p $STAGE_DIR

if [ $# -lt 1 ]; then
    echo "Usage: sh $0 version"
    exit -1
fi

version=$1
checkVersion $version
if [ $? = 0 ]; then
	exit -1
fi

echo "mkdir $STAGE_DIR/$version"
mkdir -p "$STAGE_DIR/$version"

cp -rf "$CURRENT_DIR/templates/" "$STAGE_DIR/$version"

echo "staged templates into folder $STAGE_DIR/$version"

# Stage the real version
# todo fix on linux (sed)
#find "$STAGE_DIR/$version" -type f -exec sed -i "" "s/ROCKETMQ_VERSION/${version}/g" {} \;
find "$STAGE_DIR/$version" -type f | xargs perl -pi -e "s/ROCKETMQ_VERSION/${version}/g"

cd $STAGE_DIR
