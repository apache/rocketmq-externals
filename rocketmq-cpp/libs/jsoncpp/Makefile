##====================================================================
# make release=0 debug版。
# make release=1 release版。
CXXFLAGS = -g -Wall -fPIC -Wno-deprecated -fno-strict-aliasing -O3 -DNDEBUG -std=c++0x

ifeq ($(shell uname -m),x86_64)
	CXXFLAGS += -m64
else
	CXXFLAGS += -m32
endif

##====================================================================
TOPDIR := .
TARGET := $(TOPDIR)/lib/libmq_json.a

CPP_SRCDIR  := $(TOPDIR)/src
CPP_SRC := $(foreach dir,$(CPP_SRCDIR), $(wildcard $(dir)/*.cpp))
CPP_OBJS := $(patsubst %.cpp, %.o, $(CPP_SRC))
VPATH := $(CPP_SRCDIR)

LDFLAGS :=
LIBPATH :=
LDLIBS :=
CPPFLAGS := -I$(TOPDIR)/include $(addprefix -I,$(CPP_SRCDIR))

CXX := g++
AR := ar
ARFLAGS := rcs
##====================================================================
all: build

build:$(TARGET)


$(TARGET):$(CPP_OBJS)
	$(AR) $(ARFLAGS) $@ $^ $(LIBPATH) $(LDLIBS)

%.o: %.cpp
	$(CXX) -c $(CXXFLAGS) $(CPPFLAGS) -o $@ $<


rebuild:clean build

test:
	@echo $(CPP_SRC)
	@echo $(CPP_OBJS)

clean:
	$(RM) -rf $(CPP_OBJS)
	$(RM) -rf $(TARGET)
	$(RM) -rf tmp



