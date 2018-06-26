!/bin/bash
sudo docker build -t apache/rocketmq-namesrv:4.2.0 ./namesrv

sudo docker build -t apache/rocketmq-broker:4.2.0 ./broker

sudo docker run -d -p 9876:9876 --name rmqnamesrv  apache/rocketmq-namesrv:4.2.0

sudo docker run -d -p 10911:10911 -p 10909:10909 --name rmqbroker --link rmqnamesrv:rmqnamesrv -e "NAMESRV_ADDR=rmqnamesrv:9876" apache/rocketmq-broker:4.2.0
