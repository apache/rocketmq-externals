ROCKETMQ_PATH := /data/libs/rocketmq

INCLUDE += -I$(ROCKETMQ_PATH)/include 
INCLUDE_32 += -I$(ROCKETMQ_PATH)/include -march=i686
LIB_32 += -L$(ROCKETMQ_PATH)/lib32 -lrocketmq -lz -lrt -lpthread
LIB_64 += -L$(ROCKETMQ_PATH)/lib64 -lrocketmq -lz -lrt -lpthread
