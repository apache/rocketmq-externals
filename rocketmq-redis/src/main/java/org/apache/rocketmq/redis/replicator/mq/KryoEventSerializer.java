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

package org.apache.rocketmq.redis.replicator.mq;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.util.ByteArrayList;
import com.moilioncircle.redis.replicator.util.ByteArrayMap;
import com.moilioncircle.redis.replicator.util.ByteArraySet;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class KryoEventSerializer implements Serializer<Event> {

    private static final ThreadLocal<Wrapper> WRAPPERS = new ThreadLocal<>();

    private final int bufferSize;
    private final int maxBufferSize;
    private List<Class<?>> classes = new ArrayList<>();

    public KryoEventSerializer() {
        this(8192, -1);
    }

    public KryoEventSerializer(final int maxBufferSize) {
        this(8192, maxBufferSize);
    }

    public KryoEventSerializer(final int bufferSize, final int maxBufferSize) {
        this.bufferSize = bufferSize;
        this.maxBufferSize = maxBufferSize;
    }

    public void register(Class<?> clazz) {
        this.classes.add(clazz);
    }

    @Override
    public Event read(byte[] bytes) {
        return getWrapper().read(bytes);
    }

    @Override
    public byte[] write(Event event) {
        return getWrapper().write(event);
    }

    private Wrapper getWrapper() {
        Wrapper r = WRAPPERS.get();
        if (r != null) return r;
        r = new Wrapper();
        r.bufferSize = bufferSize;
        r.maxBufferSize = maxBufferSize;
        r.kryo = new Kryo();
        r.kryo.register(ArrayList.class);
        r.kryo.register(ByteArrayMap.class);
        r.kryo.register(ByteArrayList.class);
        r.kryo.register(ByteArraySet.class);
        r.kryo.register(LinkedHashSet.class);
        r.kryo.register(LinkedHashMap.class);
        for (Class<?> clazz : classes) r.kryo.register(clazz);
        r.kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        WRAPPERS.set(r);
        return r;
    }

    private static class Wrapper {
        //
        private Kryo kryo;
        private int bufferSize;
        private int maxBufferSize;

        public Event read(final byte[] bytes) {
            Input input = new Input();
            input.setBuffer(bytes);
            return (Event)this.kryo.readClassAndObject(input);
        }

        public byte[] write(final Event value) {
            Output output = new Output(bufferSize, maxBufferSize);
            this.kryo.writeClassAndObject(output, value);
            return output.toBytes();
        }
    }
}
