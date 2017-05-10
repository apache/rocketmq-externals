package rocketmq

import ()
import (
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/config"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/model/constant"
	"github.com/apache/incubator-rocketmq-externals/rocketmq-go/service"
)

type RocketMQProducer interface {
	Send(message *model.Message) (sendResult *model.SendResult, err error)
	SendWithTimeout(message *model.Message, timeout int) (sendResult *model.SendResult, err error)
	//SendAsync(message *model.Message) (sendResult *model.SendResult,err error)
	//SendAsyncWithTimeout(message *model.Message) (sendResult *model.SendResult,err error)
	//SendOneWay(message *model.Message) (sendResult *model.SendResult,err error)
}
type DefaultMQProducer struct {
	producerGroup  string
	ProducerConfig *config.RocketMqProducerConfig

	producerService service.ProducerService
}

func NewDefaultMQProducer(producerGroup string) (rocketMQProducer *DefaultMQProducer) {
	rocketMQProducer = &DefaultMQProducer{
		producerGroup:  producerGroup,
		ProducerConfig: config.NewProducerConfig(),
	}
	return
}

func (self *DefaultMQProducer) Send(message *model.Message) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", self.ProducerConfig.SendMsgTimeout)
	return
}
func (self *DefaultMQProducer) SendWithTimeout(message *model.Message, timeout int64) (sendResult *model.SendResult, err error) {
	sendResult, err = self.producerService.SendDefaultImpl(message, constant.COMMUNICATIONMODE_SYNC, "", timeout)
	return
}

//private String producerGroup;
//#private String createTopicKey = MixAll.DEFAULT_TOPIC;
//private volatile int defaultTopicQueueNums = 4;
//private int sendMsgTimeout = 3000;
//private int compressMsgBodyOverHowmuch = 1024 * 4;
//private int retryTimesWhenSendFailed = 2;
//private int retryTimesWhenSendAsyncFailed = 2;
//
//private boolean retryAnotherBrokerWhenNotStoreOK = false;
//private int maxMessageSize = 1024 * 1024 * 4; // 4M

//#innner
// todo
// private final ConcurrentHashMap<String/* topic */, TopicPublishInfo> topicPublishInfoTable = new ConcurrentHashMap<String, TopicPublishInfo>();
//private int zipCompressLevel = Integer.parseInt(System.getProperty(MixAll.MESSAGE_COMPRESS_LEVEL, "5")); 压缩
//private MQFaultStrategy mqFaultStrategy = new MQFaultStrategy(); 失败策略

//private final ArrayList<SendMessageHook> sendMessageHookList = new ArrayList<SendMessageHook>();
//private final RPCHook rpcHook;
//protected BlockingQueue<Runnable> checkRequestQueue;
//protected ExecutorService checkExecutor;
//private ServiceState serviceState = ServiceState.CREATE_JUST;
//private MQClientInstance mQClientFactory;
//private ArrayList<CheckForbiddenHook> checkForbiddenHookList = new ArrayList<CheckForbiddenHook>();
