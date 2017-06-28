#ifndef _CAST_LAUNCH_H
#define _CAST_LAUNCH_H

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif
	
extern JNIEnv *launch(char *);
extern void kill();

#ifdef __cplusplus
}
#endif

#endif
