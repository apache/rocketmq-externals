package org.apache.rocketmq.mqtt.protocol.header;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

public class CloseClientConnectionRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private boolean currentCleanSession;

    @CFNotNull
    private String clientId;

    public boolean isCurrentCleanSession() {
        return currentCleanSession;
    }

    public void setCurrentCleanSession(boolean currentCleanSession) {
        this.currentCleanSession = currentCleanSession;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override public void checkFields() throws RemotingCommandException {

    }
}
