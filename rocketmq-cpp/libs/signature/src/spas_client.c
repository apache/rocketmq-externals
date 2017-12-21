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

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "base64.h"
#include "hmac.h"
#include "sha1.h"
#include "sha256.h"
#include "spas_client.h"

#ifdef WIN32
#include <io.h>
#include <process.h>
#else
#include <unistd.h>
#endif
#ifdef SPAS_MT
#include <pthread.h>
#endif

#ifdef __cplusplus
namespace rocketmqSignature {
#endif

#define SPAS_VERSION "SPAS_V1_0"

static SPAS_CREDENTIAL g_credential;
static char g_path[SPAS_MAX_PATH];
static int g_loaded = 0;
static unsigned int refresh = 10;
static time_t modified = 0;

#ifdef SPAS_MT

static pthread_mutex_t cred_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t cred_once = PTHREAD_ONCE_INIT;
static pthread_key_t cred_key;

#endif

extern void *_mem_alloc(unsigned int size);
extern void *_mem_realloc(void *ptr, unsigned int old_size,
                          unsigned int new_size);
extern void _mem_free(void *ptr);
extern void _trim(char *str);

void *_mem_alloc(unsigned int size) {
  void *p = malloc(size);
  if (p != NULL) {
    memset(p, 0, size);
  }
  return p;
}

void *_mem_realloc(void *ptr, unsigned int old_size, unsigned int new_size) {
  void *p = realloc(ptr, new_size);
  if (p != NULL && new_size > old_size) {
    memset((unsigned int *)p + old_size, 0, new_size - old_size);
  }
  return p;
}

void _mem_free(void *ptr) { free(ptr); }

void _trim(char *str) {
  int len = strlen(str);
  int i;
  int done = 0;
  for (i = len - 1; i >= 0; i--) {
    switch (str[i]) {
      case ' ':
      case '\t':
      case '\r':
      case '\n':
        str[i] = '\0';
        break;
      default:
        done = 1;
        break;
    }
    if (done) {
      break;
    }
  }
}

static int _load_credential(SPAS_CREDENTIAL *pcred, char *path) {
  FILE *fp = NULL;
  char buf[SPAS_MAX_KEY_LEN * 2];
  if (pcred == NULL || path == NULL) {
    return ERROR_INVALID_PARAM;
  }
  fp = fopen(path, "r");
  if (fp == NULL) {
    return ERROR_FILE_OPEN;
  }
  memset(pcred, 0, sizeof(SPAS_CREDENTIAL));
  while (fgets(buf, sizeof(buf), fp)) {
    _trim(buf);
    int len = strlen(SPAS_ACCESS_KEY_TAG);
    if (strncmp(buf, SPAS_ACCESS_KEY_TAG, len) == 0 && buf[len] == '=') {
      strncpy(pcred->access_key, buf + len + 1, SPAS_MAX_KEY_LEN - 1);
    } else {
      len = strlen(SPAS_SECRET_KEY_TAG);
      if (strncmp(buf, SPAS_SECRET_KEY_TAG, len) == 0 && buf[len] == '=') {
        strncpy(pcred->secret_key, buf + len + 1, SPAS_MAX_KEY_LEN - 1);
      }
    }
  }
  fclose(fp);
  if (strlen(pcred->access_key) == 0 || strlen(pcred->secret_key) == 0) {
    return ERROR_MISSING_KEY;
  }
  return SPAS_NO_ERROR;
}

#ifndef WIN32
static void _reload_credential(int sig) {
  int ret;
  SPAS_CREDENTIAL credential;
  struct stat status;
  struct sigaction act;

  if (sig != SIGALRM) {
    return;
  }

  memset(&act, 0, sizeof(act));
  act.sa_handler = _reload_credential;
  sigaction(SIGALRM, &act, NULL);
  alarm(refresh);
  if (g_path[0] != '\0') {
    ret = stat(g_path, &status);
    if (ret != 0) {
      return;
    }
    if (status.st_mtime == modified) {
      return;
    }
    ret = _load_credential(&credential, g_path);
    if (ret != SPAS_NO_ERROR) {
      return;
    }
#ifdef SPAS_MT
    pthread_mutex_lock(&cred_mutex);
#endif
    memcpy(&g_credential, &credential, sizeof(SPAS_CREDENTIAL));
#ifdef SPAS_MT
    pthread_mutex_unlock(&cred_mutex);
#endif
    modified = status.st_mtime;
  }
}

static int _update_credential_by_alarm() {
  struct sigaction act;

  memset(&act, 0, sizeof(act));
  act.sa_handler = _reload_credential;
  sigaction(SIGALRM, &act, NULL);
  alarm(refresh);
  return SPAS_NO_ERROR;
}
#endif

#ifdef SPAS_MT

static void *_update_credential_entry(void *arg) {
  int ret;
  SPAS_CREDENTIAL credential;
  struct stat status;
  struct timeval tv;
  while (1) {
    tv.tv_sec = refresh;
    tv.tv_usec = 0;
    select(0, NULL, NULL, NULL, &tv);
    if (g_path[0] != '\0') {
      ret = stat(g_path, &status);
      if (ret != 0) {
        continue;
      }
      if (status.st_mtime == modified) {
        continue;
      }
      ret = _load_credential(&credential, g_path);
      if (ret != SPAS_NO_ERROR) {
        continue;
      }
      pthread_mutex_lock(&cred_mutex);
      memcpy(&g_credential, &credential, sizeof(SPAS_CREDENTIAL));
      pthread_mutex_unlock(&cred_mutex);
      modified = status.st_mtime;
    }
  }
  return NULL;
}

static int _update_credential_by_thread() {
  pthread_t tid;
  int ret;

  ret = pthread_create(&tid, NULL, _update_credential_entry, NULL);
  if (ret != 0) {
    return ERROR_UPDATE_CREDENTIAL;
  }
  pthread_detach(tid);
  return SPAS_NO_ERROR;
}

int spas_load_credential(char *path, CREDENTIAL_UPDATE_MODE mode) {
  int ret = SPAS_NO_ERROR;
  SPAS_CREDENTIAL credential;

  if (g_loaded) {
    return SPAS_NO_ERROR;
  }
  if (path == NULL) {
    path = getenv(SPAS_CREDENTIAL_ENV);
    if (path == NULL) {
      return ERROR_NO_CREDENTIAL;
    }
  }
  strncpy(g_path, path, SPAS_MAX_PATH - 1);
  ret = _load_credential(&credential, path);
  if (ret != SPAS_NO_ERROR) {
    return ret;
  }
#ifdef SPAS_MT
  pthread_mutex_lock(&cred_mutex);
#endif
  if (!g_loaded) {
    memcpy(&g_credential, &credential, sizeof(SPAS_CREDENTIAL));
    g_loaded = 1;
  }
#ifdef SPAS_MT
  pthread_mutex_unlock(&cred_mutex);
#endif
  switch (mode) {
    case UPDATE_BY_ALARM:
      ret = _update_credential_by_alarm();
      break;
#ifdef SPAS_MT
    case UPDATE_BY_THREAD:
      ret = _update_credential_by_thread();
      break;
#endif
    case NO_UPDATE:
    default:
      ret = SPAS_NO_ERROR;
      break;
  }
  return ret;
}

#endif

SPAS_CREDENTIAL *spas_get_credential(void) {
  SPAS_CREDENTIAL *credential =
      (SPAS_CREDENTIAL *)_mem_alloc(sizeof(SPAS_CREDENTIAL));
  if (credential != NULL) {
#ifdef SPAS_MT
    pthread_mutex_lock(&cred_mutex);
#endif
    memcpy(credential, &g_credential, sizeof(SPAS_CREDENTIAL));
#ifdef SPAS_MT
    pthread_mutex_unlock(&cred_mutex);
#endif
  }
  return credential;
}

int spas_set_access_key(char *key) {
  int len = 0;
  if (key == NULL) {
    return ERROR_INVALID_PARAM;
  }
  len = strlen(key);
  if (len == 0 || len >= SPAS_MAX_KEY_LEN) {
    return ERROR_KEY_LENGTH;
  }
#ifdef SPAS_MT
  pthread_mutex_lock(&cred_mutex);
#endif
  memcpy(g_credential.access_key, key, len + 1);
#ifdef SPAS_MT
  pthread_mutex_unlock(&cred_mutex);
#endif
  return SPAS_NO_ERROR;
}

int spas_set_secret_key(char *key) {
  int len = 0;
  if (key == NULL) {
    return ERROR_INVALID_PARAM;
  }
  len = strlen(key);
  if (len == 0 || len >= SPAS_MAX_KEY_LEN) {
    return ERROR_KEY_LENGTH;
  }
#ifdef SPAS_MT
  pthread_mutex_lock(&cred_mutex);
#endif
  memcpy(g_credential.secret_key, key, len + 1);
#ifdef SPAS_MT
  pthread_mutex_unlock(&cred_mutex);
#endif
  return SPAS_NO_ERROR;
}

char *spas_get_access_key() { return g_credential.access_key; }

char *spas_get_secret_key() { return g_credential.secret_key; }

#ifdef SPAS_MT

static void _free_thread_credential(void *credential) {
  if (credential != NULL) {
    _mem_free(credential);
  }
}

static void _init_credential_key(void) {
  pthread_key_create(&cred_key, _free_thread_credential);
}

static SPAS_CREDENTIAL *_get_thread_credential(void) {
  int ret = 0;
  SPAS_CREDENTIAL *credential = NULL;
  ret = pthread_once(&cred_once, _init_credential_key);
  if (ret != 0) {
    return NULL;
  }
  credential = pthread_getspecific(cred_key);
  if (credential == NULL) {
    credential = _mem_alloc(sizeof(SPAS_CREDENTIAL));
    if (credential == NULL) {
      return NULL;
    }
    ret = pthread_setspecific(cred_key, credential);
    if (ret != 0) {
      _mem_free(credential);
      return NULL;
    }
  }
  return credential;
}

int spas_load_thread_credential(char *path) {
  int ret = SPAS_NO_ERROR;
  SPAS_CREDENTIAL *credential = NULL;
  credential = _get_thread_credential();
  if (credential == NULL) {
    return ERROR_MEM_ALLOC;
  }
  ret = _load_credential(credential, path);
  if (ret != SPAS_NO_ERROR) {
    memset(credential, 0, sizeof(SPAS_CREDENTIAL));
    return ret;
  }
  return SPAS_NO_ERROR;
}

int spas_set_thread_access_key(char *key) {
  int len = 0;
  SPAS_CREDENTIAL *credential = NULL;
  if (key == NULL) {
    return ERROR_INVALID_PARAM;
  }
  len = strlen(key);
  if (len == 0 || len >= SPAS_MAX_KEY_LEN) {
    return ERROR_KEY_LENGTH;
  }
  credential = _get_thread_credential();
  if (credential == NULL) {
    return ERROR_MEM_ALLOC;
  }
  memcpy(credential->access_key, key, len + 1);
  return SPAS_NO_ERROR;
}

int spas_set_thread_secret_key(char *key) {
  int len = 0;
  SPAS_CREDENTIAL *credential = NULL;
  if (key == NULL) {
    return ERROR_INVALID_PARAM;
  }
  len = strlen(key);
  if (len == 0 || len >= SPAS_MAX_KEY_LEN) {
    return ERROR_KEY_LENGTH;
  }
  credential = _get_thread_credential();
  if (credential == NULL) {
    return ERROR_MEM_ALLOC;
  }
  memcpy(credential->secret_key, key, len + 1);
  return SPAS_NO_ERROR;
}

char *spas_get_thread_access_key(void) {
  SPAS_CREDENTIAL *credential = _get_thread_credential();
  if (credential == NULL) {
    return NULL;
  }
  return credential->access_key;
}

char *spas_get_thread_secret_key(void) {
  SPAS_CREDENTIAL *credential = _get_thread_credential();
  if (credential == NULL) {
    return NULL;
  }
  return credential->secret_key;
}

#endif

char *spas_get_signature(const SPAS_PARAM_LIST *list, const char *key) {
  return spas_get_signature2(list, key, SIGN_HMACSHA1);
}

char *spas_get_signature2(const SPAS_PARAM_LIST *list, const char *key,
                          SPAS_SIGN_ALGORITHM algorithm) {
  char *sign = NULL;
  char *data = NULL;
  if (list == NULL || key == NULL) {
    return NULL;
  }
  data = param_list_to_str(list);
  if (data == NULL) {
    return NULL;
  }
  sign = spas_sign2(data, strlen(data), key, algorithm);
  _mem_free(data);
  return sign;
}

char *spas_sign(const char *data, size_t size, const char *key) {
  return spas_sign2(data, size, key, SIGN_HMACSHA1);
}

char *spas_sign2(const char *data, size_t size, const char *key,
                 SPAS_SIGN_ALGORITHM algorithm) {
  int ret;
  int dsize = 0;
  char *sha_buf = NULL;
  char *base64_ret = NULL;
  if (data == NULL || key == NULL) {
    return NULL;
  }
  if (algorithm == SIGN_HMACSHA1) {
    dsize = SHA1_DIGEST_SIZE;
    sha_buf = (char *)_mem_alloc(dsize + 1);
    if (sha_buf == NULL) {
      return NULL;
    }
    ret = hmac_sha1(key, strlen(key), data, size, sha_buf);
    if (ret < 0) {
      _mem_free(sha_buf);
      return NULL;
    }
  } else if (algorithm == SIGN_HMACSHA256) {
    dsize = SHA256_DIGEST_SIZE;
    sha_buf = (char *)_mem_alloc(dsize + 1);
    if (sha_buf == NULL) {
      return NULL;
    }
    ret = hmac_sha256(key, strlen(key), data, strlen(data), sha_buf);
    if (ret < 0) {
      _mem_free(sha_buf);
      return NULL;
    }
  } else {
    return NULL;
  }
  ret = base64_encode_alloc(sha_buf, dsize, &base64_ret);
  _mem_free(sha_buf);
  return base64_ret;
}

void spas_mem_free(char *pSignature) { _mem_free(pSignature); }

char *spas_get_version(void) { return SPAS_VERSION; }

#ifdef __cplusplus
}
#endif
