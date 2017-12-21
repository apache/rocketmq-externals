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

#ifndef PARAM_LIST_H
#define PARAM_LIST_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
namespace rocketmqSignature{
#endif


typedef struct _spas_param_node {
	char *name;
	char *value;
	struct _spas_param_node *pnext;
} SPAS_PARAM_NODE;

typedef struct _spas_param_list {
	SPAS_PARAM_NODE *phead;
	unsigned int length; /* count of nodes */
	unsigned int size; /* total size of string presentation */
} SPAS_PARAM_LIST;

extern SPAS_PARAM_LIST * create_param_list(void);
extern int add_param_to_list(SPAS_PARAM_LIST *list, const char *name, const char *value);
extern void free_param_list(SPAS_PARAM_LIST *list);
extern char * param_list_to_str(const SPAS_PARAM_LIST *list);

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
}
#endif

#endif

