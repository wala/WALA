#if defined( _INCLUDE_CFM )
#define _CAstControlFlowMap( __id, __type )    static jobject __id;

#elif defined( _CPP_CFM )
#define _CAstControlFlowMap( __id, __type )    jobject CAstWrapper::__id;

#elif defined( _CODE_CFM )
#define _CAstControlFlowMap( __id, __type )						\
{									\
  jfieldID f##__id = env->GetStaticFieldID(CAstControlFlowMap, #__id, __type);	\
  THROW_ANY_EXCEPTION(exp);						\
  jobject o##__id = env->GetStaticObjectField(CAstControlFlowMap, f##__id);	\
  CAstWrapper::__id = env->NewGlobalRef(o##__id);			\
  THROW_ANY_EXCEPTION(exp);						\
}

#else 
#error "bad use of CAst control flow map"

#endif

_CAstControlFlowMap(SWITCH_DEFAULT, "Ljava/lang/Object;")
_CAstControlFlowMap(EXCEPTION_TO_EXIT, "Lcom/ibm/wala/cast/tree/CAstNode;")

#undef _CODE_CFM
#undef _CPP_CFM
#undef _INCLUDE_CFM 
#undef _CAstControlFlowMap

