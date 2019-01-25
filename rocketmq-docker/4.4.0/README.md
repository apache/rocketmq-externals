# Run namesrv and broker
```
docker run -d -p 9876:9876 -v `pwd`/data/namesrv/logs:/root/logs -v `pwd`/data/namesrv/store:/root/store --name rmqnamesrv  rocketmqinc/rocketmq:4.4.0 sh mqnamesrv
docker run -d -p 10911:10911 -p 10909:10909 -v `pwd`/data/broker/logs:/root/logs -v `pwd`/data/broker/store:/root/store --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" rocketmqinc/rocketmq:4.4.0 sh mqbroker
```

# To use specified heap size for JVM

1. Use the environment variable MAX_POSSIBLE_HEAP to specify the max heap size JVM will use. And at the sametime, when the image is run as a RocketMQ broker, the max direct memory size for storage is also set as the same size of MAX_POSSIBLE_HEAP.

2. To verify the usage:

Run:

```

docker run -d -p 9876:9876 -v `pwd`/data/namesrv/logs:/root/logs -v `pwd`/data/namesrv/store:/root/store --name rmqnamesrv -e "MAX_POSSIBLE_HEAP=100000000" rocketmqinc/rocketmq:4.4.0 sh mqnamesrv

docker run -d -p 10911:10911 -p 10909:10909 -v `pwd`/data/broker/logs:/root/logs -v `pwd`/data/broker/store:/root/store --name rmqbroker --link rmqnamesrv:namesrv -e "NAMESRV_ADDR=namesrv:9876" -e "MAX_POSSIBLE_HEAP=200000000" rocketmqinc/rocketmq:4.4.0 sh mqbroker

```

# Test to produce/consume messages
```
docker exec -ti rmqbroker sh ./tools.sh org.apache.rocketmq.example.quickstart.Producer

docker exec -ti rmqbroker sh ./tools.sh org.apache.rocketmq.example.quickstart.Consumer
```
