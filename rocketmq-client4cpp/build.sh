#!/bin/sh
VERSION=1.0.3
BUILD_PATH=`pwd`
INSTALL_PATH=$BUILD_PATH/release
RELEASE_PATH=/data/libs/rocketmq

echo -e  "\e[33;1m# copy include files...\e[0m"
mkdir -p $INSTALL_PATH
rm -rf $INSTALL_PATH/*
cp -rf $BUILD_PATH/rocketmq.mk $INSTALL_PATH/
cp -rf $BUILD_PATH/include $INSTALL_PATH/
cp -rf $BUILD_PATH/example $INSTALL_PATH/

echo -e  "\e[33;1m# build target with BIT=32...\e[0m"
cd $BUILD_PATH/
BIT=32 make clean >/dev/null
BIT=32 make all >/dev/null
mkdir -p $INSTALL_PATH/lib32
cp -rf $BUILD_PATH/src/librocketmq.a $INSTALL_PATH/lib32/librocketmq.a

echo -e  "\e[33;1m# build target with BIT=64...\e[0m"
cd $BUILD_PATH/
BIT=64 make clean >/dev/null
BIT=64 make all  >/dev/null
mkdir -p $INSTALL_PATH/lib64
cp -rf $BUILD_PATH/src/librocketmq.a $INSTALL_PATH/lib64/librocketmq.a

echo -e  "\e[33;1m# release libs...\e[0m"
cd $BUILD_PATH/
tar czf rocketmq-client4cpp-${VERSION}.tgz release/

#rm -rf $RELEASE_PATH
#cp -rf $INSTALL_PATH $RELEASE_PATH

echo -e "\e[33;1m# build example...\e[0m"
cd $INSTALL_PATH/example
make all >/dev/null

