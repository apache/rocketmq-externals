package client

import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/hook"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/message"
)

var sendSmartMsg bool = true // TODO get from system env

type TopAddress struct {
}

type ClientRemotingProcessor interface {
}

func init() {
	//os.Setenv(Remo)
	// TODO
	//static {
	//	System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
	//}
}

type MQClientAPI struct {
	remotingClient    *rocketmq.RemotingClient
	topAddress        *TopAddress
	crp               *ClientRemotingProcessor
	nameServerAddress string
	config            *ClientConfig
}

func NewMQClientAPI(cfg *ClientConfig, processor *ClientRemotingProcessor, hook hook.RPCHook) *MQClientAPI {
	api := &MQClientAPI{
		remotingClient: &rocketmq.RemotingClient{}, //TODO
		topAddress:     &TopAddress{},              // TODO
		crp:            processor,
		config:         cfg,
	}

	// TODO
	//this.remotingClient.registerRPCHook(rpcHook);
	//this.remotingClient.registerProcessor(RequestCode.CHECK_TRANSACTION_STATE, this.clientRemotingProcessor, null);
	//
	//this.remotingClient.registerProcessor(RequestCode.NOTIFY_CONSUMER_IDS_CHANGED, this.clientRemotingProcessor, null);
	//
	//this.remotingClient.registerProcessor(RequestCode.RESET_CONSUMER_CLIENT_OFFSET, this.clientRemotingProcessor, null);
	//
	//this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT, this.clientRemotingProcessor, null);
	//
	//this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_RUNNING_INFO, this.clientRemotingProcessor, null);
	//
	//this.remotingClient.registerProcessor(RequestCode.CONSUME_MESSAGE_DIRECTLY, this.clientRemotingProcessor, null);
	return api
}

func (api *MQClientAPI) SendMessage(addr, brokerName string,
	message msg.Message, requestHeader rocketmq.SendMessageRequestHeader, timeout int64) *rocketmq.SendResult {
	var request *rocketmq.RemotingCommand
	request = rocketmq.CreateRemotingCommand(rocketmq.SendMsg, &requestHeader)
	request.SetBody(message.Body)
	return api.sendMessageSync(addr, brokerName, msg, timeout, request)
}

//TODO

func (api *MQClientAPI) sendMessageSync(addr, brokerName string,
	msg msg.Message,
	timeout int64,
	request *rocketmq.RemotingCommand) *rocketmq.SendResult {
	response := api.invokeSync(addr, request, timeout)
	if response == nil {
		panic("invokeSync panci!")
	}
	return nil
	// TODO return api.processSendResponse(brokerName, msg, response)
}

func (api *MQClientAPI) invokeSync(addr string, cmd *rocketmq.RemotingCommand, timeout int64) *rocketmq.RemotingCommand {
	return nil
}

func (api *MQClientAPI) processSendResponse(name string, msg msg.Message, cmd *rocketmq.RemotingCommand) *rocketmq.RemotingCommand {
	return nil
}
