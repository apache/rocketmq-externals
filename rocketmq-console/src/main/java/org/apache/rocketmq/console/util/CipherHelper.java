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

package org.apache.rocketmq.console.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;

public class CipherHelper {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String ALGORITHM = "AES";

    private SecretKeySpec secretKeySpec;

    public CipherHelper(String ciperKey) throws UnsupportedEncodingException {
        secretKeySpec = new SecretKeySpec(ciperKey.getBytes("UTF-8"), ALGORITHM);
    }

    /**
     * encrypt
     * @param toBeEncrypt
     * @return
     */
    public String encrypt(String toBeEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(toBeEncrypt.getBytes());
            return Base64.encodeBase64String(encrypted);
        } catch (Exception e) {
            logger.error("encrypt:{} err", toBeEncrypt, e);
        }
        return null;
    }

    /**
     * decrypt
     * @param encrypted
     * @return
     */
    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.decodeBase64(encrypted));
            return new String(decryptedBytes);
        } catch (Exception e) {
            logger.error("decrypt:{} err", encrypted, e);
        }
        return null;
    }
}
