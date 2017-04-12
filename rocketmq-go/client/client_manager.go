package client

import (
	"../hook"
	"sync"
	"sync/atomic"
)

var lock sync.RWMutex

var indexGenerator int32
var clientInstanceTable map[string]*MQClientInstance

func CreateMQClientInstance(config *ClientConfig, hook hook.RPCHook) *MQClientInstance {
	id := config.BuildMQClientId()
	lock.Lock()
	defer lock.Unlock()

	instance, existed := clientInstanceTable[id]
	if !existed {
		indexGenerator = atomic.AddInt32(&indexGenerator, 1)
		instance = NewMQClientInstance(config, int(indexGenerator), id)
		clientInstanceTable[id] = instance
		// TODO: log.info("Created new MQClientInstance for clientId:[{}]", clientId);
	}
	// log.warn("Returned Previous MQClientInstance for clientId:[{}]", clientId);
	return instance
}

func RomoveMQClientInstance(id string) {
	lock.Lock()
	defer lock.Unlock()
	delete(clientInstanceTable, id)
}
