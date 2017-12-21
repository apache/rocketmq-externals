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

#include <stddef.h>
#include <stdint.h>

/* Return X rotated left by N bits, where 0 < N < 64.  */
#define u64rol(x, n) u64or (u64shl (x, n), u64shr (x, 64 - n))

#ifdef UINT64_MAX

/* Native implementations are trivial.  See below for comments on what
   these operations do.  */
typedef uint64_t u64;
# define u64hilo(hi, lo) ((u64) (((u64) (hi) << 32) + (lo)))
# define u64init(hi, lo) u64hilo (hi, lo)
# define u64lo(x) ((u64) (x))
# define u64lt(x, y) ((x) < (y))
# define u64and(x, y) ((x) & (y))
# define u64or(x, y) ((x) | (y))
# define u64xor(x, y) ((x) ^ (y))
# define u64plus(x, y) ((x) + (y))
# define u64shl(x, n) ((x) << (n))
# define u64shr(x, n) ((x) >> (n))

#else

/* u64 is a 64-bit unsigned integer value.
   u64init (HI, LO), is like u64hilo (HI, LO), but for use in
   initializer contexts.  */
# ifdef WORDS_BIGENDIAN
typedef struct { uint32_t hi, lo; } u64;
#  define u64init(hi, lo) { hi, lo }
# else
typedef struct { uint32_t lo, hi; } u64;
#  define u64init(hi, lo) { lo, hi }
# endif

/* Given the high and low-order 32-bit quantities HI and LO, return a u64
   value representing (HI << 32) + LO.  */
static inline u64
u64hilo (uint32_t hi, uint32_t lo)
{
  u64 r;
  r.hi = hi;
  r.lo = lo;
  return r;
}

/* Return a u64 value representing LO.  */
static inline u64
u64lo (uint32_t lo)
{
  u64 r;
  r.hi = 0;
  r.lo = lo;
  return r;
}

/* Return X < Y.  */
static inline int
u64lt (u64 x, u64 y)
{
  return x.hi < y.hi || (x.hi == y.hi && x.lo < y.lo);
}

/* Return X & Y.  */
static inline u64
u64and (u64 x, u64 y)
{
  u64 r;
  r.hi = x.hi & y.hi;
  r.lo = x.lo & y.lo;
  return r;
}

/* Return X | Y.  */
static inline u64
u64or (u64 x, u64 y)
{
  u64 r;
  r.hi = x.hi | y.hi;
  r.lo = x.lo | y.lo;
  return r;
}

/* Return X ^ Y.  */
static inline u64
u64xor (u64 x, u64 y)
{
  u64 r;
  r.hi = x.hi ^ y.hi;
  r.lo = x.lo ^ y.lo;
  return r;
}

/* Return X + Y.  */
static inline u64
u64plus (u64 x, u64 y)
{
  u64 r;
  r.lo = x.lo + y.lo;
  r.hi = x.hi + y.hi + (r.lo < x.lo);
  return r;
}

/* Return X << N.  */
static inline u64
u64shl (u64 x, int n)
{
  u64 r;
  if (n < 32)
    {
      r.hi = (x.hi << n) | (x.lo >> (32 - n));
      r.lo = x.lo << n;
    }
  else
    {
      r.hi = x.lo << (n - 32);
      r.lo = 0;
    }
  return r;
}

/* Return X >> N.  */
static inline u64
u64shr (u64 x, int n)
{
  u64 r;
  if (n < 32)
    {
      r.hi = x.hi >> n;
      r.lo = (x.hi << (32 - n)) | (x.lo >> n);
    }
  else
    {
      r.hi = 0;
      r.lo = x.hi >> (n - 32);
    }
  return r;
}

#endif
