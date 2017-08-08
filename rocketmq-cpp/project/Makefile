##====================================================================
# make release=0 debug版。
# make release=1 release版。
CXXFLAGS = -g2 -Wall -fPIC -Wno-deprecated -fno-strict-aliasing -O3 -DNDEBUG -std=c++0x

ifeq ($(shell uname -m),x86_64)
	CXXFLAGS += -m64
else
	CXXFLAGS += -m32
endif

##====================================================================
PREFIX:=/usr/local
TOPDIR := ..
STATIC_TARGET := $(TOPDIR)/bin/librocketmq.a
SHARED_TARGET := $(TOPDIR)/bin/librocketmq.so

SRCDIR:=$(TOPDIR)/src
INCFILES:=$(wildcard $(TOPDIR)/include/*.h)
CPP_SRCDIR := $(SRCDIR) \
			  $(SRCDIR)/common \
        	  $(SRCDIR)/thread \
			  $(SRCDIR)/log \
			  $(SRCDIR)/consumer \
			  $(SRCDIR)/message \
        	  $(SRCDIR)/producer \
        	  $(SRCDIR)/protocol \
        	  $(SRCDIR)/transport \
              $(SRCDIR)/statistics

CPP_SRC := $(foreach dir,$(CPP_SRCDIR), $(wildcard $(dir)/*.cpp))
STATIC_OBJ := $(filter-out %/dllmain.o,$(patsubst %.cpp, %.o, $(CPP_SRC)))
SHARED_OBJ := $(filter-out %/dllmain.lo,$(patsubst %.cpp, %.lo, $(CPP_SRC)))
VPATH:=$(CPP_SRCDIR)

JSONCPP_INCLUDE := /pathOfJsonCPP/include
BOOST_INCLUDE := /pathOfBoost/include
LIBEVENT_INCLUDE := /pathOfLibEvent/include

CPPFLAGS := -I$(TOPDIR)/include \
                        $(addprefix -I,$(JSONCPP_INCLUDE)) \
			$(addprefix -I,$(BOOST_INCLUDE)) \
			$(addprefix -I,$(LIBEVENT_INCLUDE)) \	
			$(addprefix -I,$(CPP_SRCDIR)) \
			$(addprefix -I,$(foreach dir,$(TOPDIR)/libs, $(wildcard $(dir)/*/include)))

LDFLAGS := --shared -Wl,-soname=librocketmq.so -fPIC
LIBPATH := $(addprefix -L,$(foreach dir,$(TOPDIR)/libs, $(wildcard $(dir)/*/lib)))
LDLIBS := $(addprefix -l, alog Signature pthread dl rt)

CXX := g++
AR := ar
ARFLAGS := rcs
LIBS:= $(foreach dir,$(TOPDIR)/libs, $(wildcard $(dir)/*/lib/*.a))
##====================================================================
include     tool.mak
##====================================================================
all:build-static  build-shared

build-static:$(STATIC_TARGET)

build-shared:$(SHARED_TARGET)

$(STATIC_TARGET):$(STATIC_OBJ) $(LIBS)
	$(BUILD_LIBRARY)

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
	mkdir -p $(PREFIX)/include/metaq
	rm -rf $(PREFIX)/include/metaq/*
	cp -r $(INCFILES) --target-directory $(PREFIX)/include/metaq/
	mkdir -p $(PREFIX)/lib
	rm -rf $(PREFIX)/lib/librocketmq.a
	rm -rf $(PREFIX)/lib/librocketmq.so
	cp -rf $(STATIC_TARGET) $(PREFIX)/lib/
	cp -rf $(SHARED_TARGET) $(PREFIX)/lib/
	@echo
	@echo 'Install succeed, target directory is "'$(PREFIX)'".'
