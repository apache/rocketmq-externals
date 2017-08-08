#ifndef PARAM_LIST_H
#define PARAM_LIST_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
namespace metaqSignature{
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

