#/*
#* Licensed to the Apache Software Foundation (ASF) under one or more
#* contributor license agreements.  See the NOTICE file distributed with
#* this work for additional information regarding copyright ownership.
#* The ASF licenses this file to You under the Apache License, Version 2.0
#* (the "License"); you may not use this file except in compliance with
#* the License.  You may obtain a copy of the License at
#*
#*     http://www.apache.org/licenses/LICENSE-2.0
#*
#* Unless required by applicable law or agreed to in writing, software
#* distributed under the License is distributed on an "AS IS" BASIS,
#* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#* See the License for the specific language governing permissions and
#* limitations under the License.
#*/

##====================================================================
# make release=0 debug版。
# make release=1 release版。
CXXFLAGS = -g -pthread -Wall -fPIC -Wno-deprecated -fno-strict-aliasing -fno-omit-frame-pointer -O0 -DNDEBUG

ifeq ($(shell uname -m),x86_64)
	CXXFLAGS += -m64
else
	CXXFLAGS += -m32
endif

##====================================================================
PREFIX:=/usr/local
TOPDIR := ..
STATIC_TARGET := $(TOPDIR)/bin/librocketmqclientpython.a
SHARED_TARGET := $(TOPDIR)/bin/librocketmqclientpython.so
SRCDIR:=$(TOPDIR)/src
INCFILES:=$(wildcard $(TOPDIR)/include/*.h)
CPP_SRCDIR := $(SRCDIR)

CPP_SRC := $(foreach dir,$(CPP_SRCDIR), $(wildcard $(dir)/*.cpp))
STATIC_OBJ := $(filter-out %/dllmain.o,$(patsubst %.cpp, %.o, $(CPP_SRC)))
SHARED_OBJ := $(filter-out %/dllmain.lo,$(patsubst %.cpp, %.lo, $(CPP_SRC)))
VPATH:=$(CPP_SRCDIR)

PYTHON_INCLUDE:=/usr/include/python2.7
ROCKETMQ_INCLUDE:=/usr/local/include/rocketmq
PYTHON_LIB:=/usr/python/bin
PYTHON_LIBD:=/usr/python/lib
ROCKETMQ_LIBD:=/usr/local/lib
CPPFLAGS := -I$(TOPDIR)/include \
		    $(addprefix -I,$(CPP_SRCDIR)) \
		    $(addprefix -I,$(ROCKETMQ_INCLUDE)) \
			$(addprefix -I,$(PYTHON_INCLUDE))

LDFLAGS := -shared -Wl,-soname=librocketmqclientpython.so -pthread -fPIC
LIBPATH := $(addprefix -L, $(ROCKETMQ_LIBD) $(PYTHON_LIBD))
LDLIBS := $(addprefix -l,rocketmq boost_python python2.7)

CXX := g++
AR := ar
ARFLAGS := rcs
LIBS:= $(foreach dir,$(ROCKETMQ_LIBD),$(wildcard $(dir)/*/lib/*.a))
##====================================================================
#include     tool.mak
##====================================================================
all:build-static  build-shared

build-static:$(STATIC_TARGET)

build-shared:$(SHARED_TARGET)

$(STATIC_TARGET):$(STATIC_OBJ) $(LIBS)

$(SHARED_TARGET):$(SHARED_OBJ)
	$(CXX) $(LDFLAGS) -o $@ $^ $(LIBPATH) $(LDLIBS)

%.o:%.cpp
	$(CXX) -c $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

%.lo:%.cpp
	$(CXX) -c $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

rebuild:clean build

test:
	@echo $(LIBS)

clean:
	$(RM) -rf $(STATIC_OBJ)
	$(RM) -rf $(SHARED_OBJ)
	$(RM) -rf  ipch  *.sdf *.opensdf *.user *.suo

install: $(STATIC_TARGET) $(SHARED_TARGET)
	mkdir -p $(PREFIX)/lib
	rm -rf $(PREFIX)/lib/librocketmqclientpython.a
	rm -rf $(PREFIX)/lib/librocketmqclientpython.so
	#cp -rf $(STATIC_TARGET) $(PREFIX)/lib/
	cp -rf $(SHARED_TARGET) $(PREFIX)/lib/
	@echo
	@echo 'Install succeed, target directory is "'$(PREFIX)'".'
