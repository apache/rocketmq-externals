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

package org.apache.rocketmq.jms.msg.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.jms.JMSException;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ObjectSerialize implements Serialize<Object> {

    private static ObjectSerialize ins = new ObjectSerialize();

    public static ObjectSerialize instance() {
        return ins;
    }

    private ObjectSerialize() {
    }

    public byte[] serialize(Object object) throws JMSException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            baos.close();
            return baos.toByteArray();
        }
        catch (IOException e) {
            throw new JMSException(ExceptionUtils.getStackTrace(e));
        }
    }

    public Serializable deserialize(byte[] bytes) throws JMSException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.close();
            bais.close();
            return (Serializable) ois.readObject();
        }
        catch (IOException e) {
            throw new JMSException(e.getMessage());
        }
        catch (ClassNotFoundException e) {
            throw new JMSException(e.getMessage());
        }
    }
}
