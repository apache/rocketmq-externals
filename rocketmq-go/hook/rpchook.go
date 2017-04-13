package hook

import "github.com/apache/incubator-rocketmq-externals/rocketmq-go"

type RPCHook interface {
	DoBeforeRequest(string, *rocketmq.RemotingCommand)
	DoBeforeResponse(string, *rocketmq.RemotingCommand)
}
