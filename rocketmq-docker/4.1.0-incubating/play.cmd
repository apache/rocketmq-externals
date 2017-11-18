docker build -t apache/incubator-rocketmq-namesrv:4.1.0-incubating ./namesrv

docker build -t apache/incubator-rocketmq-broker:4.1.0-incubating ./broker

docker run -d -p 9876:9876 --name rmqnamesrv  apache/incubator-rocketmq-namesrv:4.1.0-incubating

docker run -d -p 10911:10911 -p 10909:10909 --name rmqbroker --link rmqnamesrv:rmqnamesrv -e "NAMESRV_ADDR=rmqnamesrv:9876" apache/incubator-rocketmq-broker:4.1.0-incubating
