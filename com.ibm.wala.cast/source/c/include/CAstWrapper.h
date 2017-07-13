#ifndef _CAST_WRAPPER_H
#define _CAST_WRAPPER_H

#include <list>
#include "jni.h"
#include "Exceptions.h"
#include "launch.h"

using namespace std;


#ifdef TRACE_CAST_WRAPPER
#define LOG(x) log(x);

#else
#define LOG(x)

#endif

#if __WIN32__
#ifdef BUILD_CAST_DLL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif
class DLLEXPORT CAstWrapper {
#else

/**
 *  This class is a simple wrapper that provides a C++ object veneer
 * over JNI calls to a CAst object in Javaland.  This wrapper is used
 * by native code to build a CAst tree in Javaland.
 */
class CAstWrapper {
#endif


protected:
  JNIEnv *env;
  Exceptions &java_ex;
  jobject xlator;
  jobject Ast;
  jclass HashSet;
  jclass LinkedList;
  jmethodID hashSetInit;
  jmethodID hashSetAdd;
  jmethodID linkedListInit;
  jmethodID linkedListAdd;
  jfieldID astField;
  jclass AbstractScriptEntity;

private:
  jclass CAstNode;
  jclass CAstInterface;
  jclass CAstPrinter;
  jclass CAstSymbol;
  jclass CAstType;
  jclass NativeEntity;
  jclass NativeClassEntity;
  jclass NativeCodeEntity;
  jclass NativeFieldEntity;
  jclass NativeGlobalEntity;
  jclass NativeBridge;
  jclass NativeTranslatorToCAst;
  jmethodID classEntityInit;
  jmethodID castPrint;
  jmethodID makeNode0;
  jmethodID makeNode1;
  jmethodID makeNode2;
  jmethodID makeNode3;
  jmethodID makeNode4;
  jmethodID makeNode5;
  jmethodID makeNode6;
  jmethodID makeNodeNary;
  jmethodID makeNode1Nary;
  jmethodID makeBool;
  jmethodID makeChar;
  jmethodID makeShort;
  jmethodID makeInt;
  jmethodID makeLong;
  jmethodID makeDouble;
  jmethodID makeFloat;
  jmethodID makeObject;
  jmethodID getChild;
  jmethodID _getChildCount;
  jmethodID getValue;
  jmethodID _getKind;
  jmethodID toString;
  jmethodID getClass;
  jmethodID intValue;
  jmethodID _getEntityName;
  jmethodID castSymbolInit1;
  jmethodID castSymbolInit2;
  jmethodID castSymbolInit3;
  jmethodID castSymbolInit4;
  jmethodID addScopedEntity;
  jmethodID entityGetType;
  jmethodID fieldEntityInit;
  jmethodID globalEntityInit;
  jmethodID _makeLocation;
  jmethodID setNodePosition;
  jmethodID setNodeType;
  jmethodID setPosition;
  jmethodID codeSetGotoTarget;
  jmethodID codeSetLabelledGotoTarget;
  jobject callReference;

public:

#define _INCLUDE_CONSTANTS 
#include "cast_constants.h"

#define _INCLUDE_OPERATORS 
#include "cast_operators.h"

#define _INCLUDE_QUALIFIERS
#include "cast_qualifiers.h"

#define _INCLUDE_CFM
#include "cast_control_flow_map.h"

public:

  CAstWrapper(JNIEnv *env, Exceptions &ex, jobject Ast);

  virtual ~CAstWrapper() { }
  
  void assertIsCAstNode(jobject, int);

  jobject makeNode(int);

  jobject makeNode(int, jobject);

  jobject makeNode(int, jobject, jobject);

  jobject makeNode(int, jobject, jobject, jobject);

  jobject makeNode(int, jobject, jobject, jobject, jobject);

  jobject makeNode(int, jobject, jobject, jobject, jobject, jobject);

  jobject makeNode(int, jobject, jobject, jobject, jobject, jobject, jobject);

  jobject makeNode(int, jobjectArray);

  jobject makeNode(int, jobject, jobjectArray);

  jobject makeConstant(bool);

  jobject makeConstant(char);

  jobject makeConstant(short);

  jobject makeConstant(int);

  jobject makeConstant(long);

  jobject makeConstant(double);

  jobject makeConstant(float);

  jobject makeConstant(jobject);

  jobject makeConstant(const char *);

  jobject makeConstant(const char *, int);

  jobject getNthChild(jobject, int);

  int getChildCount(jobject);

  int getKind(jobject);

  bool isConstantValue(jobject);

  bool isConstantOfType(jobject, const char *);

  bool isConstantOfType(jobject, jclass);

  bool isSwitchDefaultConstantValue(jobject);

  const char *getStringConstantValue(jobject);

  jobject getConstantValue(jobject);

  int getIntConstantValue(jobject);

  jobjectArray makeArray(list<jobject> *);

  jobjectArray makeArray(jclass, list<jobject> *);

  jobjectArray makeArray(int, jobject[]);

  jobjectArray makeArray(jclass, int, jobject[]);

  jobject makeSet(list<jobject> *);

  jobject makeList(list<jobject> *);

  jobject getCallReference();

  const char *getEntityName(jobject);

  jobject makeSymbol(const char *);

  jobject makeSymbol(const char *, bool);

  jobject makeSymbol(const char *, bool, bool);

  jobject makeSymbol(const char *, bool, bool, jobject);

  void log(jobject);

  void addChildEntity(jobject, jobject, jobject);

  void setGotoTarget(jobject, jobject, jobject);
  
  void setGotoTarget(jobject, jobject, jobject, bool);
  
  void setGotoTarget(jobject, jobject, jobject, jobject);
  
  void setAstNodeLocation(jobject, jobject, jobject);

  void setAstNodeType(jobject, jobject, jobject);

  void setLocation(jobject, jobject);

  jobject makeLocation(int, int, int, int);

  jobject makeFieldEntity(jobject, jobject, bool, list<jobject> *);

  jobject makeGlobalEntity(char *, jobject, list<jobject> *);

  jobject makeClassEntity(jobject);

  jobject getEntityAst(jobject);

  virtual void setEntityAst(jobject, jobject);

  jobject getEntityType(jobject);

  void die(const char *);
};
#endif

