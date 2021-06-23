# timeline

timeline is a package that provides functionality for easily implementing social scene application such as IM app, feed stream app.

timeline is based on [TableStore](https://cn.aliyun.com/product/ots
) timeline model.

## Installation

```
$ go get github.com/aliyun/aliyun-tablestore-go-sdk
```

## Sample

* im application demo:
```
$ cd timeline/sample/im
$ go run main.go im.go
```

* feed stream demo:
```
$ cd timeline/sample/feed
$ go run main.go feed.go
```

## Benchmark

**VM OS:** Aliyun Linux 17.1 64bit

**VM Configuration:** 4 CPU, 8 GB RAM.

**GO VERSION:** 1.10.3 linux/amd64

**Timeline Message Size:** almost 220 bytes

* on TableStore SSD High-performance instance
```
$ cd timeline
$ go test -bench=. -benchtime=10s -test.cpu=12
goos: linux
goarch: amd64
pkg: github.com/aliyun/aliyun-tablestore-go-sdk/timeline
BenchmarkTmLine_BatchStore_Concurrent-12                       	   10000	   1737993 ns/op
BenchmarkTmLine_BatchStore_WriteSpread-12                      	     100	 127729883 ns/op
BenchmarkTmLine_BatchStore_WriteSpread_IgnoreMessageLost-12    	     200	  80166859 ns/op
PASS
```

* on TableStore Capacity instance

```
$ cd timeline
$ go test -bench=. -benchtime=10s -test.cpu=12
goos: linux
goarch: amd64
pkg: github.com/aliyun/aliyun-tablestore-go-sdk/timeline
BenchmarkTmLine_BatchStore_Concurrent-12                       	   10000	   1791522 ns/op
BenchmarkTmLine_BatchStore_WriteSpread-12                      	     100	 124597783 ns/op
BenchmarkTmLine_BatchStore_WriteSpread_IgnoreMessageLost-12    	     200	  83780501 ns/op
```
