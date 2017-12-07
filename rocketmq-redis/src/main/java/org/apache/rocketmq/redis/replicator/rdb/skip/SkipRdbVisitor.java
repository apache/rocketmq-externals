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

package org.apache.rocketmq.redis.replicator.rdb.skip;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.event.Event;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.DefaultRdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.datatype.DB;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleParser;

import static org.apache.rocketmq.redis.replicator.RedisConstants.MODULE_SET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_MODULE_OPCODE_EOF;

public class SkipRdbVisitor extends DefaultRdbVisitor {

    public SkipRdbVisitor(Replicator replicator) {
        super(replicator);
    }

    @Override
    public DB applySelectDB(RedisInputStream in, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadLen();
        return null;
    }

    @Override
    public DB applyResizeDB(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadLen();
        parser.rdbLoadLen();
        return null;
    }

    @Override
    public Event applyExpireTime(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadTime();
        int valueType = applyType(in);
        rdbLoadObject(in, db, valueType, version);
        return null;
    }

    @Override
    public Event applyExpireTimeMs(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadMillisecondTime();
        int valueType = applyType(in);
        rdbLoadObject(in, db, valueType, version);
        return null;
    }

    @Override
    public Event applyAux(RedisInputStream in, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadEncodedStringObject();
        return null;
    }

    @Override
    public Event applyString(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadEncodedStringObject();
        return null;
    }

    @Override
    public Event applyList(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        for (int i = 0; i < len; i++) {
            parser.rdbLoadEncodedStringObject();
        }
        return null;
    }

    @Override
    public Event applySet(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        for (int i = 0; i < len; i++) {
            parser.rdbLoadEncodedStringObject();
        }
        return null;
    }

    @Override
    public Event applyZSet(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        while (len > 0) {
            parser.rdbLoadEncodedStringObject();
            parser.rdbLoadDoubleValue();
            len--;
        }
        return null;
    }

    @Override
    public Event applyZSet2(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        while (len > 0) {
            parser.rdbLoadEncodedStringObject();
            parser.rdbLoadBinaryDoubleValue();
            len--;
        }
        return null;
    }

    @Override
    public Event applyHash(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        while (len > 0) {
            parser.rdbLoadEncodedStringObject();
            parser.rdbLoadEncodedStringObject();
            len--;
        }
        return null;
    }

    @Override
    public Event applyHashZipMap(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadPlainStringObject();
        return null;
    }

    @Override
    public Event applyListZipList(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadPlainStringObject();
        return null;
    }

    @Override
    public Event applySetIntSet(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadPlainStringObject();
        return null;
    }

    @Override
    public Event applyZSetZipList(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadPlainStringObject();
        return null;
    }

    @Override
    public Event applyHashZipList(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        parser.rdbLoadPlainStringObject();
        return null;
    }

    @Override
    public Event applyListQuickList(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        long len = parser.rdbLoadLen().len;
        for (int i = 0; i < len; i++) {
            parser.rdbGenericLoadStringObject();
        }
        return null;
    }

    @Override
    public Event applyModule(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        char[] c = new char[9];
        long moduleid = parser.rdbLoadLen().len;
        for (int i = 0; i < c.length; i++) {
            c[i] = MODULE_SET[(int) (moduleid >>> (10 + (c.length - 1 - i) * 6) & 63)];
        }
        String moduleName = new String(c);
        int moduleVersion = (int) (moduleid & 1023);
        ModuleParser<? extends Module> moduleParser = lookupModuleParser(moduleName, moduleVersion);
        if (moduleParser == null) {
            throw new NoSuchElementException("module[" + moduleName + "," + moduleVersion + "] not exist.");
        }
        moduleParser.parse(in, 1);
        return null;
    }

    @Override
    public Event applyModule2(RedisInputStream in, DB db, int version) throws IOException {
        SkipRdbParser parser = new SkipRdbParser(in);
        parser.rdbLoadEncodedStringObject();
        char[] c = new char[9];
        long moduleid = parser.rdbLoadLen().len;
        for (int i = 0; i < c.length; i++) {
            c[i] = MODULE_SET[(int) (moduleid >>> (10 + (c.length - 1 - i) * 6) & 63)];
        }
        String moduleName = new String(c);
        int moduleVersion = (int) (moduleid & 1023);
        ModuleParser<? extends Module> moduleParser = lookupModuleParser(moduleName, moduleVersion);
        if (moduleParser == null) {
            throw new NoSuchElementException("module[" + moduleName + "," + moduleVersion + "] not exist.");
        }
        moduleParser.parse(in, 2);
        long eof = parser.rdbLoadLen().len;
        if (eof != RDB_MODULE_OPCODE_EOF) {
            throw new UnsupportedOperationException("The RDB file contains module data for the module '" + moduleName + "' that is not terminated by the proper module value EOF marker");
        }
        return null;
    }
}
