#ifndef STDBOOL_H_
#define STDBOOL_H_

/**
 * stdbool.h
 * Author    - Yaping Xin
 * E-mail    - xinyp at live dot com
 * Date      - February 10, 2014
 * Copyright - You are free to use for any purpose except illegal acts
 * Warrenty  - None: don't blame me if it breaks something
 *
 * In ISO C99, stdbool.h is a standard header and _Bool is a keyword, but
 * some compilers don't offer these yet. This header file is an 
 * implementation of the stdbool.h header file.
 *
 */

#ifndef _Bool
typedef unsigned char _Bool;
#endif /* _Bool */

/**
 * Define the Boolean macros only if they are not already defined.
 */
#ifndef __bool_true_false_are_defined
#define bool _Bool
#define false 0 
#define true 1
#define __bool_true_false_are_defined 1
#endif /* __bool_true_false_are_defined */

#endif /* STDBOOL_H_ */