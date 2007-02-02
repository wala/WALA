#include <jni.h>

#include <iterator>

#include <stdarg.h>
#include <string.h>
#include <CAstWrapper.h>

#if defined(__MINGW32__) || defined(_MSC_VER)
#define strndup(s,n) strdup(s)
#endif

#define __SIG( __nm ) "L" __nm ";"

#define __CTN "com/ibm/wala/cast/tree/CAst"
#define __CTS __SIG(  __CTN )

#define __CNN "com/ibm/wala/cast/tree/CAstNode"
#define __CNS __SIG( __CNN )

#define __CRN "com/ibm/wala/cast/tree/CAstMemberReference"
#define __CRS __SIG( __CRN )

#define __OBJN "java/lang/Object"
#define __OBJS __SIG( __OBJN )

static const char *__MN = "makeNode";
static const char *__MC = "makeConstant";
 
static const char *zeroSig = "(I)" __CNS;
static const char *oneSig = "(I" __CNS ")" __CNS;
static const char *twoSig = "(I" __CNS __CNS ")" __CNS;
static const char *threeSig = "(I" __CNS __CNS __CNS ")" __CNS;
static const char *fourSig = "(I" __CNS __CNS __CNS __CNS ")" __CNS;
static const char *fiveSig = "(I" __CNS __CNS __CNS __CNS __CNS ")" __CNS;
static const char *sixSig = "(I" __CNS __CNS __CNS __CNS __CNS __CNS ")"  __CNS;
static const char *narySig = "(I[" __CNS ")"  __CNS;
static const char *oneNarySig = "(I" __CNS "[" __CNS ")"  __CNS;

static const char *boolSig = "(Z)" __CNS;
static const char *charSig = "(C)" __CNS;
static const char *shortSig = "(S)" __CNS;
static const char *intSig = "(I)" __CNS;
static const char *longSig = "(J)" __CNS;
static const char *doubleSig = "(D)" __CNS;
static const char *floatSig = "(F)" __CNS;
static const char *objectSig = "(" __OBJS ")" __CNS;

CAstWrapper::CAstWrapper(JNIEnv *env, Exceptions &ex, jobject Ast) 
  : java_ex(ex), env(env), Ast(Ast) 
{
  this->CAstNode = env->FindClass( __CNN );
  this->CAstInterface = env->FindClass( __CTN );
  this->HashSet = env->FindClass("java/util/HashSet");
  this->LinkedList = env->FindClass("java/util/LinkedList");

  this->makeNode0 = env->GetMethodID(CAstInterface, __MN, zeroSig);
  this->makeNode1 = env->GetMethodID(CAstInterface, __MN, oneSig);
  this->makeNode2 = env->GetMethodID(CAstInterface, __MN, twoSig);
  this->makeNode3 = env->GetMethodID(CAstInterface, __MN, threeSig);
  this->makeNode4 = env->GetMethodID(CAstInterface, __MN, fourSig);
  this->makeNode5 = env->GetMethodID(CAstInterface, __MN, fiveSig);
  this->makeNode6 = env->GetMethodID(CAstInterface, __MN, sixSig);
  this->makeNodeNary = env->GetMethodID(CAstInterface, __MN, narySig);
  this->makeNode1Nary = env->GetMethodID(CAstInterface, __MN, oneNarySig);

  this->makeBool = env->GetMethodID(CAstInterface, __MC, boolSig);
  this->makeChar = env->GetMethodID(CAstInterface, __MC, charSig);
  this->makeShort = env->GetMethodID(CAstInterface, __MC, shortSig);
  this->makeInt = env->GetMethodID(CAstInterface, __MC, intSig);
  this->makeLong = env->GetMethodID(CAstInterface, __MC, longSig);
  this->makeDouble = env->GetMethodID(CAstInterface, __MC, doubleSig);
  this->makeFloat = env->GetMethodID(CAstInterface, __MC, floatSig);
  this->makeObject = env->GetMethodID(CAstInterface, __MC, objectSig);

  this->getChild = env->GetMethodID(CAstNode, "getChild", "(I)" __CNS);
  this->_getChildCount = env->GetMethodID(CAstNode, "getChildCount", "()I");
  this->getValue = env->GetMethodID(CAstNode, "getValue", "()" __OBJS);
  this->_getKind = env->GetMethodID(CAstNode, "getKind", "()I");

  jclass CAstMemberReference = env->FindClass( __CRN );
  THROW_ANY_EXCEPTION(java_ex);
  jfieldID ff = env->GetStaticFieldID(CAstMemberReference, "FUNCTION", __CRS);
  THROW_ANY_EXCEPTION(java_ex);
  this->callReference = env->GetStaticObjectField(CAstMemberReference, ff);
  THROW_ANY_EXCEPTION(java_ex);

  this->CAstPrinter = env->FindClass("com/ibm/wala/cast/util/CAstPrinter");
  this->castPrint = env->GetStaticMethodID(CAstPrinter, "print", "(Lcom/ibm/wala/cast/tree/CAstNode;)Ljava/lang/String;");

  this->hashSetInit = env->GetMethodID(HashSet, "<init>", "()V");
  this->hashSetAdd = env->GetMethodID(HashSet, "add", "(Ljava/lang/Object;)Z");

  this->linkedListInit = env->GetMethodID(LinkedList, "<init>", "()V");
  this->linkedListAdd = env->GetMethodID(LinkedList, "add", "(Ljava/lang/Object;)Z");

  jclass obj = env->FindClass( __OBJN );
  this->toString = env->GetMethodID(obj, "toString", "()Ljava/lang/String;");
  this->getClass = env->GetMethodID(obj, "getClass", "()Ljava/lang/Class;");

  jclass intcls = env->FindClass("java/lang/Integer");
  this->intValue = env->GetMethodID(intcls, "intValue", "()I");

  jclass castEntity = env->FindClass("com/ibm/wala/cast/tree/CAstEntity");
  this->_getEntityName = env->GetMethodID(castEntity, "getName", "()Ljava/lang/String;");

  THROW_ANY_EXCEPTION(java_ex);
}

#define _CPP_CONSTANTS 
#include "cast_constants.h"

#define _CPP_OPERATORS 
#include "cast_operators.h"

#define _CPP_QUALIFIERS
#include "cast_qualifiers.h"

#define _CPP_CFM
#include "cast_control_flow_map.h"

void CAstWrapper::log(jobject castTree) {
  jstring jstr = (jstring)env->CallStaticObjectMethod(CAstPrinter, castPrint, castTree);
  const char *cstr = env->GetStringUTFChars(jstr, NULL);
  fprintf(stderr, "%s\n", cstr); 
  env->ReleaseStringUTFChars(jstr, cstr);
  THROW_ANY_EXCEPTION(java_ex);
}

void CAstWrapper::assertIsCAstNode(jobject obj, int n) {
  if (! env->IsInstanceOf(obj, CAstNode)) {
    jstring jstr = (jstring)env->CallObjectMethod(obj, toString);
    const char *cstr = env->GetStringUTFChars(jstr, NULL);

    jobject cls = env->CallObjectMethod(obj, getClass);
    jstring jclsstr = (jstring)env->CallObjectMethod(cls, toString);
    const char *cclsstr = env->GetStringUTFChars(jclsstr, NULL);

#if defined(_MSC_VER)
	char* buf = (char*)_alloca(strlen(cstr) + strlen(cclsstr) + 100);
#else
    char buf[ strlen(cstr) + strlen(cclsstr) + 100 ];
#endif
    sprintf(buf, "argument %d (%s of type %s) is not a CAstNode\n", n, cstr, cclsstr); 

    env->ReleaseStringUTFChars(jstr, cstr);
    env->ReleaseStringUTFChars(jclsstr, cclsstr);
    THROW(java_ex, buf);
  }
}
  
jobject CAstWrapper::makeNode(int kind) {
  jobject r = env->CallObjectMethod(Ast, makeNode0, (jint) kind);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1) {
  assertIsCAstNode(c1, 1);
  jobject r = env->CallObjectMethod(Ast, makeNode1, (jint) kind, c1);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1, jobject c2) {
  assertIsCAstNode(c1, 1);
  assertIsCAstNode(c2, 2);
  jobject r = env->CallObjectMethod(Ast, makeNode2, (jint) kind, c1, c2);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1, jobject c2, jobject c3) {
  assertIsCAstNode(c1, 1);
  assertIsCAstNode(c2, 2);
  assertIsCAstNode(c3, 3);
  jobject r = env->CallObjectMethod(Ast, makeNode3, (jint) kind, c1, c2, c3);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1, jobject c2, jobject c3, jobject c4) {
  assertIsCAstNode(c1, 1);
  assertIsCAstNode(c2, 2);
  assertIsCAstNode(c3, 3);
  assertIsCAstNode(c4, 4);
  jobject r = env->CallObjectMethod(Ast, makeNode4, (jint) kind, c1, c2, c3, c4);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1, jobject c2, jobject c3, jobject c4, jobject c5) {
  assertIsCAstNode(c1, 1);
  assertIsCAstNode(c2, 2);
  assertIsCAstNode(c3, 3);
  assertIsCAstNode(c4, 4);
  assertIsCAstNode(c5, 5);
  jobject r = env->CallObjectMethod(Ast, makeNode5, (jint) kind, c1, c2, c3, c4, c5);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject c1, jobject c2, jobject c3, jobject c4, jobject c5, jobject c6) {
  assertIsCAstNode(c1, 1);
  assertIsCAstNode(c2, 2);
  assertIsCAstNode(c3, 3);
  assertIsCAstNode(c4, 4);
  assertIsCAstNode(c5, 5);
  assertIsCAstNode(c6, 6);
  jobject r = env->CallObjectMethod(Ast, makeNode6, (jint) kind, c1, c2, c3, c4, c5, c6);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobjectArray cs) {
  jobject r = env->CallObjectMethod(Ast, makeNodeNary, (jint) kind, cs);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeNode(int kind, jobject n, jobjectArray cs) {
  jobject r = env->CallObjectMethod(Ast, makeNode1Nary, (jint) kind, n, cs);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(bool val) {
  jobject r = env->CallObjectMethod(Ast, makeBool, (jboolean)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(char val) {
  jobject r = env->CallObjectMethod(Ast, makeChar, (jchar)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(short val) {
  jobject r = env->CallObjectMethod(Ast, makeShort, (jshort)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(int val) {
  jobject r = env->CallObjectMethod(Ast, makeInt, (jint)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(long val) {
  jobject r = env->CallObjectMethod(Ast, makeLong, (jlong)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(double val) {
  jobject r = env->CallObjectMethod(Ast, makeDouble, (jdouble)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(float val) {
  jobject r = env->CallObjectMethod(Ast, makeFloat, (jfloat)val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(jobject val) {
  jobject r = env->CallObjectMethod(Ast, makeObject, val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::makeConstant(const char *strData) {
  return makeConstant(strData, strlen(strData));
}

jobject CAstWrapper::makeConstant(const char *strData, int strLen) {
  char *safeData = strndup(strData, strLen);
  jobject val = env->NewStringUTF( safeData );
  delete safeData;
  jobject r = env->CallObjectMethod(Ast, makeObject, val);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(r);
  return r;
}

jobject CAstWrapper::getNthChild(jobject castNode, int index) {
  jobject result = env->CallObjectMethod(castNode, getChild, index);
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}

int CAstWrapper::getChildCount(jobject castNode) {
  int result = env->CallIntMethod(castNode, _getChildCount);
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}

int CAstWrapper::getKind(jobject castNode) {
  jint result = env->CallIntMethod(castNode, _getKind);
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}

bool CAstWrapper::isConstantValue(jobject castNode) {
  jobject jval = env->CallObjectMethod(castNode, getValue);
  THROW_ANY_EXCEPTION(java_ex);
  return jval != NULL;
}

bool CAstWrapper::isConstantOfType(jobject castNode, const char *typeName) {
  jclass type = env->FindClass( typeName );
  THROW_ANY_EXCEPTION(java_ex);

  return isConstantOfType(castNode, type);
}

bool CAstWrapper::isConstantOfType(jobject castNode, jclass type) {
  //
  // one might think this test against null is not needed, since
  // IsInstanceoOf ought to return false given null and any type at
  // all, which is what happens with the `instanceof'operator in Java.
  // This does not seem to be the case however.
  //
  if (isConstantValue(castNode)) {
    jobject jval = env->CallObjectMethod(castNode, getValue);
    THROW_ANY_EXCEPTION(java_ex);
    jboolean result = env->IsInstanceOf(jval, type);
    THROW_ANY_EXCEPTION(java_ex);
    
    return (bool) result;
  } else {
    return false;
  }
}

bool CAstWrapper::isSwitchDefaultConstantValue(jobject castNode) {
  jobject jval = env->CallObjectMethod(castNode, getValue);
  THROW_ANY_EXCEPTION(java_ex);

  return env->IsSameObject(jval, SWITCH_DEFAULT);
}

const char *CAstWrapper::getStringConstantValue(jobject castNode) {
  jstring jstr = (jstring)env->CallObjectMethod(castNode, getValue);
  THROW_ANY_EXCEPTION(java_ex);
  const char *cstr1 = env->GetStringUTFChars(jstr, NULL);
  const char *cstr2 = strdup( cstr1 );
  env->ReleaseStringUTFChars(jstr, cstr1);
  THROW_ANY_EXCEPTION(java_ex);

  return cstr2;
}
  
int CAstWrapper::getIntConstantValue(jobject castNode) {
  jobject jval = env->CallObjectMethod(castNode, getValue);
  THROW_ANY_EXCEPTION(java_ex);
  int cval = env->CallIntMethod(jval, intValue);
  THROW_ANY_EXCEPTION(java_ex);

  return cval;
}
  
jobject CAstWrapper::getConstantValue(jobject castNode) {
  jobject jval = env->CallObjectMethod(castNode, getValue);
  THROW_ANY_EXCEPTION(java_ex);
  return jval;
}
  
jobjectArray CAstWrapper::makeArray(list<jobject> *elts) {
  return makeArray(CAstNode, elts);
}

jobjectArray CAstWrapper::makeArray(jclass type, list<jobject> *elts) {
  jobjectArray result = env->NewObjectArray(elts->size(), type, NULL);
  int i = 0;
  for(list<jobject>::iterator it=elts->begin(); it!=elts->end(); it++) {
    env->SetObjectArrayElement(result, i++, *it);
  }
  
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}

jobjectArray CAstWrapper::makeArray(int count, jobject elts[]) {
  return makeArray(CAstNode, count, elts);
}

jobjectArray CAstWrapper::makeArray(jclass type, int count, jobject elts[]) {
  jobjectArray result = env->NewObjectArray(count, type, NULL);
  THROW_ANY_EXCEPTION(java_ex);

  for(int i = 0; i < count; i++) {
    env->SetObjectArrayElement(result, i, elts[i]);
  }
  
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}

jobject CAstWrapper::makeSet(list<jobject> *elts) {
  jobject set = env->NewObject(HashSet, hashSetInit);
  THROW_ANY_EXCEPTION(java_ex);

  if (elts == NULL) return set;

  for(list<jobject>::iterator it=elts->begin(); it!=elts->end(); it++) {
    env->CallVoidMethod(set, hashSetAdd, *it);
  }
  
  return set;
}

jobject CAstWrapper::makeList(list<jobject> *elts) {
  jobject set = env->NewObject(LinkedList, linkedListInit);
  THROW_ANY_EXCEPTION(java_ex);

  if (elts == NULL) return set;

  for(list<jobject>::iterator it=elts->begin(); it!=elts->end(); it++) {
    env->CallVoidMethod(set, linkedListAdd, *it);
  }
  
  return set;
}

jobject CAstWrapper::getCallReference() {
  return callReference;
}

const char *CAstWrapper::getEntityName(jobject entity) {
  jstring jstr = (jstring) env->CallObjectMethod(entity, _getEntityName);
  THROW_ANY_EXCEPTION(java_ex);

  const char *cstr1 = env->GetStringUTFChars(jstr, NULL);
  const char *cstr2 = strdup( cstr1 );
  env->ReleaseStringUTFChars(jstr, cstr1);
  THROW_ANY_EXCEPTION(java_ex);

  return cstr2;
}
