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

#include "sha1.h"

#include <stddef.h>
#include <string.h>

#if USE_UNLOCKED_IO
# include "unlocked-io.h"
#endif

#ifdef __cplusplus
namespace rocketmqSignature {
#endif

#ifdef WORDS_BIGENDIAN
# define SWAP(n) (n)
#else
# define SWAP(n) \
    (((n) << 24) | (((n) & 0xff00) << 8) | (((n) >> 8) & 0xff00) | ((n) >> 24))
#endif

#define BLOCKSIZE 4096
#if BLOCKSIZE % 64 != 0
# error "invalid BLOCKSIZE"
#endif

/* This array contains the bytes used to pad the buffer to the next
   64-byte boundary.  (RFC 1321, 3.1: Step 1)  */
static const unsigned char fillbuf[64] = { 0x80, 0 /* , 0, 0, ...  */ };

/*!
 * @fn void sha1_init_ctx (struct sha1_ctx *ctx)
 *
 * @brief initialize a context with start constants
 *
 * @details Take a pointer to a 160 bit block of data (five 32 bit ints) and
 *          initialize it to the start constants of the SHA1 algorithm.  This
 *          must be called before using hash in the call to sha1_hash.
 *
 * @param[out] ctx  pointer to a context to be initialized
 */
void
sha1_init_ctx (struct sha1_ctx *ctx)
{
  ctx->A = 0x67452301;
  ctx->B = 0xefcdab89;
  ctx->C = 0x98badcfe;
  ctx->D = 0x10325476;
  ctx->E = 0xc3d2e1f0;

  ctx->total[0] = ctx->total[1] = 0;
  ctx->buflen = 0;
}

/*!
 * @fn static __inline__ void set_uint32 (char *cp, uint32_t v)
 *
 * @brief Copy the 4 byte value from v into the memory location pointed to
          by *cp
 *
 * @details Copy the 4 byte value from v into the memory location pointed to by
 *          *cp, If your architecture allows unaligned access this is equivalent
 *          to * (uint32_t *) cp = v
 *
 * @param[out]  cp  memory location to copy v into
 * @param[in]   v   4 byte value to be copied
 */
#ifdef WIN32
static _inline void
#else
static __inline__ void
#endif
set_uint32 (char *cp, uint32_t v)
{
  memcpy (cp, &v, sizeof v);
}

/*!
 * @fn void *sha1_read_ctx (const struct sha1_ctx *ctx, void *resbuf)
 *
 * @brief Put result from CTX in first 20 bytes following RESBUF
 *
 * @details Put result from CTX in first 20 bytes following RESBUF.  The result
 *          must be in little endian byte order.
 *
 * @param[in]   ctx     context whose results will be copied
 * @param[out]  resbuf  result of copies saved in little endian byte order
 * @return resbuf
 */
void *
sha1_read_ctx (const struct sha1_ctx *ctx, void *resbuf)
{
  char *r = (char*)resbuf;
  set_uint32 (r + 0 * sizeof ctx->A, SWAP (ctx->A));
  set_uint32 (r + 1 * sizeof ctx->B, SWAP (ctx->B));
  set_uint32 (r + 2 * sizeof ctx->C, SWAP (ctx->C));
  set_uint32 (r + 3 * sizeof ctx->D, SWAP (ctx->D));
  set_uint32 (r + 4 * sizeof ctx->E, SWAP (ctx->E));

  return resbuf;
}

/*!
 * @fn void *sha1_finish_ctx (struct sha1_ctx *ctx, void *resbuf)
 *
 * @brief Process the remaining bytes in the internal buffer and write
          the result to RESBUF.
 *
 * @details Process the remaining bytes in the internal buffer and the usual
 *          prolog according to the standard and write the result to RESBUF.
 *
 * @param[in]  ctx     context to be used
 * @param[out] resbuf  resultant SHA1 hash
 * @return resultant SHA1 hash
 */
void *
sha1_finish_ctx (struct sha1_ctx *ctx, void *resbuf)
{
  /* Take yet unprocessed bytes into account.  */
  uint32_t bytes = ctx->buflen;
  size_t size = (bytes < 56) ? 64 / 4 : 64 * 2 / 4;

  /* Now count remaining bytes.  */
  ctx->total[0] += bytes;
  if (ctx->total[0] < bytes)
    ++ctx->total[1];

  /* Put the 64-bit file length in *bits* at the end of the buffer.  */
  ctx->buffer[size - 2] = SWAP ((ctx->total[1] << 3) | (ctx->total[0] >> 29));
  ctx->buffer[size - 1] = SWAP (ctx->total[0] << 3);

  memcpy (&((char *) ctx->buffer)[bytes], fillbuf, (size - 2) * 4 - bytes);

  /* Process last bytes.  */
  sha1_process_block (ctx->buffer, size * 4, ctx);

  return sha1_read_ctx (ctx, resbuf);
}


/*
 * @fn void *sha1_stream (FILE *stream, void *resblock)
 *
 * @brief Compute SHA1 message digest for A Stream.
 *
 * @details Compute SHA1 message digest for Stream.  The
 *          result is always in little endian byte order, so that a byte-wise
 *          output yields to the wanted ASCII representation of the message
 *          digest.
 *
 * @param[in]  stream    message stream to be hashed
 * @param[out] resblock  resultant hash in little endian byte order
 * @return resultant hash in little endian byte order
 */
int
sha1_stream (FILE *stream, void *resblock)
{
  struct sha1_ctx ctx;
  char buffer[BLOCKSIZE + 72];
  size_t sum;

  /* Initialize the computation context.  */
  sha1_init_ctx (&ctx);

  /* Iterate over full file contents.  */
  while (1)
    {
      /* We read the file in blocks of BLOCKSIZE bytes.  One call of the
	 computation function processes the whole buffer so that with the
	 next round of the loop another block can be read.  */
      size_t n;
      sum = 0;

      /* Read block.  Take care for partial reads.  */
      while (1)
	{
	  n = fread (buffer + sum, 1, BLOCKSIZE - sum, stream);

	  sum += n;

	  if (sum == BLOCKSIZE)
	    break;

	  if (n == 0)
	    {
	      /* Check for the error flag IFF N == 0, so that we don't
		 exit the loop after a partial read due to e.g., EAGAIN
		 or EWOULDBLOCK.  */
	      if (ferror (stream))
		return 1;
	      goto process_partial_block;
	    }

	  /* We've read at least one byte, so ignore errors.  But always
	     check for EOF, since feof may be true even though N > 0.
	     Otherwise, we could end up calling fread after EOF.  */
	  if (feof (stream))
	    goto process_partial_block;
	}

      /* Process buffer with BLOCKSIZE bytes.  Note that
			BLOCKSIZE % 64 == 0
       */
      sha1_process_block (buffer, BLOCKSIZE, &ctx);
    }

 process_partial_block:;

  /* Process any remaining bytes.  */
  if (sum > 0)
    sha1_process_bytes (buffer, sum, &ctx);

  /* Construct result in desired memory.  */
  sha1_finish_ctx (&ctx, resblock);
  return 0;
}


/*
 * @fn void *sha1_buffer (const char *buffer, size_t len, void *resblock)
 *
 * @brief Compute SHA1 message digest for LEN bytes beginning at BUFFER.
 *
 * @details Compute SHA1 message digest for LEN bytes beginning at BUFFER.  The
 *          result is always in little endian byte order, so that a byte-wise
 *          output yields to the wanted ASCII representation of the message
 *          digest.
 *
 * @param[in]  buffer    message to be hashed
 * @param[in]  len       length of buffer
 * @param[out] resblock  resultant hash in little endian byte order
 * @return resultant hash in little endian byte order
 */
void *
sha1_buffer (const char *buffer, size_t len, void *resblock)
{
  struct sha1_ctx ctx;

  /* Initialize the computation context.  */
  sha1_init_ctx (&ctx);

  /* Process whole buffer but last len % 64 bytes.  */
  sha1_process_bytes (buffer, len, &ctx);

  /* Put result in desired memory area.  */
  return sha1_finish_ctx (&ctx, resblock);
}

/*!
 * @fn void sha1_process_bytes (const void *buffer, size_t len, struct sha1_ctx *ctx)
 *
 * @brief update the context for the next LEN bytes starting at BUFFER.
 *
 * @details Starting with the result of former calls of this function (or the
 *          initialization function) update the context for the next LEN bytes
 *          starting at BUFFER.
 *          It is NOT required that LEN is a multiple of 64.
 *
 * @param[in]  buffer  buffer used to update context values
 * @param[in]  len     length of buffer
 * @param[out] ctx     context to be updated
 */
void
sha1_process_bytes (const void *buffer, size_t len, struct sha1_ctx *ctx)
{
  /* When we already have some bits in our internal buffer concatenate
     both inputs first.  */
  if (ctx->buflen != 0)
    {
      size_t left_over = ctx->buflen;
      size_t add = 128 - left_over > len ? len : 128 - left_over;

      memcpy (&((char *) ctx->buffer)[left_over], buffer, add);
      ctx->buflen += add;

      if (ctx->buflen > 64)
	{
	  sha1_process_block (ctx->buffer, ctx->buflen & ~63, ctx);

	  ctx->buflen &= 63;
	  /* The regions in the following copy operation cannot overlap.  */
	  memcpy (ctx->buffer,
		  &((char *) ctx->buffer)[(left_over + add) & ~63],
		  ctx->buflen);
	}

      buffer = (const char *) buffer + add;
      len -= add;
    }

  /* Process available complete blocks.  */
  if (len >= 64)
    {
#if !_STRING_ARCH_unaligned
# define alignof(type) offsetof (struct { char c; type x; }, x)
# define UNALIGNED_P(p) (((size_t) p) % alignof (uint32_t) != 0)
      if (UNALIGNED_P (buffer))
	while (len > 64)
	  {
	    sha1_process_block (memcpy (ctx->buffer, buffer, 64), 64, ctx);
	    buffer = (const char *) buffer + 64;
	    len -= 64;
	  }
      else
#endif
	{
	  sha1_process_block (buffer, len & ~63, ctx);
	  buffer = (const char *) buffer + (len & ~63);
	  len &= 63;
	}
    }

  /* Move remaining bytes in internal buffer.  */
  if (len > 0)
    {
      size_t left_over = ctx->buflen;

      memcpy (&((char *) ctx->buffer)[left_over], buffer, len);
      left_over += len;
      if (left_over >= 64)
	{
	  sha1_process_block (ctx->buffer, 64, ctx);
	  left_over -= 64;
	  memcpy (ctx->buffer, &ctx->buffer[16], left_over);
	}
      ctx->buflen = left_over;
    }
}

/* --- Code below is the primary difference between md5.c and sha1.c --- */

/* SHA1 round constants */
#define K1 0x5a827999
#define K2 0x6ed9eba1
#define K3 0x8f1bbcdc
#define K4 0xca62c1d6

/* Round functions.  Note that F2 is the same as F4.  */
#define F1(B,C,D) ( D ^ ( B & ( C ^ D ) ) )
#define F2(B,C,D) (B ^ C ^ D)
#define F3(B,C,D) ( ( B & C ) | ( D & ( B | C ) ) )
#define F4(B,C,D) (B ^ C ^ D)

/*!
 * @fn void sha1_process_block (const void *buffer, size_t len, struct sha1_ctx *ctx)
 *
 * @brief Process LEN bytes of BUFFER, accumulating context into CTX.
 *
 * @details Process LEN bytes of BUFFER, accumulating context into CTX.
 *          It is assumed that LEN % 64 == 0.
 *          Most of this code comes from GnuPG's cipher/sha1.c.
 *
 * @param[in]  buffer buffer to be processed
 * @param[in]  len    length of buffer
 * @param[out] ctx    context used to accumulate results
 */

void
sha1_process_block (const void *buffer, size_t len, struct sha1_ctx *ctx)
{
  const uint32_t *words = (const uint32_t*)buffer;
  size_t nwords = len / sizeof (uint32_t);
  const uint32_t *endp = words + nwords;
  uint32_t x[16];
  uint32_t a = ctx->A;
  uint32_t b = ctx->B;
  uint32_t c = ctx->C;
  uint32_t d = ctx->D;
  uint32_t e = ctx->E;

  /* First increment the byte count.  RFC 1321 specifies the possible
     length of the file up to 2^64 bits.  Here we only compute the
     number of bytes.  Do a double word increment.  */
  ctx->total[0] += len;
  if (ctx->total[0] < len)
    ++ctx->total[1];

#define rol(x, n) (((x) << (n)) | ((uint32_t) (x) >> (32 - (n))))

#define M(I) ( tm =   x[I&0x0f] ^ x[(I-14)&0x0f] \
		    ^ x[(I-8)&0x0f] ^ x[(I-3)&0x0f] \
	       , (x[I&0x0f] = rol(tm, 1)) )

#define R(A,B,C,D,E,F,K,M)  do { E += rol( A, 5 )     \
				      + F( B, C, D )  \
				      + K	      \
				      + M;	      \
				 B = rol( B, 30 );    \
			       } while(0)

  while (words < endp)
    {
      uint32_t tm;
      int t;
      for (t = 0; t < 16; t++)
	{
	  x[t] = SWAP (*words);
	  words++;
	}

      R( a, b, c, d, e, F1, K1, x[ 0] );
      R( e, a, b, c, d, F1, K1, x[ 1] );
      R( d, e, a, b, c, F1, K1, x[ 2] );
      R( c, d, e, a, b, F1, K1, x[ 3] );
      R( b, c, d, e, a, F1, K1, x[ 4] );
      R( a, b, c, d, e, F1, K1, x[ 5] );
      R( e, a, b, c, d, F1, K1, x[ 6] );
      R( d, e, a, b, c, F1, K1, x[ 7] );
      R( c, d, e, a, b, F1, K1, x[ 8] );
      R( b, c, d, e, a, F1, K1, x[ 9] );
      R( a, b, c, d, e, F1, K1, x[10] );
      R( e, a, b, c, d, F1, K1, x[11] );
      R( d, e, a, b, c, F1, K1, x[12] );
      R( c, d, e, a, b, F1, K1, x[13] );
      R( b, c, d, e, a, F1, K1, x[14] );
      R( a, b, c, d, e, F1, K1, x[15] );
      R( e, a, b, c, d, F1, K1, M(16) );
      R( d, e, a, b, c, F1, K1, M(17) );
      R( c, d, e, a, b, F1, K1, M(18) );
      R( b, c, d, e, a, F1, K1, M(19) );
      R( a, b, c, d, e, F2, K2, M(20) );
      R( e, a, b, c, d, F2, K2, M(21) );
      R( d, e, a, b, c, F2, K2, M(22) );
      R( c, d, e, a, b, F2, K2, M(23) );
      R( b, c, d, e, a, F2, K2, M(24) );
      R( a, b, c, d, e, F2, K2, M(25) );
      R( e, a, b, c, d, F2, K2, M(26) );
      R( d, e, a, b, c, F2, K2, M(27) );
      R( c, d, e, a, b, F2, K2, M(28) );
      R( b, c, d, e, a, F2, K2, M(29) );
      R( a, b, c, d, e, F2, K2, M(30) );
      R( e, a, b, c, d, F2, K2, M(31) );
      R( d, e, a, b, c, F2, K2, M(32) );
      R( c, d, e, a, b, F2, K2, M(33) );
      R( b, c, d, e, a, F2, K2, M(34) );
      R( a, b, c, d, e, F2, K2, M(35) );
      R( e, a, b, c, d, F2, K2, M(36) );
      R( d, e, a, b, c, F2, K2, M(37) );
      R( c, d, e, a, b, F2, K2, M(38) );
      R( b, c, d, e, a, F2, K2, M(39) );
      R( a, b, c, d, e, F3, K3, M(40) );
      R( e, a, b, c, d, F3, K3, M(41) );
      R( d, e, a, b, c, F3, K3, M(42) );
      R( c, d, e, a, b, F3, K3, M(43) );
      R( b, c, d, e, a, F3, K3, M(44) );
      R( a, b, c, d, e, F3, K3, M(45) );
      R( e, a, b, c, d, F3, K3, M(46) );
      R( d, e, a, b, c, F3, K3, M(47) );
      R( c, d, e, a, b, F3, K3, M(48) );
      R( b, c, d, e, a, F3, K3, M(49) );
      R( a, b, c, d, e, F3, K3, M(50) );
      R( e, a, b, c, d, F3, K3, M(51) );
      R( d, e, a, b, c, F3, K3, M(52) );
      R( c, d, e, a, b, F3, K3, M(53) );
      R( b, c, d, e, a, F3, K3, M(54) );
      R( a, b, c, d, e, F3, K3, M(55) );
      R( e, a, b, c, d, F3, K3, M(56) );
      R( d, e, a, b, c, F3, K3, M(57) );
      R( c, d, e, a, b, F3, K3, M(58) );
      R( b, c, d, e, a, F3, K3, M(59) );
      R( a, b, c, d, e, F4, K4, M(60) );
      R( e, a, b, c, d, F4, K4, M(61) );
      R( d, e, a, b, c, F4, K4, M(62) );
      R( c, d, e, a, b, F4, K4, M(63) );
      R( b, c, d, e, a, F4, K4, M(64) );
      R( a, b, c, d, e, F4, K4, M(65) );
      R( e, a, b, c, d, F4, K4, M(66) );
      R( d, e, a, b, c, F4, K4, M(67) );
      R( c, d, e, a, b, F4, K4, M(68) );
      R( b, c, d, e, a, F4, K4, M(69) );
      R( a, b, c, d, e, F4, K4, M(70) );
      R( e, a, b, c, d, F4, K4, M(71) );
      R( d, e, a, b, c, F4, K4, M(72) );
      R( c, d, e, a, b, F4, K4, M(73) );
      R( b, c, d, e, a, F4, K4, M(74) );
      R( a, b, c, d, e, F4, K4, M(75) );
      R( e, a, b, c, d, F4, K4, M(76) );
      R( d, e, a, b, c, F4, K4, M(77) );
      R( c, d, e, a, b, F4, K4, M(78) );
      R( b, c, d, e, a, F4, K4, M(79) );

      a = ctx->A += a;
      b = ctx->B += b;
      c = ctx->C += c;
      d = ctx->D += d;
      e = ctx->E += e;
    }
}
#ifdef __cplusplus
}
#endif
