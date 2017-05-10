package rocketmq

type RebalanceController struct {
	clientFactory *ClientFactory
}

func NewRebalanceController(clientFactory *ClientFactory) *RebalanceController {
	return &RebalanceController{
		clientFactory: clientFactory,
	}
}

func (self *RebalanceController) doRebalance() {
	for _, consumer := range self.clientFactory.ConsumerTable {
		consumer.rebalance.DoRebalance()
	}
}
