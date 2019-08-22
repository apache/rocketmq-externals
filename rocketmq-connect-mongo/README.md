# RocketMQ-connect-mongo

this is source connector moudle for mongo,you can run this by running rocketmq connecotr api,

some junit rely on mongo database you can start with a docker container

`docker run -p27027:27017 --name mongo-test -d  mongo:4.0.10 --replSet "repl1"`

and then init a mongo replicaSet

`docker exec -it mongo-test mongo ` and `rs.initiate()` and then you can run all junit test



## task config params

| param | Description | type |
| --- | --- | --- |
| mongoAddr | shardName=replicaSetName/127.0.0.1:2781,127.0.0.1:2782,127.0.0.1:2783; | string, split by ; |
| mongoUserName | mongo root username| string |
| mongoPassWord | mongo root password| string |
| interestDbAndCollection | {"dbName":["collection1","collection2"]}, collectionName can be "*" means all collection | json |
| positionTimeStamp | mongo oplog `bsontimestamp.value`, runtime store position is highest level | int |
| positionInc | mongo oplog `bsontimestamp.inc`, runtime store position is highest level | int |
| dataSync | sync all interestDbAndCollection data, runtime store position is highest level | json, Map<String(dbName), List<String(collectionName)>> |
| serverSelectionTimeoutMS | mongo driver select replicaServer timeout | long | 
| connectTimeoutMS | mongo driver connect socket timeout | long |
| socketTimeoutMS | mongo driver read or write timeout | long |
| ssl or tsl | mongo driver use ssl or tsl | boolean |
| tlsInsecure or sslInvalidHostNameAllowed | mongo driver when use ssl or tsl allow invalid hostname | boolean|
| compressors | compressors way | string (zlib or snappy)
| zlibCompressionLevel | zlib compressors level| int (1-7)|
| trustStore | ssl pem| path|
| trustStorePassword | ssl pem decrypt password | string|
