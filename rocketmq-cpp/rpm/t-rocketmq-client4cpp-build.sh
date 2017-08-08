#!/bin/bash
#===============================================================================
#          FILE:  t-rocketmq-client4cpp-build.sh
# 
#         USAGE:  ./t-rocketmq-client4cpp-build.sh 
# 
#   DESCRIPTION:  the shell script of building librocketmq
# 
#       OPTIONS:  ---
#  REQUIREMENTS:  ---
#          BUGS:  ---
#         NOTES:  ---
#        AUTHOR:  zixiu, jianlin.yjl@alibaba-inc.com
#       COMPANY:  alibaba-inc.com
#       VERSION:  1.0
#       CREATED:  10/14/2014
#      REVISION:  ---
#===============================================================================

# set workspace
BASE_PATH="$(cd ${BASH_SOURCE[0]%/*}; pwd)"
cd $BASE_PATH

# rpm create
/usr/local/bin/rpm_create -p /home/admin/share -v $3 -r $4 $2.spec -k
