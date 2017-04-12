package hook

import ptc "../protocol"

type RPCHook interface {
	DoBeforeRequest(string, *ptc.RemotingCommand)
	DoBeforeResponse(string, *ptc.RemotingCommand)
}
