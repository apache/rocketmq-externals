# RocketMQ Go Client Release-Notes

## 2.0.0 - Alpha2

Many features have not a independent issue, you can find them in Roadmap issue: [2.0.0-alpha2 Roadmap](https://github.com/apache/rocketmq-client-go/issues/75)

### Feature
- [[PR-86](https://github.com/apache/rocketmq-client-go/issues/86)] - Support Interceptor  
- [[PR-96](https://github.com/apache/rocketmq-client-go/pull/96)] - Support multiple NameServer  
- [[PR-102](https://github.com/apache/rocketmq-client-go/pull/102)] - Support QueueSelector  
- [[PR-100](https://github.com/apache/rocketmq-client-go/pull/100)] - Support SendAsync  
- [[PR-106](https://github.com/apache/rocketmq-client-go/pull/106)] - Support PullConsumer  
- [[PR-113](https://github.com/apache/rocketmq-client-go/pull/113)] - Support Statistics of client  
- [[PR-115](https://github.com/apache/rocketmq-client-go/pull/115)] - Support order message in PushConsumer  
- [[PR-117](https://github.com/apache/rocketmq-client-go/pull/117)] - Support ACL 
- [[PR-119](https://github.com/apache/rocketmq-client-go/pull/119/)] - Support retry when consume failed in PushConsumer  

### Improvement
- [[ISSUE-93](https://github.com/apache/rocketmq-client-go/issues/93)] - refactor API to make more usability.
- [[PR-107](https://github.com/apache/rocketmq-client-go/pull/107)] - return an error in `start()`  instead of fatal directly
- [[PR-109](https://github.com/apache/rocketmq-client-go/pull/109)] - add unit test for producer
- [[PR-116](https://github.com/apache/rocketmq-client-go/pull/116)] - add unit test for internal/route.go

### Bug
- [[ISSUE-65](https://github.com/apache/rocketmq-client-go/issues/65)] - Fixed the issue that Missing Tag and Properties in Message when use producer.
- [[PR-97](https://github.com/apache/rocketmq-client-go/pull/97)] - Fixed the issue that The timeout of RPC doesn't work and The InvokeAsync doesn't work
