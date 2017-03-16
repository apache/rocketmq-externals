/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.model;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.protocol.body.Connection;

public class ConnectionInfo extends Connection {
    private String versionDesc;

    public static ConnectionInfo buildConnectionInfo(Connection connection) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setClientId(connection.getClientId());
        connectionInfo.setClientAddr(connection.getClientAddr());
        connectionInfo.setLanguage(connection.getLanguage());
        connectionInfo.setVersion(connection.getVersion());
        connectionInfo.setVersionDesc(MQVersion.getVersionDesc(connection.getVersion()));
        return connectionInfo;
    }

    public static HashSet<Connection> buildConnectionInfoHashSet(Collection<Connection> connectionList) {
        HashSet<Connection> connectionHashSet = Sets.newHashSet();
        for (Connection connection : connectionList) {
            connectionHashSet.add(buildConnectionInfo(connection));
        }
        return connectionHashSet;
    }

    public String getVersionDesc() {
        return versionDesc;
    }

    public void setVersionDesc(String versionDesc) {
        this.versionDesc = versionDesc;
    }
}
