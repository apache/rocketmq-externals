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

#include <stdio.h>
#include <string.h>
#include "spas_client.h"

#ifdef __cplusplus
namespace rocketmqSignature {
#endif

extern void *_mem_alloc(unsigned int size);
extern void _mem_free(void *ptr);

static int _nodecmp(SPAS_PARAM_NODE *n1, SPAS_PARAM_NODE *n2) {
  int ret = strcmp(n1->name, n2->name);
  if (ret == 0) {
    ret = strcmp(n1->value, n2->value);
  }
  return ret;
}

SPAS_PARAM_LIST *create_param_list() {
  return (SPAS_PARAM_LIST *)_mem_alloc(sizeof(SPAS_PARAM_LIST));
}

void free_param_list(SPAS_PARAM_LIST *list) {
  SPAS_PARAM_NODE *pnode = NULL;
  SPAS_PARAM_NODE *pnext = NULL;
  if (list == NULL) {
    return;
  }
  pnode = list->phead;
  while (pnode != NULL) {
    pnext = pnode->pnext;
    _mem_free(pnode->name);
    _mem_free(pnode->value);
    _mem_free(pnode);
    pnode = pnext;
  }
  _mem_free(list);
}

int add_param_to_list(SPAS_PARAM_LIST *list, const char *name,
                      const char *value) {
  SPAS_PARAM_NODE *pnode = NULL;
  SPAS_PARAM_NODE *plast = NULL;
  int nlen = 0;
  int vlen = 0;
  if (list == NULL || name == NULL || value == NULL) {
    return ERROR_INVALID_PARAM;
  }
  nlen = strlen(name);
  vlen = strlen(value);
  pnode = (SPAS_PARAM_NODE *)_mem_alloc(sizeof(SPAS_PARAM_NODE));
  if (pnode == NULL) {
    return ERROR_MEM_ALLOC;
  }
  pnode->name = (char *)_mem_alloc(nlen + 1);
  if (pnode->name == NULL) {
    _mem_free(pnode);
    return ERROR_MEM_ALLOC;
  }
  pnode->value = (char *)_mem_alloc(vlen + 1);
  if (pnode->value == NULL) {
    _mem_free(pnode->name);
    _mem_free(pnode);
    return ERROR_MEM_ALLOC;
  }
  memcpy(pnode->name, name, nlen);
  memcpy(pnode->value, value, vlen);
  if (list->phead == NULL) {
    list->phead = pnode;
  } else if (_nodecmp(pnode, list->phead) <= 0) {
    pnode->pnext = list->phead;
    list->phead = pnode;
  } else {
    plast = list->phead;
    while (plast->pnext != NULL) {
      if (_nodecmp(pnode, plast->pnext) <= 0) {
        pnode->pnext = plast->pnext;
        plast->pnext = pnode;
        break;
      } else {
        plast = plast->pnext;
      }
    }
    if (plast->pnext == NULL) {
      plast->pnext = pnode;
    }
  }
  list->length++;
  list->size += nlen + vlen + 1; /* 1 overhead for '=' */
  return SPAS_NO_ERROR;
}

char *param_list_to_str(const SPAS_PARAM_LIST *list) {
  int size = 0;
  int pos = 0;
  char *buf = NULL;
  SPAS_PARAM_NODE *pnode = NULL;
  if (list == NULL) {
    return NULL;
  }
  if (list->length == 0) {
    return (char *)_mem_alloc(1);
  }
  size = list->size + list->length - 1; /* overhead for '&' */
  buf = (char *)_mem_alloc(size);
  if (buf == NULL) {
    return NULL;
  }
  pnode = list->phead;
  if (pnode != NULL) {
    sprintf(buf, "%s=%s", pnode->name, pnode->value);
    pos += strlen(pnode->name) + strlen(pnode->value) + 1;
    pnode = pnode->pnext;
  }
  while (pnode != NULL) {
    sprintf(buf + pos, "&%s=%s", pnode->name, pnode->value);
    pos += strlen(pnode->name) + strlen(pnode->value) + 2;
    pnode = pnode->pnext;
  }
  return buf;
}
#ifdef __cplusplus
}
#endif