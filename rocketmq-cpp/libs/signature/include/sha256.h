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

#ifndef SHA256_H
# define SHA256_H 1

# include <stdio.h>
# include <stdint.h>

#ifdef __cplusplus
namespace rocketmqSignature{
#endif

/* Structure to save state of computation between the single steps.  */
struct sha256_ctx
{
  uint32_t state[8];

  uint32_t total[2];
  size_t buflen;
  uint32_t buffer[32];
};

enum { SHA224_DIGEST_SIZE = 28 };
enum { SHA256_DIGEST_SIZE = 32 };

/* Initialize structure containing state of computation. */
extern void sha256_init_ctx (struct sha256_ctx *ctx);
extern void sha224_init_ctx (struct sha256_ctx *ctx);

/* Starting with the result of former calls of this function (or the
   initialization function update the context for the next LEN bytes
   starting at BUFFER.
   It is necessary that LEN is a multiple of 64!!! */
extern void sha256_process_block (const void *buffer, size_t len,
				  struct sha256_ctx *ctx);

/* Starting with the result of former calls of this function (or the
   initialization function update the context for the next LEN bytes
   starting at BUFFER.
   It is NOT required that LEN is a multiple of 64.  */
extern void sha256_process_bytes (const void *buffer, size_t len,
				  struct sha256_ctx *ctx);

/* Process the remaining bytes in the buffer and put result from CTX
   in first 32 (28) bytes following RESBUF.  The result is always in little
   endian byte order, so that a byte-wise output yields to the wanted
   ASCII representation of the message digest.  */
extern void *sha256_finish_ctx (struct sha256_ctx *ctx, void *resbuf);
extern void *sha224_finish_ctx (struct sha256_ctx *ctx, void *resbuf);


/* Put result from CTX in first 32 (28) bytes following RESBUF.  The result is
   always in little endian byte order, so that a byte-wise output yields
   to the wanted ASCII representation of the message digest.  */
extern void *sha256_read_ctx (const struct sha256_ctx *ctx, void *resbuf);
extern void *sha224_read_ctx (const struct sha256_ctx *ctx, void *resbuf);


/* Compute SHA256 (SHA224) message digest for bytes read from STREAM.  The
   resulting message digest number will be written into the 32 (28) bytes
   beginning at RESBLOCK.  */
extern int sha256_stream (FILE *stream, void *resblock);
extern int sha224_stream (FILE *stream, void *resblock);

/* Compute SHA256 (SHA224) message digest for LEN bytes beginning at BUFFER.  The
   result is always in little endian byte order, so that a byte-wise
   output yields to the wanted ASCII representation of the message
   digest.  */
extern void *sha256_buffer (const char *buffer, size_t len, void *resblock);
extern void *sha224_buffer (const char *buffer, size_t len, void *resblock);

#ifdef __cplusplus
}
#endif 

#endif
