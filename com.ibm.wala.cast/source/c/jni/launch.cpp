#include <cstdio>
 #include <jni.h>
#include <string>
#include "Exceptions.h"
#include "CAstWrapper.h"
#include "launch.h"

JavaVM *javaVM;

static string javaPathFlag(const string &kind, const char *classpath) {
   return "-Djava." + kind + ".path=" + classpath;
}

JNIEnv *launch_jvm(char *classpath) {
   JavaVMOption jvmopt[2];

   string buf_jcp = javaPathFlag("class", classpath);
   jvmopt[0].optionString = const_cast<char *>(buf_jcp.c_str());

   string buf_jlp = javaPathFlag("library", classpath);
   jvmopt[1].optionString = const_cast<char *>(buf_jlp.c_str());

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

// Local variables:
// c-basic-offset: 3
// End:
