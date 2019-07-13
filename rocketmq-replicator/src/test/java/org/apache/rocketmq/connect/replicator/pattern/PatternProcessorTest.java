package org.apache.rocketmq.connect.replicator.pattern;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.DataVersion;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerOffsetSerializeWrapper;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.connect.replicator.Config;
import org.apache.rocketmq.connect.replicator.Replicator;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PatternProcessor.class)
public class PatternProcessorTest {

    PatternProcessor patternProcessor;

    Replicator replicator;

    Config config;

    MQClientAPIImpl clientAPIImpl;

    RemotingClient remotingClient;

    SubscriptionGroupWrapper subscriptionGroupWrapper;

    ConsumerOffsetSerializeWrapper consumerOffsetSerializeWrapper;

    @Before
    public void before() {
        config = new Config();
        replicator = new Replicator(config);
        patternProcessor = PowerMockito.spy(new PatternProcessor(replicator));

        try {
            clientAPIImpl = Mockito.mock(MQClientAPIImpl.class);
            MemberModifier.field(PatternProcessor.class, "clientAPIImpl").set(patternProcessor, clientAPIImpl);
            remotingClient = Mockito.mock(RemotingClient.class);
            MemberModifier.field(PatternProcessor.class, "remotingClient").set(patternProcessor, remotingClient);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private ClusterInfo getClusterInfo() {
        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String/* brokerName */, BrokerData> brokerAddrTable = new HashMap<String, BrokerData>();
        BrokerData brokerData = new BrokerData();
        HashMap<Long/* brokerId */, String/* broker address */> brokerAddrs = new HashMap<Long, String>();
        brokerAddrs.put(1L, "127.0.0.1");
        brokerAddrs.put(2L, "127.0.0.2");
        brokerAddrs.put(3L, "127.0.0.3");
        brokerData.setBrokerAddrs(brokerAddrs);

        brokerAddrTable.put("broker-a", brokerData);

        brokerData = new BrokerData();
        brokerAddrs = new HashMap<Long, String>();
        brokerAddrs.put(1L, "127.0.0.4");
        brokerAddrs.put(2L, "127.0.0.5");
        brokerAddrs.put(3L, "127.0.0.6");
        brokerData.setBrokerAddrs(brokerAddrs);
        brokerAddrTable.put("broker-b", brokerData);

        clusterInfo.setBrokerAddrTable(brokerAddrTable);
        return clusterInfo;

    }

    private RemotingCommand getSubscriptionGroupWrapper() {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);

        SubscriptionGroupWrapper content = new SubscriptionGroupWrapper();
        subscriptionGroupWrapper = content;
        ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable =
            new ConcurrentHashMap<String, SubscriptionGroupConfig>(1024);
        content.setSubscriptionGroupTable(subscriptionGroupTable);
        content.setDataVersion(new DataVersion());
        response.setBody(content.encode());
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getConsumerOffsetSerializeWrapper() {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);

        ConsumerOffsetSerializeWrapper content = new ConsumerOffsetSerializeWrapper();
        consumerOffsetSerializeWrapper = content;
        ConcurrentMap<String/* topic@group */, ConcurrentMap<Integer, Long>> offsetTable =
            new ConcurrentHashMap<String, ConcurrentMap<Integer, Long>>(512);
        ConcurrentMap<Integer, Long> queueOffset = new ConcurrentHashMap<>();
        queueOffset.put(1, 123L);
        queueOffset.put(2, 123L);
        queueOffset.put(3, 123L);
        offsetTable.put("topic@group", queueOffset);
        offsetTable.put("topic@group1", queueOffset);
        offsetTable.put("topic@group2", queueOffset);
        
        content.setOffsetTable(offsetTable);
        response.setBody(content.encode());
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getAllDelayOffset() {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        response.setBody("1024".getBytes());
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    @Test
    public void executeTest() throws Exception {
        PowerMockito.doReturn(getClusterInfo()).when(patternProcessor, "getBrokerClusterInfo");
        PowerMockito.doNothing().when(patternProcessor).getBrokerInfo(ArgumentMatchers.anyString());
        patternProcessor.execute();
        Thread.sleep(50);
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(6)).invoke("getBrokerInfo", ArgumentMatchers.anyString());
    }

    public void executeExceptionTest() {
        try {
            PowerMockito.doThrow(new RemotingException("test exception")).when(patternProcessor, "getBrokerClusterInfo");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        patternProcessor.execute();
    }

    @Test
    public void getBrokerInfoTest() throws Exception {
        Mockito.when(clientAPIImpl.getBrokerClusterInfo(ArgumentMatchers.anyLong())).thenReturn(getClusterInfo());
        Mockito.when(clientAPIImpl.getBrokerConfig(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())).thenReturn(null);
        Mockito.when(clientAPIImpl.getAllTopicConfig(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())).thenReturn(new TopicConfigSerializeWrapper());

        Mockito.when(remotingClient.invokeSync(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyLong()))
            .thenReturn(getAllDelayOffset())
            .thenReturn(getConsumerOffsetSerializeWrapper())
            .thenReturn(getSubscriptionGroupWrapper())
        .thenReturn(getAllDelayOffset())
        .thenReturn(getConsumerOffsetSerializeWrapper())
        .thenReturn(getSubscriptionGroupWrapper());

        List<ConsumerConnection> list = new ArrayList<ConsumerConnection>(12);
        PowerMockito.doReturn(list).when(patternProcessor, "getAllConsumerConnectionToConsumerOffset", ArgumentMatchers.anyString(), ArgumentMatchers.any(ConsumerOffsetSerializeWrapper.class));

        patternProcessor.getBrokerInfo("");

        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getBrokerConfig", ArgumentMatchers.anyString());
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllTopicConfig", ArgumentMatchers.anyString());
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllDelayOffset", ArgumentMatchers.anyString());

        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllConsumerOffset", ArgumentMatchers.anyString());
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllSubscriptionGroup", ArgumentMatchers.anyString());
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllConsumerConnectionToConsumerOffset", ArgumentMatchers.anyString(), ArgumentMatchers.any(ConsumerOffsetSerializeWrapper.class));
        PowerMockito.verifyPrivate(patternProcessor, Mockito.never()).invoke("getAllConsumerConnectionToTopicConfig", ArgumentMatchers.anyString(), ArgumentMatchers.any(TopicConfigSerializeWrapper.class));
        
        config.setSyncConsumerOffset(false);
        patternProcessor.getBrokerInfo("");
        PowerMockito.verifyPrivate(patternProcessor, Mockito.times(1)).invoke("getAllConsumerConnectionToTopicConfig", ArgumentMatchers.anyString(), ArgumentMatchers.any(TopicConfigSerializeWrapper.class));
    }
    
    @Test
    public void test() throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        MQClientInstance mqClientInstance = MQClientManager.getInstance().getAndCreateMQClientInstance(new ClientConfig(), null);
        config.setNameServerAddress("127.0.0.1:9876");
        mqClientInstance.getMQClientAPIImpl().updateNameServerAddressList(config.getNameServerAddress());
        MQClientAPIImpl clientAPIImpl = mqClientInstance.getMQClientAPIImpl();
    	clientAPIImpl.getBrokerConfig("127.0.0.1:10911", config.getTimeoutMillis());
    	Thread.sleep(5000);
    }
}
