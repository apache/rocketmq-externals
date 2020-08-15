### Tunnel service

Table Store tunnel service golang sdk.

### Install

* download tunnel client source code

```bash
go get github.com/aliyun/aliyun-tablestore-go-sdk/tunnel
```


* use dep to install dependencies under tunnel directory
  * install [dep](https://github.com/golang/dep#installation)
  * dep ensure -v
* or use `go get` to install dependencies

```bash
go get -u go.uber.org/zap
go get github.com/cenkalti/backoff
go get github.com/golang/protobuf/proto
go get github.com/satori/go.uuid
go get github.com/stretchr/testify/assert
go get github.com/smartystreets/goconvey/convey
go get github.com/golang/mock/gomock
go get gopkg.in/natefinch/lumberjack.v2
```


### Document

* [中文文档](https://help.aliyun.com/document_detail/102520.html?spm=a2c4g.11174283.6.766.379b15c3R8lQsh)

### Quick Start

* tunnel type

  * TunnelTypeStream:stream data(增量数据流)
  * TunnelTypeBaseData: full data(全量数据流)
  * TunnelTypeBaseStream: full and stream data(先全量后增量数据流)

* init tunnel client

```go
    tunnelClient := tunnel.NewTunnelClient(endpoint, instance,
       accessKeyId, accessKeySecret)
```

* create new tunnel

```go
    req := &tunnel.CreateTunnelRequest{
       TableName:  "testTable",
       TunnelName: "testTunnel",
       Type:       tunnel.TunnelTypeBaseStream, //base and stream data tunnel
    }
    resp, err := tunnelClient.CreateTunnel(req)
    if err != nil {
       log.Fatal("create test tunnel failed", err)
    }
    log.Println("tunnel id is", resp.TunnelId)
```

* get existing tunnel detail information

```go
    req := &tunnel.DescribeTunnelRequest{
       TableName:  "testTable",
       TunnelName: "testTunnel",
    }
    resp, err := tunnelClient.DescribeTunnel(req)
    if err != nil {
       log.Fatal("create test tunnel failed", err)
    }
    log.Println("tunnel id is", resp.Tunnel.TunnelId)
```

* consume tunnel data with callback function

```go
//user-defined callback function
func exampleConsumeFunction(ctx *tunnel.ChannelContext, records []*tunnel.Record) error {
	fmt.Println("user-defined information", ctx.CustomValue)
	for _, rec := range records {
		fmt.Println("tunnel record detail:", rec.String())
	}
	fmt.Println("a round of records consumption finished")
	return nil
}

//set callback to SimpleProcessFactory
workConfig := &tunnel.TunnelWorkerConfig{
   ProcessorFactory: &tunnel.SimpleProcessFactory{
      CustomValue: "user custom interface{} value",
      ProcessFunc: exampleConsumeFunction,
   },
}

//use TunnelDaemon to consume tunnel with specified tunnelId
daemon := tunnel.NewTunnelDaemon(tunnelClient, tunnelId, workConfig)
log.Fatal(daemon.Run())
```

* delete tunnel
```go
req := &tunnel.DeleteTunnelRequest {
   TableName: "testTable",
   TunnelName: "testTunnel",
}
_, err := tunnelClient.DeleteTunnel(req)
if err != nil {
   log.Fatal("delete test tunnel failed", err)
}
```

See the sample directory for more details.

### tunnel document

* [Godoc](todo)
* [API document](todo)

### configuration

* Default TunnelConfig definition

```go
var DefaultTunnelConfig = &TunnelConfig{
      //Max backoff retry duration.
      MaxRetryElapsedTime: 75 * time.Second,
      //HTTP request timeout.
      RequestTimeout:      60 * time.Second,
      //http.DefaultTransport.
      Transport:           http.DefaultTransport,
}
```

* TunnelWorkerConfig definition

```go
type TunnelWorkerConfig struct {
   //The heartbeat timeout time of the worker. If nil, the default value is used.
   HeartbeatTimeout  time.Duration
   //The heartbeat interval time of the worker. If nil, the default value is used.
   HeartbeatInterval time.Duration
   //The channel connection dial interface. If nil, the default dialer is used.
   //Usually the default dialer is fine.
   ChannelDialer     ChannelDialer

   //The channel processor creation interface.
   //It's recomended to use the pre-defined SimpleChannelProcessorFactory.
   ProcessorFactory ChannelProcessorFactory

   //zap log config. If nil, the DefaultLogConfig is used.
   LogConfig      *zap.Config
   //zap log rotate config. If nil, the DefaultSyncer is used.
   LogWriteSyncer zapcore.WriteSyncer
}
```
