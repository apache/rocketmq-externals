package org.apache.rocketmq.mqtt.processor;

import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.protocol.header.CloseClientConnectionRequestHeader;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotingCommandRequestProcessor implements NettyRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(RemotingCommandRequestProcessor.class);

    protected final MqttBridgeController mqttBridgeController;

    public RemotingCommandRequestProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        if (ctx != null) {
            log.debug("receive request, {} {} {}",
                request.getCode(),
                RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
                request);
        }
        switch (request.getCode()) {
            case RequestCode.CLOSE_MQTTCLIENT_CONNECTION:
                return closeMqttClientConnection(ctx, request);
            default:
                break;
        }
        return null;
    }

    private RemotingCommand closeMqttClientConnection(ChannelHandlerContext ctx,
        RemotingCommand request) throws RemotingCommandException {
        CloseClientConnectionRequestHeader requestHeader = (CloseClientConnectionRequestHeader) request.decodeCommandCustomHeader(CloseClientConnectionRequestHeader.class);
        MQTTSession client = (MQTTSession) this.mqttBridgeController.getPersistService().getClientByClientId(requestHeader.getClientId());
        log.info("Receive Close Connection Requst from Bridge：[{}], New cleanSession is [{}], and old client is [{}] ", ctx.channel().remoteAddress(), requestHeader.isCurrentCleanSession(), client.toString());

        //要清理的Mqtt Client 并非由当前Mqtt Bridge管理
        if (!client.getMqttBridgeAddr().equals(this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP())) {
            return RemotingCommand.createResponseCommand(ResponseCode.CLIENT_NOT_MANAGED_BY_CURRENT_BRIDGE, null);
        }

        // 当前新连接cleanSession=true,应该断开本地连接（若存在），抛弃之前所有持久化信息，清理本地缓存；
        // 当前新连接cleanSession=false, 旧连接cleanSession=true, 断开连接（若存在），清理缓存，清理持久化信息；旧连接cleanSession=false, 断开连接（若存在），清理缓存，持久化的信息更新由新连接到的bridge更新
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        RemotingChannel channel = iotClientManager.getChannel(client.getClientId());
        if (channel != null) {
            iotClientManager.onClose(channel);
            channel.close();
        }

        if (requestHeader.isCurrentCleanSession() || client.isCleanSession()) {
            iotClientManager.cleanSessionState(client);
            iotClientManager.deletePersistConsumeOffset(client);
        }

        RemotingCommand response = RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null);
        return response;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
