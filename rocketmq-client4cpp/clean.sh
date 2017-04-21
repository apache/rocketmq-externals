#!/bin/sh

BUILD_PATH=`pwd`
BIT=32 make -C ./src/ cleanall
BIT=64 make -C ./src/ cleanall
rm -rf release

