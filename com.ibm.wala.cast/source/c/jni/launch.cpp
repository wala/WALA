#include <jni.h>
#include <iostream>
#include <string>
#include "Exceptions.h"
#include "CAstWrapper.h"
using namespace std;

JavaVM *javaVM;

JNIEnv *launch() {
   JavaVMOption jvmopt[1];
   jvmopt[0].optionString = "-Djava.class.path=" WALA_CLASSPATH ":.";

   JavaVMInitArgs vmArgs;
   vmArgs.version = JNI_VERSION_1_2;
   vmArgs.nOptions = 1;
   vmArgs.options = jvmopt;
   vmArgs.ignoreUnrecognized = JNI_TRUE;

   // Create the JVM
   JNIEnv *jniEnv;
   long flag = JNI_CreateJavaVM(&javaVM, (void**)
      &jniEnv, &vmArgs);
   if (flag == JNI_ERR) {
      cout << "Error creating VM. Exiting...\n";
      return NULL;
   }

   return jniEnv;
}

void kill() {
   javaVM->DestroyJavaVM();
}

void run() {
  JNIEnv *java_env = launch();
  TRY(exp, java_env)
  
  CAstWrapper CAst(java_env, exp, NULL);
  THROW_ANY_EXCEPTION(exp);

  CATCH()
}
