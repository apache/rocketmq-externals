CHECKFILE  =  .dep_create/bin
all:
	@exist=`rpm -qa dep_create`; if [ $$exist"x" == "x" ]; then sudo yum install -b current dep_create -y; fi;
	@if [ ! -e $(CHECKFILE) ]; then source /etc/profile;dep_create t-rocketmq-client4cpp; fi; 
clean:
