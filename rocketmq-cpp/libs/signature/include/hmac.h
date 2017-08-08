
#ifndef _HMAC_HMAC_H
#define _HMAC_HMAC_H

#ifdef __cplusplus
extern "C" {
#endif

#include <sys/types.h>

#ifndef SHA1_DIGEST_LEN
#define SHA1_DIGEST_LEN		20
#endif

#ifndef SHA256_DIGEST_LEN
#define SHA256_DIGEST_LEN	32
#endif

#ifndef SHA512_DIGEST_LEN
#define SHA512_DIGEST_LEN	64
#endif

/*
 * hmac_sha1:
 * hmac_sha256:
 * hmac_sha512:
 *	Calculate Hashed Message Authentication Code with sha1/256/512 algorithm
 *	Caution: ret_buf should provide enough space for HMAC result.
 *
 *	@key [in]: the secure-key string
 *	@key_len [in]: the length of secure-key
 *	@data [in]: data string could be calculated.
 *	@data_len [in]: the length of data. length is needed because strlen could not take effect.
 *	@ret_buf [out]: HMAC result stored in ret_buf.
 */

#ifdef __cplusplus
namespace metaqSignature{

#endif

extern int hmac_sha1(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf);
extern int hmac_sha256(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf);
extern int hmac_sha512(const void *key, size_t key_len, const void *data, size_t data_len, void *ret_buf);

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
}
#endif

#endif

