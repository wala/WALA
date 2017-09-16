#include <jni.h>
#include <setjmp.h>
#include <string.h>
#include "Exceptions.h"

#ifdef _MSC_VER
#include <malloc.h>
#endif

Exceptions::Exceptions(JNIEnv *java_env, jmp_buf& c_env) : 
  _java_env(java_env), 
  _c_env(c_env) 
{
  _jre = java_env->FindClass("java/lang/RuntimeException");
  _ctr = java_env->GetMethodID(_jre, "<init>", "(Ljava/lang/String;)V");
  _wrapper_ctr = 
    java_env->GetMethodID(_jre, 
			  "<init>", 
			  "(Ljava/lang/String;Ljava/lang/Throwable;)V");
}

void Exceptions::throwAnyException(const char *file_name, int line_number) {
  if (_java_env->ExceptionCheck()) throwException(file_name, line_number);
}

void Exceptions::throwException(const char *file_name, int line_number) {
  jthrowable real_ex = _java_env->ExceptionOccurred();
  _java_env->ExceptionClear();

  char msg[strlen(file_name) + 1024];
  memset(msg, 0, strlen(file_name) + 1024);
  sprintf(msg, "exception at %s:%d", file_name, line_number);
  jstring java_message = _java_env->NewStringUTF(msg);

  jthrowable ex = (jthrowable)
    _java_env->NewObject(_jre, _wrapper_ctr, java_message, real_ex);

  if (_java_env->ExceptionCheck()) {
    jthrowable new_real_ex = _java_env->ExceptionOccurred();
    _java_env->Throw(new_real_ex);
  } else {
    _java_env->Throw(ex);
  }

  longjmp( _c_env, -1 );
}

void 
Exceptions::throwException(const char *file_name, int line_number, const char *c_message) {
  char msg[strlen(file_name) + strlen(c_message) + 1024]; 
  memset(msg, 0, strlen(file_name) + strlen(c_message) + 1024);
  sprintf(msg, "exception at %s:%d: %s", file_name, line_number, c_message);
  jstring java_message = _java_env->NewStringUTF(msg);
  jthrowable ex = (jthrowable)_java_env->NewObject(_jre, _ctr, java_message);
  _java_env->Throw(ex);
  
  longjmp( _c_env, -1 );
}

