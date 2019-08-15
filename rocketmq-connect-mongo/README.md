# RocketMQ-connect-mongo

this is source connector moudle for mongo,you can run this by running rocketmq connecotr api,

some junit rely on mongo database you can start with a docker container

`docker run -p27027:27017 --name mongo-test -d  mongo:4.0.10 --replSet "repl1"`

and then init a mongo replicaSet

`docker exec -it mongo-test mongo ` and `rs.initiate()` and then you can run all junit test

