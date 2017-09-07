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

package org.apache.rocketmq.redis.replicator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import sun.nio.cs.ThreadLocalCoders;

public final class RedisURI implements Comparable<RedisURI> {

    private String host;
    private String path;
    private String query;
    private int port = -1;
    private String scheme;
    private String userInfo;
    private String fragment;
    private String authority;
    private FileType fileType;

    private URI uri;
    Map<String, String> parameters = new HashMap<>();

    public RedisURI(String uri) throws URISyntaxException {
        parse(uri);
    }

    private void parse(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
        if (this.uri.getScheme().equalsIgnoreCase("redis")) {
            this.scheme = "redis";
        } else {
            throw new IllegalArgumentException("scheme must be [redis].");
        }
        this.path = this.uri.getPath();
        this.host = this.uri.getHost();
        this.query = this.uri.getQuery();
        this.fragment = this.uri.getFragment();
        this.userInfo = this.uri.getUserInfo();
        this.authority = this.uri.getAuthority();
        this.port = this.uri.getPort() == -1 ? 6379 : this.uri.getPort();
        if (this.path != null && this.userInfo == null) {
            int idx = this.path.lastIndexOf('.');
            if (idx >= 0) {
                String type = this.path.substring(idx + 1);
                this.fileType = FileType.parse(type);
            }
        }

        String rawQuery = this.uri.getRawQuery();
        if (rawQuery == null)
            return;

        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        StringBuilder sb = key;
        for (char c : rawQuery.toCharArray()) {
            switch (c) {
                case '&':
                case ';':
                    if (key.length() > 0 && value.length() > 0) {
                        parameters.put(decode(key.toString()), decode(value.toString()));
                    }
                    key.setLength(0);
                    value.setLength(0);
                    sb = key;
                    break;
                case '=':
                    sb = value;
                    break;
                default:
                    sb.append(c);
            }
        }
        if (key.length() > 0 && value.length() > 0) {
            parameters.put(decode(key.toString()), decode(value.toString()));
        }
    }

    private static String decode(String s) {
        if (s == null)
            return null;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuilder sb = new StringBuilder(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n; ) {
            assert c == s.charAt(i);
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            while (true) {
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 0xF) << 4) | ((decode(c2) & 0xF) << 0));
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getFragment() {
        return fragment;
    }

    public String getAuthority() {
        return authority;
    }

    public FileType getFileType() {
        return fileType;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RedisURI))
            return false;
        return this.uri.equals(((RedisURI) o).uri);
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }

    @Override
    public int compareTo(RedisURI that) {
        return this.uri.compareTo(that.uri);
    }

    public URL toURL() throws MalformedURLException {
        Objects.requireNonNull(getFileType());
        try {
            return new URI("file", uri.getRawAuthority(), uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment()).toURL();
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return this.uri.toString();
    }

}
