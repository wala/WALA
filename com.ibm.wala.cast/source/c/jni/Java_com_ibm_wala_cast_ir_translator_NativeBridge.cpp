
#include <jni.h>

#include "CAstWrapper.h"
#include "Exceptions.h"
#include "com_ibm_wala_cast_ir_translator_NativeBridge.h"

extern "C" {

JNIEXPORT void JNICALL 
Java_com_ibm_wala_cast_ir_translator_NativeBridge_initialize(
  JNIEnv *env, 
  jclass cls)
{
  TRY(exp, env)

  jclass CAstNode = env->FindClass( "com/ibm/wala/cast/tree/CAstNode" );
  THROW_ANY_EXCEPTION(exp);
  jclass CAstOperator = env->FindClass( "com/ibm/wala/cast/tree/impl/CAstOperator" );
  THROW_ANY_EXCEPTION(exp);
  jclass CAstQualifier = env->FindClass( "com/ibm/wala/cast/tree/CAstQualifier" );
  THROW_ANY_EXCEPTION(exp);
  jclass CAstControlFlowMap = env->FindClass( "com/ibm/wala/cast/tree/CAstControlFlowMap" );
  THROW_ANY_EXCEPTION(exp);

#define _CODE_CONSTANTS 
#include "cast_constants.h"

#define _CODE_OPERATORS 
#include "cast_operators.h"

#define _CODE_QUALIFIERS
#include "cast_qualifiers.h"

#define _CODE_CFM
#include "cast_control_flow_map.h"

  CATCH()
}

}
