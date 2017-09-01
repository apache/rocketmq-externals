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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.redis.replicator.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import org.apache.rocketmq.redis.replicator.Configuration;

public class RedisSocketFactory extends SocketFactory {

    protected final Configuration configuration;

    public RedisSocketFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        if (configuration.isSsl()) {
            return buildSsl(build(configuration.getSslSocketFactory().createSocket(host, port)), host);
        } else {
            return build(new Socket(host, port));
        }
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        if (configuration.isSsl()) {
            return buildSsl(build(configuration.getSslSocketFactory().createSocket(host, port, localAddr, localPort)), host);
        } else {
            return build(new Socket(host, port, localAddr, localPort));
        }
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        if (configuration.isSsl()) {
            return buildSsl(build(configuration.getSslSocketFactory().createSocket(address, port)), address.getHostAddress());
        } else {
            return build(new Socket(address, port));
        }
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        if (configuration.isSsl()) {
            return buildSsl(build(configuration.getSslSocketFactory().createSocket(address, port, localAddr, localPort)), address.getHostAddress());
        } else {
            return build(new Socket(address, port, localAddr, localPort));
        }
    }

    public Socket createSocket(String host, int port, int timeout) throws IOException {
        Socket socket = new Socket();
        build(socket);
        socket.connect(new InetSocketAddress(host, port), timeout);
        if (configuration.isSsl()) {
            socket = configuration.getSslSocketFactory().createSocket(socket, host, port, true);
            return buildSsl(socket, host);
        } else {
            return socket;
        }
    }

    private Socket build(Socket socket) throws SocketException {
        socket.setReuseAddress(true);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(true, 0);
        if (configuration.getReadTimeout() > 0) {
            socket.setSoTimeout(configuration.getReadTimeout());
        }
        if (configuration.getReceiveBufferSize() > 0) {
            socket.setReceiveBufferSize(configuration.getReceiveBufferSize());
        }
        if (configuration.getSendBufferSize() > 0) {
            socket.setSendBufferSize(configuration.getSendBufferSize());
        }
        return socket;
    }

    private Socket buildSsl(Socket socket, String host) throws SocketException {
        if (configuration.getSslParameters() != null) {
            ((SSLSocket) socket).setSSLParameters(configuration.getSslParameters());
        }
        if (configuration.getHostnameVerifier() != null && !configuration.getHostnameVerifier().verify(host, ((SSLSocket) socket).getSession())) {
            throw new SocketException("the connection to " + host + " failed ssl/tls hostname verification.");
        }
        return socket;
    }
}
