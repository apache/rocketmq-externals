#-----------------------------------------------------------------------
TARGET          := librocketmq.a
CONFIG          :=
STRIP_FLAG      := N
MFLAGS      	:= ${BIT}
CFLAGS          += -g -fPIC -Wno-deprecated -fno-strict-aliasing -fno-omit-frame-pointer
CFLAGS_32       += -march=i686
INCLUDE         += -I../include -I./common -I./kpr -I./protocol -I./message -I./transport -I./producer -I./consumer \
				   -I./jsoncpp 
LIB             += -lz -lrt -lpthread
LIB32           += 
LIB64           += 
#-----------------------------------------------------------------------

LOCAL_SRC += $(wildcard jsoncpp/*.cpp)
LOCAL_SRC += $(wildcard kpr/*.cpp)
LOCAL_SRC += $(wildcard common/*.cpp)
LOCAL_SRC += $(wildcard protocol/*.cpp)
LOCAL_SRC += $(wildcard message/*.cpp)
LOCAL_SRC += $(wildcard transport/*.cpp)
LOCAL_SRC += $(wildcard producer/*.cpp)
LOCAL_SRC += $(wildcard consumer/*.cpp)

include ./Makefile.std
#-----------------------------------------------------------------------

