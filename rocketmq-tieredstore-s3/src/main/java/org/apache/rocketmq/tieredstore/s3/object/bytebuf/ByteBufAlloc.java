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

package org.apache.rocketmq.tieredstore.s3.object.bytebuf;

import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

public class ByteBufAlloc {

    /**
     * The policy used to allocate memory.
     */
    private static ByteBufAllocPolicy policy = ByteBufAllocPolicy.POOLED_HEAP;
    /**
     * The allocator used to allocate memory. It should be updated when {@link #policy} is updated.
     */
    private static AbstractByteBufAllocator allocator = getAllocatorByPolicy(policy);

    /**
     * Set the policy used to allocate memory.
     */
    public static void setPolicy(ByteBufAllocPolicy policy) {
        ByteBufAlloc.policy = policy;
        ByteBufAlloc.allocator = getAllocatorByPolicy(policy);
    }

    public static ByteBufAllocPolicy getPolicy() {
        return policy;
    }

    public static CompositeByteBuf compositeByteBuffer() {
        return allocator.compositeDirectBuffer(Integer.MAX_VALUE);
    }

    public static ByteBuf byteBuffer(int initCapacity) {
        try {
            return policy.isDirect() ? allocator.directBuffer(initCapacity) : allocator.heapBuffer(initCapacity);
        }
        catch (OutOfMemoryError e) {
            System.err.println("alloc buffer OOM");
            Runtime.getRuntime().halt(1);
            throw e;
        }
    }

    private static AbstractByteBufAllocator getAllocatorByPolicy(ByteBufAllocPolicy policy) {
        if (policy.isPooled()) {
            return PooledByteBufAllocator.DEFAULT;
        }
        return UnpooledByteBufAllocator.DEFAULT;
    }

}
