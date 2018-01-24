#include "launch.h"
#include <stdlib.h>
#include <string.h>

void die(JNIEnv *java_env) {
  jclass Object =
    java_env->FindClass("java/lang/Object");
  
  jclass NativeTranslatorTest =
    java_env->FindClass("com/ibm/wala/cast/test/TestNativeTranslator");

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
}

int main(int argc, char **argv) {
  char *buf = (char *)malloc((strlen(argv[1]) + 100) * sizeof(char));
  strcpy(buf, argv[1]);

  printf("1: %s\n", buf);
  
  JNIEnv *java_env = launch_jvm(buf);
  
  printf("2: %s, %p\n", buf, java_env);
  
  jclass NativeTranslatorTest =
    java_env->FindClass("com/ibm/wala/cast/test/TestNativeTranslator");
  if (java_env->ExceptionCheck()) { die(java_env); }

  printf("3: %s\n", buf);
  
  jmethodID testInit =
    java_env->GetMethodID(NativeTranslatorTest, "<init>", "()V");
  if (java_env->ExceptionCheck()) { die(java_env); }

  printf("4: %s\n", buf);
  
  jobject test =
    java_env->NewObject(NativeTranslatorTest, testInit);
  if (java_env->ExceptionCheck()) { die(java_env); }

  printf("5: %s\n", buf);
  
  jmethodID testAst =
    java_env->GetMethodID(NativeTranslatorTest, "testNativeCAst", "()V");
  if (java_env->ExceptionCheck()) { die(java_env); }
  
  printf("6: %s\n", buf);
  
  java_env->CallVoidMethod(test, testAst);
  if (java_env->ExceptionCheck()) { die(java_env); }

  printf("6: %s\n", buf);

  exit(0);
}
