#/***************************************************************************
#created:  2014-03-20
#filename: Makefile
#author:   自修 yjlfuture@163.com(jianlin.yjl@alibaba-inc.com)
#breaf:
#***************************************************************************/
CXX := /usr/local/gcc-4.9.2/bin/g++ 
CXXFLAGS = -g2 -Wall -Wno-deprecated -fno-strict-aliasing -O3 -DNDEBUG -std=c++11

ifeq ($(shell uname -m),x86_64)
	CXXFLAGS += -m64
else
	CXXFLAGS += -m32
endif

#<!***************************************************************************
TOPDIR := ..
SRCS := $(wildcard *.cpp)
OBJS := $(patsubst %.cpp, %.o, $(SRCS))
TARGET := $(patsubst %.cpp, $(TOPDIR)/bin/%, $(SRCS))

BOOST_INCLUDE := /pathOfBoost/include

CPPFLAGS := -I$(TOPDIR)/include \
			-I$(TOPDIR)/src/common \
			-I$(TOPDIR)/src/log \
			-I$(TOPDIR)/src/thread \
			$(addprefix -I,$(BOOST_INCLUDE))\
			$(addprefix -I,$(foreach dir,$(TOPDIR)/libs, $(wildcard $(dir)/*/include)))

JSONCPP_LIB := /pathOfJsonCPP/lib
BOOST_LIB := /pathOfBoost/lib
LIBEVENT_LIB := /pathOfLibEvent/lib
LDLIBS := -lrocketmq -L$(BOOST_LIB)  -lboost_log_setup -lboost_log -lboost_regex -lboost_filesystem -lboost_system -lboost_thread -lboost_chrono -lboost_serialization -lboost_iostreams -L$(JSONCPP_LIB) -l jsoncpp \
	-L$(LIBEVENT_LIB) -levent -levent_core -levent_pthreads -levent_openssl -levent_extra -lpthread -ldl -lrt

LDFLAGS :=
LIBPATH := -L$(TOPDIR)/bin

##====================================================================
all:$(TARGET)

$(TOPDIR)/bin/%:%.o
	$(CXX) $(LDFLAGS) -o $@ $<  $(LIBPATH) $(LDLIBS)

%.o:%.cpp
	$(CXX) -c $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

test:
	@echo $(TARGET)

clean:
	$(RM) -rf $(TARGET)
	$(RM) -rf $(OBJS)
	$(RM) -rf *.log *.suo
