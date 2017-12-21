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

#ifndef BASE64_H
# define BASE64_H

/* Get size_t. */
# include <stddef.h>

/* Get bool. */
# include <stdbool.h>


#ifdef __cplusplus
namespace rocketmqSignature{
#endif

		/* This uses that the expression (n+(k-1))/k means the smallest
			 integer >= n/k, i.e., the ceiling of n/k.  */
# define BASE64_LENGTH(inlen) ((((inlen) + 2) / 3) * 4)

extern bool isbase64(char ch);

extern void base64_encode(const char *in, size_t inlen,
	char *out, size_t outlen);

extern size_t base64_encode_alloc(const char *in, size_t inlen, char **out);

extern bool base64_decode(const char *in, size_t inlen,
	char *out, size_t *outlen);

extern bool base64_decode_alloc(const char *in, size_t inlen,
	char **out, size_t *outlen);

#ifdef __cplusplus
}
#endif

#endif /* BASE64_H */
