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

#ifndef SPAS_CLIENT_H
#define SPAS_CLIENT_H

#include "param_list.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
namespace rocketmqSignature {
#endif

#define SPAS_MAX_KEY_LEN 128            /* max access_key/secret_key length */
#define SPAS_MAX_PATH 256               /* max credential file path length */
#define SPAS_ACCESS_KEY_TAG "accessKey" /* access_key tag in credential file \
                                           */
#define SPAS_SECRET_KEY_TAG "secretKey" /* secret_key tag in credential file \
                                           */
#define SPAS_CREDENTIAL_ENV \
  "SPAS_CREDENTIAL" /* credential file environment variable */

typedef enum {
  SIGN_HMACSHA1 = 0,   /* HmacSHA1 */
  SIGN_HMACSHA256 = 1, /* HmacSHA256 */
} SPAS_SIGN_ALGORITHM;

typedef enum {
  NO_UPDATE = 0,       /* do not update credential */
  UPDATE_BY_ALARM = 1, /* update credential by SIGALRM */
#ifdef SPAS_MT
  UPDATE_BY_THREAD = 2, /* update credential by standalone thread */
#endif
} CREDENTIAL_UPDATE_MODE;

typedef enum {
  SPAS_NO_ERROR = 0,           /* success */
  ERROR_INVALID_PARAM = -1,    /* invalid parameter */
  ERROR_NO_CREDENTIAL = -2,    /* credential file not specified */
  ERROR_FILE_OPEN = -3,        /* file open failed */
  ERROR_MEM_ALLOC = -4,        /* memory allocation failed */
  ERROR_MISSING_KEY = -5,      /* missing access_key/secret_key */
  ERROR_KEY_LENGTH = -6,       /* key length exceed limit */
  ERROR_UPDATE_CREDENTIAL = -7 /* update credential file failed */

} SPAS_ERROR_CODE;

typedef struct _spas_credential {
  char access_key[SPAS_MAX_KEY_LEN];
  char secret_key[SPAS_MAX_KEY_LEN];
} SPAS_CREDENTIAL;

extern int spas_load_credential(char *path, CREDENTIAL_UPDATE_MODE mode);
extern int spas_set_access_key(char *key);
extern int spas_set_secret_key(char *key);
extern char *spas_get_access_key(void);
extern char *spas_get_secret_key(void);
extern SPAS_CREDENTIAL *spas_get_credential(void);

#ifdef SPAS_MT

extern int spas_load_thread_credential(char *path);
extern int spas_set_thread_access_key(char *key);
extern int spas_set_thread_secret_key(char *key);
extern char *spas_get_thread_access_key(void);
extern char *spas_get_thread_secret_key(void);

#endif

extern char *spas_get_signature(const SPAS_PARAM_LIST *list, const char *key);
extern char *spas_get_signature2(const SPAS_PARAM_LIST *list, const char *key,
                                 SPAS_SIGN_ALGORITHM algorithm);
extern char *spas_sign(const char *data, size_t size, const char *key);
extern char *spas_sign2(const char *data, size_t size, const char *key,
                        SPAS_SIGN_ALGORITHM algorithm);
extern void spas_mem_free(char *pSignature);
extern char *spas_get_version(void);

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
}
#endif

#endif
