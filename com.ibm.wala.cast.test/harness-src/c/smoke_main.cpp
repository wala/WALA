#include "launch.h"
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv) {
  char buf[ strlen(argv[1]) + 1 ];
  strcpy(buf, argv[1]);

  printf("%s\n", buf);
  
  JNIEnv *java_env = launch(buf);
  
  jclass NativeTranslatorTest =
    java_env->FindClass("com/ibm/wala/cast/test/TestNativeTranslator");

  jmethodID testInit =
    java_env->GetMethodID(NativeTranslatorTest, "<init>", "()V");

  jobject test =
    java_env->NewObject(NativeTranslatorTest, testInit);

  jmethodID testAst =
    java_env->GetMethodID(NativeTranslatorTest, "testNativeCAst", "()V");
  
  java_env->CallVoidMethod(test, testAst);
  if (java_env->ExceptionCheck()) {
    jclass Object =
      java_env->FindClass("java/lang/Object");
    
    jmethodID testInit =
      java_env->GetMethodID(NativeTranslatorTest, "<init>", "()V");

    jmethodID toString =
      java_env->GetMethodID(Object, "toString", "()Ljava/lang/String;");

    jthrowable real_ex = java_env->ExceptionOccurred();
 
    jstring msg = (jstring) java_env->CallObjectMethod(real_ex, toString);

    jboolean f = true;
    const char *text = java_env->GetStringUTFChars(msg, &f);

    printf("exception: %s\n", text);

    java_env->ReleaseStringUTFChars(msg, text);

    exit(-1);
  } else {
    exit(0);
  }
}
