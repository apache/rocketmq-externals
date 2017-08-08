#!/bin/sh

VERSION="rocketmq-client4cpp"
CWD_DIR=$(cd "$(dirname "$0")"; pwd)
DEPLOY_BUILD_HOME=${CWD_DIR}/${VERSION}

# ##====================================================================
make
# ##====================================================================
# # deploy
rm -rf   ${DEPLOY_BUILD_HOME}
mkdir -p ${DEPLOY_BUILD_HOME}/lib
mkdir -p ${DEPLOY_BUILD_HOME}/logs
rm -rf ${CWD_DIR}/bin/*.log
cp -rf ${CWD_DIR}/bin/*.a   ${DEPLOY_BUILD_HOME}/lib/
cp -rf ${CWD_DIR}/bin/*.so  ${DEPLOY_BUILD_HOME}/lib/
cp -rf ${CWD_DIR}/include ${DEPLOY_BUILD_HOME}/
cp -rf ${CWD_DIR}/example ${DEPLOY_BUILD_HOME}/
cp -rf ${CWD_DIR}/doc 	  ${DEPLOY_BUILD_HOME}/
cp -rf ${CWD_DIR}/readme  ${DEPLOY_BUILD_HOME}/


cd ${CWD_DIR} && tar -cvzf ./${VERSION}.tar.gz ./${VERSION}  >/dev/null 2>&1
rm -rf ${DEPLOY_BUILD_HOME}
# # ##====================================================================
make clean
