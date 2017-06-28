#include <jni.h>
#include <string.h>
#include <strings.h>
#include "Exceptions.h"
#include "CAstWrapper.h"

JavaVM *javaVM;

JNIEnv *launch(char *classpath) {
   JavaVMOption jvmopt[2];

   const char *jcp = "-Djava.class.path=";
   char buf_jcp[ strlen(jcp) + strlen(classpath) + 1 ];
   sprintf(buf_jcp, "%s%s", jcp, classpath);
   jvmopt[0].optionString = buf_jcp;

   const char *jlp = "-Djava.library.path=";
   char buf_jlp[ strlen(jlp) + strlen(classpath) + 1 ];
   sprintf(buf_jlp, "%s%s", jlp, classpath);
   jvmopt[1].optionString = buf_jlp;

   JavaVMInitArgs vmArgs;
   vmArgs.version = JNI_VERSION_1_8;
   vmArgs.nOptions = 2;
   vmArgs.options = jvmopt;
   vmArgs.ignoreUnrecognized = JNI_TRUE;

   // Create the JVM
   JNIEnv *jniEnv;
   long flag = JNI_CreateJavaVM(&javaVM, (void**)
      &jniEnv, &vmArgs);
   if (flag == JNI_ERR) {
     fprintf(stderr, "Error creating VM. Exiting...\n");
      return NULL;
   }

   return jniEnv;
}

void kill() {
   javaVM->DestroyJavaVM();
}
