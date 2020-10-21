#if defined( _INCLUDE_OPERATORS )
#define _CAstOperator( __id )    static jobject __id;

#elif defined( _CPP_OPERATORS )
#define _CAstOperator( __id )    jobject CAstWrapper::__id;

#elif defined( _CODE_OPERATORS )
#define _CAstOperator( __id )						\
{									\
  jfieldID f##__id = env->GetStaticFieldID(CAstOperator, #__id, "Lcom/ibm/wala/cast/tree/impl/CAstOperator;");	\
  THROW_ANY_EXCEPTION(exp);						\
  jobject o##__id = env->GetStaticObjectField(CAstOperator, f##__id);	\
  CAstWrapper::__id = env->NewGlobalRef(o##__id);			\
  THROW_ANY_EXCEPTION(exp);						\
}

#else 
#error "bad use of CAst operators"

#endif

_CAstOperator(OP_ADD)
_CAstOperator(OP_CONCAT)
_CAstOperator(OP_DIV)
_CAstOperator(OP_LSH)
_CAstOperator(OP_MOD)
_CAstOperator(OP_MUL)
_CAstOperator(OP_RSH)
_CAstOperator(OP_URSH)
_CAstOperator(OP_SUB)
_CAstOperator(OP_EQ)
_CAstOperator(OP_GE)
_CAstOperator(OP_GT)
_CAstOperator(OP_LE)
_CAstOperator(OP_LT)
_CAstOperator(OP_NE)
_CAstOperator(OP_NOT)
_CAstOperator(OP_BITNOT)
_CAstOperator(OP_BIT_AND)
_CAstOperator(OP_REL_AND)
_CAstOperator(OP_BIT_OR)
_CAstOperator(OP_REL_OR)
_CAstOperator(OP_BIT_XOR)
_CAstOperator(OP_REL_XOR)

#undef _CODE_OPERATORS
#undef _CPP_OPERATORS
#undef _INCLUDE_OPERATORS 
#undef _CAstOperator

