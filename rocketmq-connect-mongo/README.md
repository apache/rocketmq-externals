# RocketMQ-connect-mongo

this is source connector moudle for mongo,you can run this by running rocketmq connecotr api,

some junit rely on mongo database you can flow step run a mongo container

- `docker run -p27027:27017 --name mongo-test -d  mongo:4.0.10 --replSet "repl1"`
- `docker exec -it mongo-test mongo `
- `rs.initiate()` 

init a mongo replicaSet run all junit test


## a special junit
method `MongoFactoryTest#testSSLTrustStore` is for mongo ssl or tsl test,need mongod config ssl mode， if you want use ssl or tsl you need 
modify junit , appoint ssl or tsl pem path and password。


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


## use case

`http://127.0.0.1:8081/connectors/testMongoReplicaSet?config={"connector-class":"org.apache.connect.mongo.connector.MongoSourceConnector","oms-driver-url":"oms:rocketmq://localhost:9876/default:default","mongoAddr":"rep1/127.0.0.1:27077,127.0.0.1:27078,127.0.0.1:27080","source-record-converter":"org.apache.rocketmq.connect.runtime.converter.JsonConverter"}`
