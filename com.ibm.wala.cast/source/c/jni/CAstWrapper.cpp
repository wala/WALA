#include <jni.h>

#include <iterator>

#include <stdarg.h>
#include <string.h>
#include <CAstWrapper.h>

#if defined(__MINGW32__) || defined(_MSC_VER) || defined(__APPLE__)
#define strndup(s,n) strdup(s)
#endif

#define __SIG( __nm ) "L" __nm ";"

#define __CTN "com/ibm/wala/cast/tree/CAst"
#define __CTS __SIG(  __CTN )

#define __CEN "com/ibm/wala/cast/tree/CAstEntity"
#define __CES __SIG(  __CEN )

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


#define XLATOR_PKG "com/ibm/wala/cast/ir/translator/"

#define XLATOR_CLS_NAME "NativeTranslatorToCAst"

static const char *XlatorCls = XLATOR_PKG XLATOR_CLS_NAME;

static const char *EntityCls = XLATOR_PKG "AbstractEntity";
static const char *ClassCls = XLATOR_PKG "AbstractClassEntity";
static const char *CodeEntityCls = XLATOR_PKG "AbstractCodeEntity";
static const char *ScriptCls = XLATOR_PKG "AbstractScriptEntity";
static const char *FieldCls = XLATOR_PKG "AbstractFieldEntity";
static const char *GlobalCls = XLATOR_PKG "AbstractGlobalEntity";

CAstWrapper::CAstWrapper(JNIEnv *env, Exceptions &ex, jobject xlator) 
  : java_ex(ex), env(env), xlator(xlator)
{
  this->CAstNode = env->FindClass( __CNN );
  this->CAstInterface = env->FindClass( __CTN );
  this->HashSet = env->FindClass("java/util/HashSet");
  this->LinkedList = env->FindClass("java/util/LinkedList");
  this->NativeBridge =
    env->FindClass("com/ibm/wala/cast/ir/translator/NativeBridge");
  this->NativeTranslatorToCAst =
    env->FindClass("com/ibm/wala/cast/ir/translator/NativeTranslatorToCAst");

  jfieldID castFieldID = env->GetFieldID(NativeBridge, "Ast", "Lcom/ibm/wala/cast/tree/CAst;");
  this->Ast = env->GetObjectField(xlator, castFieldID);

  jclass xlatorCls = env->FindClass( XlatorCls );
  this->_makeLocation = env->GetMethodID(xlatorCls, "makeLocation", "(IIII)Lcom/ibm/wala/cast/tree/CAstSourcePositionMap$Position;");
  THROW_ANY_EXCEPTION(java_ex);

  this->NativeEntity = env->FindClass(EntityCls);
  THROW_ANY_EXCEPTION(java_ex);
  this->addScopedEntity = env->GetMethodID(NativeEntity, "addScopedEntity", "(Lcom/ibm/wala/cast/tree/CAstNode;Lcom/ibm/wala/cast/tree/CAstEntity;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->entityGetType = env->GetMethodID(NativeEntity, "getType", "()Lcom/ibm/wala/cast/tree/CAstType;");
  THROW_ANY_EXCEPTION(java_ex);

  this->NativeClassEntity = env->FindClass(ClassCls);
  THROW_ANY_EXCEPTION(java_ex);
  this->classEntityInit = env->GetMethodID(NativeClassEntity, "<init>", "(Lcom/ibm/wala/cast/tree/CAstType$Class;)V");
  THROW_ANY_EXCEPTION(java_ex);


  this->NativeCodeEntity = env->FindClass(CodeEntityCls);
  THROW_ANY_EXCEPTION(java_ex);
  this->astField = env->GetFieldID(NativeCodeEntity, "Ast", "Lcom/ibm/wala/cast/tree/CAstNode;");
  THROW_ANY_EXCEPTION(java_ex);
  this->codeSetGotoTarget = env->GetMethodID(NativeCodeEntity, "setGotoTarget", "(Lcom/ibm/wala/cast/tree/CAstNode;Lcom/ibm/wala/cast/tree/CAstNode;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->codeSetLabelledGotoTarget = env->GetMethodID(NativeCodeEntity, "setLabelledGotoTarget", "(Lcom/ibm/wala/cast/tree/CAstNode;Lcom/ibm/wala/cast/tree/CAstNode;Ljava/lang/Object;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->setNodePosition = env->GetMethodID(NativeCodeEntity, "setNodePosition", "(Lcom/ibm/wala/cast/tree/CAstNode;Lcom/ibm/wala/cast/tree/CAstSourcePositionMap$Position;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->setNodeType = env->GetMethodID(NativeCodeEntity, "setNodeType", "(Lcom/ibm/wala/cast/tree/CAstNode;Lcom/ibm/wala/cast/tree/CAstType;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->setPosition = env->GetMethodID(NativeEntity, "setPosition", "(Lcom/ibm/wala/cast/tree/CAstSourcePositionMap$Position;)V");
  THROW_ANY_EXCEPTION(java_ex);

  this->NativeFieldEntity = env->FindClass(FieldCls);
  THROW_ANY_EXCEPTION(java_ex);
  this->fieldEntityInit = env->GetMethodID(NativeFieldEntity, "<init>", "(Ljava/lang/String;Ljava/util/Set;Z" __CES ")V");
  THROW_ANY_EXCEPTION(java_ex);

  this->NativeGlobalEntity = env->FindClass(GlobalCls);
  THROW_ANY_EXCEPTION(java_ex);
  this->globalEntityInit = env->GetMethodID(NativeGlobalEntity, "<init>", "(Ljava/lang/String;Lcom/ibm/wala/cast/tree/CAstType;Ljava/util/Set;)V");
  THROW_ANY_EXCEPTION(java_ex);

  this->AbstractScriptEntity = env->FindClass(ScriptCls);
  THROW_ANY_EXCEPTION(java_ex);

  this->makeNode0 = env->GetMethodID(CAstInterface, __MN, zeroSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode1 = env->GetMethodID(CAstInterface, __MN, oneSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode2 = env->GetMethodID(CAstInterface, __MN, twoSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode3 = env->GetMethodID(CAstInterface, __MN, threeSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode4 = env->GetMethodID(CAstInterface, __MN, fourSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode5 = env->GetMethodID(CAstInterface, __MN, fiveSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode6 = env->GetMethodID(CAstInterface, __MN, sixSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNodeNary = env->GetMethodID(CAstInterface, __MN, narySig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeNode1Nary = env->GetMethodID(CAstInterface, __MN, oneNarySig);
  THROW_ANY_EXCEPTION(java_ex);

  this->makeBool = env->GetMethodID(CAstInterface, __MC, boolSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeChar = env->GetMethodID(CAstInterface, __MC, charSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeShort = env->GetMethodID(CAstInterface, __MC, shortSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeInt = env->GetMethodID(CAstInterface, __MC, intSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeLong = env->GetMethodID(CAstInterface, __MC, longSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeDouble = env->GetMethodID(CAstInterface, __MC, doubleSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeFloat = env->GetMethodID(CAstInterface, __MC, floatSig);
  THROW_ANY_EXCEPTION(java_ex);
  this->makeObject = env->GetMethodID(CAstInterface, __MC, objectSig);
  THROW_ANY_EXCEPTION(java_ex);

  this->getChild = env->GetMethodID(CAstNode, "getChild", "(I)" __CNS);
  THROW_ANY_EXCEPTION(java_ex);
  this->_getChildCount = env->GetMethodID(CAstNode, "getChildCount", "()I");
  THROW_ANY_EXCEPTION(java_ex);
  this->getValue = env->GetMethodID(CAstNode, "getValue", "()" __OBJS);
  THROW_ANY_EXCEPTION(java_ex);
  this->_getKind = env->GetMethodID(CAstNode, "getKind", "()I");
  THROW_ANY_EXCEPTION(java_ex);

  jclass CAstMemberReference = env->FindClass( __CRN );
  THROW_ANY_EXCEPTION(java_ex);
  jfieldID ff = env->GetStaticFieldID(CAstMemberReference, "FUNCTION", __CRS);
  THROW_ANY_EXCEPTION(java_ex);
  this->callReference = env->GetStaticObjectField(CAstMemberReference, ff);
  THROW_ANY_EXCEPTION(java_ex);

  this->CAstPrinter = env->FindClass("com/ibm/wala/cast/util/CAstPrinter");
  THROW_ANY_EXCEPTION(java_ex);
  this->castPrint = env->GetStaticMethodID(CAstPrinter, "print", "(Lcom/ibm/wala/cast/tree/CAstNode;)Ljava/lang/String;");
  THROW_ANY_EXCEPTION(java_ex);

  this->hashSetInit = env->GetMethodID(HashSet, "<init>", "()V");
  THROW_ANY_EXCEPTION(java_ex);
  this->hashSetAdd = env->GetMethodID(HashSet, "add", "(Ljava/lang/Object;)Z");
  THROW_ANY_EXCEPTION(java_ex);

  this->linkedListInit = env->GetMethodID(LinkedList, "<init>", "()V");
  THROW_ANY_EXCEPTION(java_ex);
  this->linkedListAdd = env->GetMethodID(LinkedList, "add", "(Ljava/lang/Object;)Z");
  THROW_ANY_EXCEPTION(java_ex);

  jclass obj = env->FindClass( __OBJN );
  THROW_ANY_EXCEPTION(java_ex);
  this->toString = env->GetMethodID(obj, "toString", "()Ljava/lang/String;");
  THROW_ANY_EXCEPTION(java_ex);
  this->getClass = env->GetMethodID(obj, "getClass", "()Ljava/lang/Class;");
  THROW_ANY_EXCEPTION(java_ex);

  jclass intcls = env->FindClass("java/lang/Integer");
  THROW_ANY_EXCEPTION(java_ex);
  this->intValue = env->GetMethodID(intcls, "intValue", "()I");
  THROW_ANY_EXCEPTION(java_ex);

  jclass castEntity = env->FindClass("com/ibm/wala/cast/tree/CAstEntity");
  THROW_ANY_EXCEPTION(java_ex);
  this->_getEntityName = env->GetMethodID(castEntity, "getName", "()Ljava/lang/String;");

  CAstSymbol = env->FindClass("com/ibm/wala/cast/tree/impl/CAstSymbolImpl");
  THROW_ANY_EXCEPTION(java_ex);
  this->castSymbolInit1 = 
    env->GetMethodID(CAstSymbol, "<init>", "(Ljava/lang/String;)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->castSymbolInit2 = 
    env->GetMethodID(CAstSymbol, "<init>", "(Ljava/lang/String;Z)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->castSymbolInit3 = 
    env->GetMethodID(CAstSymbol, "<init>", "(Ljava/lang/String;ZZ)V");
  THROW_ANY_EXCEPTION(java_ex);
  this->castSymbolInit4 = 
    env->GetMethodID(CAstSymbol, "<init>", "(Ljava/lang/String;ZZLjava/lang/Object;)V");
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
    THROW_ANY_EXCEPTION(java_ex);
  }
  
  return result;
}

jobject CAstWrapper::makeSet(list<jobject> *elts) {
  jobject set = env->NewObject(HashSet, hashSetInit);
  THROW_ANY_EXCEPTION(java_ex);

  if (elts == NULL) return set;

  for(list<jobject>::iterator it=elts->begin(); it!=elts->end(); it++) {
    env->CallBooleanMethod(set, hashSetAdd, *it);
  }
  
  return set;
}

jobject CAstWrapper::makeList(list<jobject> *elts) {
  jobject set = env->NewObject(LinkedList, linkedListInit);
  THROW_ANY_EXCEPTION(java_ex);

  if (elts == NULL) return set;

  for(list<jobject>::iterator it=elts->begin(); it!=elts->end(); it++) {
    env->CallBooleanMethod(set, linkedListAdd, *it);
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

jobject CAstWrapper::makeSymbol(const char *name) {
  char *safeName = strndup(name, strlen(name)+1);
  jobject val = env->NewStringUTF( safeName );
  delete safeName;

  jobject s = env->NewObject(CAstSymbol, castSymbolInit1, val);
  THROW_ANY_EXCEPTION(java_ex);

  LOG(s);
  return s;
}

jobject CAstWrapper::makeSymbol(const char *name, bool isFinal) {
  char *safeName = strndup(name, strlen(name)+1);
  jobject val = env->NewStringUTF( safeName );
  delete safeName;

  THROW_ANY_EXCEPTION(java_ex);

  jobject s = env->NewObject(CAstSymbol, castSymbolInit2, val, isFinal);
  LOG(s);
  return s;
}

jobject 
  CAstWrapper::makeSymbol(const char *name, bool isFinal, bool isCaseInsensitive) 
{
  char *safeName = strndup(name, strlen(name)+1);
  jobject val = env->NewStringUTF( safeName );
  delete safeName;

  jobject s = env->NewObject(CAstSymbol, castSymbolInit3, val, isFinal, isCaseInsensitive);
  THROW_ANY_EXCEPTION(java_ex);

  LOG(s);
  return s;
}

jobject 
  CAstWrapper::makeSymbol(const char *name, 
			  bool isFinal, 
			  bool isCaseInsensitive, 
			  jobject defaultValue) 
{
  char *safeName = strndup(name, strlen(name)+1);
  jobject val = env->NewStringUTF( safeName );
  delete safeName;

  jobject s = env->NewObject(CAstSymbol, castSymbolInit4, val, isFinal, isCaseInsensitive, defaultValue);
  THROW_ANY_EXCEPTION(java_ex);

  LOG(s);
  return s;
}

void CAstWrapper::addChildEntity(jobject parent, jobject n, jobject child) 
{
  env->CallVoidMethod(parent, addScopedEntity, n, child);
}

void CAstWrapper::setGotoTarget(jobject entity, jobject from, jobject to) {
  env->CallVoidMethod(entity, codeSetGotoTarget, from, to);
}

void CAstWrapper::setGotoTarget(jobject entity, jobject from, jobject to, bool label) {
  jobject javaLabel;
  jclass boolean = env->FindClass("java/lang/Boolean");
  if (label) {
    jfieldID trueId =
      env->GetStaticFieldID(boolean, "TRUE", "Ljava/lang/Boolean;");
    javaLabel = env->GetStaticObjectField(boolean, trueId);
  } else {
    jfieldID falseId =
      env->GetStaticFieldID(boolean, "FALSE", "Ljava/lang/Boolean;");
    javaLabel = env->GetStaticObjectField(boolean, falseId);
  }
  setGotoTarget(entity, from, to, javaLabel);
}

void CAstWrapper::setGotoTarget(jobject entity, jobject from, jobject to, jobject label) {
  env->CallVoidMethod(entity, codeSetLabelledGotoTarget, from, to, label);
}

void CAstWrapper::setLocation(jobject entity, jobject loc) {
  env->CallVoidMethod(entity, setPosition, loc);
}

void CAstWrapper::setAstNodeLocation(jobject entity, jobject astNode, jobject loc) {
  env->CallVoidMethod(entity, setNodePosition, astNode, loc);
}

void CAstWrapper::setAstNodeType(jobject entity, jobject astNode, jobject loc) {
  env->CallVoidMethod(entity, setNodeType, astNode, loc);
}

jobject CAstWrapper::makeLocation(int fl, int fc, int ll, int lc) {
  return env->CallObjectMethod(xlator, _makeLocation, fl, fc, ll, lc);
}

jobject CAstWrapper::makeFieldEntity(jobject declaringClass, jobject name, bool isStatic, list<jobject> *modifiers) {

  jobject entity = env->NewObject(NativeFieldEntity, fieldEntityInit, getConstantValue(name), makeSet(modifiers), isStatic, declaringClass);

  THROW_ANY_EXCEPTION(java_ex);
  return entity;
}

jobject CAstWrapper::makeClassEntity(jobject classType) {

  jobject entity = env->NewObject(NativeClassEntity, classEntityInit, classType);

  THROW_ANY_EXCEPTION(java_ex);
  return entity;
}

jobject CAstWrapper::makeGlobalEntity(char *name, jobject type, list<jobject> *modifiers) {
  char *safeData = strdup(name);
  jobject val = env->NewStringUTF( safeData );
  THROW_ANY_EXCEPTION(java_ex);
  delete safeData;

  jobject entity = env->NewObject(NativeGlobalEntity, globalEntityInit, val, type, makeSet(modifiers));
  THROW_ANY_EXCEPTION(java_ex);

  return entity;
}

jobject CAstWrapper::getEntityAst(jobject entity) {
  jobject result = env->GetObjectField(entity, astField);
  THROW_ANY_EXCEPTION(java_ex);
  LOG(result);
  return result;
}

void CAstWrapper::setEntityAst(jobject entity, jobject ast) {
  env->SetObjectField(entity, astField, ast);
  THROW_ANY_EXCEPTION(java_ex);
}

jobject CAstWrapper::getEntityType(jobject entity) {
  jobject result = env->CallObjectMethod(entity, entityGetType);
  THROW_ANY_EXCEPTION(java_ex);
  return result;
}
 
void CAstWrapper::die(const char *message) {
  THROW(java_ex, message);
}
 
