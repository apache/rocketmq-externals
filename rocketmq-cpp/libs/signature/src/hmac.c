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

#include <string.h>
#include <stdint.h>

#include "hmac.h"
#include "sha1.h"
#include "sha256.h"
#include "sha512.h"

#ifdef __cplusplus
namespace rocketmqSignature{
#endif

#define IPAD 0x36
#define OPAD 0x5c

int hmac_sha1(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf)
{
	uint32_t i;
	struct sha1_ctx inner;
	struct sha1_ctx outer;
	struct sha1_ctx key_hash;
	char ipad[64] = {0};
	char opad[64] = {0};
	char key_buf[SHA1_DIGEST_SIZE] = {0};
	char inner_buf[SHA1_DIGEST_SIZE] = {0};

	if (key == NULL || data == NULL || ret_buf == NULL) return -1;

 	if (key_len > 64) {
		sha1_init_ctx(&key_hash);
		sha1_process_bytes(key, key_len, &key_hash);
		sha1_finish_ctx(&key_hash, key_buf);

		key = key_buf;
		key_len = SHA1_DIGEST_SIZE;
	}

	sha1_init_ctx (&inner);

	for (i = 0; i < 64; i++) {
		if (i < key_len) {
			ipad[i] = ((const char *)key)[i] ^ IPAD;
			opad[i] = ((const char *)key)[i] ^ OPAD;
		} else {
			ipad[i] = IPAD;
			opad[i] = OPAD;
		}
	}

  sha1_process_block (ipad, 64, &inner);
  sha1_process_bytes (data, data_len, &inner);

  sha1_finish_ctx (&inner, inner_buf);

  sha1_init_ctx (&outer);

  sha1_process_block (opad, 64, &outer);
  sha1_process_bytes (inner_buf, SHA1_DIGEST_SIZE, &outer);

  sha1_finish_ctx (&outer, ret_buf);

  return 0;
}

int hmac_sha256(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf)
{
	uint32_t i;
	struct sha256_ctx inner;
	struct sha256_ctx outer;
	struct sha256_ctx key_hash;
	char ipad[64] = {0};
	char opad[64] = {0};
	char key_buf[SHA256_DIGEST_SIZE] = {0};
	char inner_buf[SHA256_DIGEST_SIZE] = {0};

	if (key == NULL || data == NULL || ret_buf == NULL) return -1;

 	if (key_len > 64) {
		sha256_init_ctx(&key_hash);
		sha256_process_bytes(key, key_len, &key_hash);
		sha256_finish_ctx(&key_hash, key_buf);

		key = key_buf;
		key_len = SHA256_DIGEST_SIZE;
	}

	sha256_init_ctx (&inner);

	for (i = 0; i < 64; i++) {
		if (i < key_len) {
			ipad[i] = ((const char *)key)[i] ^ IPAD;
			opad[i] = ((const char *)key)[i] ^ OPAD;
		} else {
			ipad[i] = IPAD;
			opad[i] = OPAD;
		}
	}

  sha256_process_block (ipad, 64, &inner);
  sha256_process_bytes (data, data_len, &inner);

  sha256_finish_ctx (&inner, inner_buf);

  sha256_init_ctx (&outer);

  sha256_process_block (opad, 64, &outer);
  sha256_process_bytes (inner_buf, SHA256_DIGEST_SIZE, &outer);

  sha256_finish_ctx (&outer, ret_buf);

  return 0;
}

int hmac_sha512(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf)
{
	uint32_t i;
	struct sha512_ctx inner;
	struct sha512_ctx outer;
	struct sha512_ctx key_hash;
	char ipad[128] = {0};
	char opad[128] = {0};
	char key_buf[SHA512_DIGEST_SIZE] = {0};
	char inner_buf[SHA512_DIGEST_SIZE] = {0};

	if (key == NULL || data == NULL || ret_buf == NULL) return -1;

 	if (key_len > 128) {
		sha512_init_ctx(&key_hash);
		sha512_process_bytes(key, key_len, &key_hash);
		sha512_finish_ctx(&key_hash, key_buf);

		key = key_buf;
		key_len = SHA512_DIGEST_SIZE;
	}

	sha512_init_ctx (&inner);

	for (i = 0; i < 128; i++) {
		if (i < key_len) {
			ipad[i] = ((const char *)key)[i] ^ IPAD;
			opad[i] = ((const char *)key)[i] ^ OPAD;
		} else {
			ipad[i] = IPAD;
			opad[i] = OPAD;
		}
	}

  sha512_process_block (ipad, 128, &inner);
  sha512_process_bytes (data, data_len, &inner);

  sha512_finish_ctx (&inner, inner_buf);

  sha512_init_ctx (&outer);

  sha512_process_block (opad, 128, &outer);
  sha512_process_bytes (inner_buf, SHA512_DIGEST_SIZE, &outer);

  sha512_finish_ctx (&outer, ret_buf);

  return 0;
}
#ifdef __cplusplus
}
#endif