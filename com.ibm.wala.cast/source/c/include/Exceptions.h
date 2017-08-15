#ifndef EXCEPTIONS_H
#define EXCEPTIONS_H

/**
 *  The combination of the macroes and the Exceptions class declared 
 * in this file is used to provide a (shaky) veneer of exception handling
 * to JNI code.
 *
 *  The idea is that a C function called from JNI will be enclosed with
 * TRY/CATCH macroes, and the CPP_EXP_NAME variable defined will be passed
 * to callees who might want to throw an exception.  The callees will use
 * the THROW macro to throw exceptions.  When a THROW is executed, control
 * is transferred to the CATCH, and a RuntimeException is thrown to the
 * calling Java code when the C code returns.
 *
 *  WARNING: this is C code, so you should know better than to expect any
 * kind of robust, high-level exception-handling semantics.  The way to use
 * this code is to put a TRY/CATCH combination in the JNI entry point, to do
 * only trivial things after the CATCH, and to use only THROW anywhere else.
 *
 *  The implementation does the obvious dance with setjmp and longjmp, which
 * is one reason it is rather shaky.  For instance, TRY has to be a macro,
 * because otherwise the jump buffer would be invalidated when the TRY function
 * returned.
 */

#include <jni.h>

extern "C" {
#include <setjmp.h>
}

#define TRY(CPP_EXP_NAME, JAVA_ENV_VAR)			\
{ jmp_buf jump_buffer;					\
  if (setjmp(jump_buffer) == 0) {			\
    Exceptions CPP_EXP_NAME(JAVA_ENV_VAR, jump_buffer);

#define CATCH()			\
  }				\
}

#define START_CATCH_BLOCK()	\
  } else {

#define END_CATCH_BLOCK()	\
  }				\
}

#define THROW(CPP_EXP_NAME, MESSAGE)	\
(CPP_EXP_NAME).throwException(__FILE__, __LINE__, MESSAGE)

#define THROW_ANY_EXCEPTION(CPP_EXP_NAME)	\
(CPP_EXP_NAME).throwAnyException(__FILE__, __LINE__)

#define NULL_CHECK(cpp_exp_name, c_expr) \
  if ((c_expr) == NULL) {						\
  (CPP_EXP_NAME).throwException(__FILE__, __LINE__, "unexpected null value"); \
}

#if __WIN32__
#ifdef BUILD_CAST_DLL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif
class DLLEXPORT Exceptions {

#else
class Exceptions {
#endif

private:
  JNIEnv *_java_env;
  jmp_buf& _c_env;
  jclass _jre;
  jmethodID _ctr;
  jmethodID _wrapper_ctr;

public:
  Exceptions(JNIEnv *java_env, jmp_buf& c_env);

  void throwException(const char *file_name, int line_number);
  void throwAnyException(const char *file_name, int line_number);
  void throwException(const char *file_name, int line_number, const char *c_message);
};
#endif
