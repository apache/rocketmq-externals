##====================================================================
# make release=0 debug版。
# make release=1 release版。
CXXFLAGS = -g -fPIC -Wno-deprecated -O3

ifeq ($(shell uname -m),x86_64)
	CXXFLAGS += -m64
	BITS:= 64
else
	CXXFLAGS += -m32
	BITS := 32
endif

ifeq ($(release),0)
	CXXFLAGS += -DDEBUG
	OUTCONFIG := debug
else
	CXXFLAGS += -DNDEBUG
	OUTCONFIG := release
endif

##====================================================================
TOPDIR := .
TARGET := $(TOPDIR)/lib/libSignature.a

CPP_SRCDIR  := $(TOPDIR)/src
CPP_SRC := $(foreach dir,$(CPP_SRCDIR), $(wildcard $(dir)/*.c))
CPP_OBJS := $(patsubst %.c, %.o, $(CPP_SRC))
VPATH := $(CPP_SRCDIR)

LDLIBS :=
LIBPATH :=
CPPFLAGS := -I$(TOPDIR)/include $(addprefix -I,$(CPP_SRCDIR))

CXX := g++
AR := ar
ARFLAGS := rcs
##====================================================================
all: build

build:$(TARGET)


$(TARGET):$(CPP_OBJS)
	$(AR) $(ARFLAGS) $@ $^ $(LIBPATH) $(LDLIBS)

%.o: %.c
	$(CXX) -c $(CXXFLAGS) $(CPPFLAGS) -o $@ $<


rebuild:clean build

test:
	@echo $(CPP_SRC)
	@echo $(CPP_OBJS)

clean:
	$(RM) -rf $(CPP_OBJS)
	$(RM) -rf $(TARGET)
	$(RM) -rf tmp



