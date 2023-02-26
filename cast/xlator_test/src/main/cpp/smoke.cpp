#include "CAstWrapper.h"
#include "com_ibm_wala_cast_test_TestNativeTranslator.h"

JNIEXPORT jobject JNICALL Java_com_ibm_wala_cast_test_TestNativeTranslator_inventAst
  (JNIEnv *java_env, jclass cls, jobject ast)
{
  TRY(exp, java_env)

  CAstWrapper CAst(java_env, exp, ast);
  THROW_ANY_EXCEPTION(exp);

  return
    CAst.makeNode(CAst.BINARY_EXPR,
      CAst.OP_ADD,
      CAst.makeConstant(1),
      CAst.makeConstant(2));
  
  CATCH()
  return NULL;
}
