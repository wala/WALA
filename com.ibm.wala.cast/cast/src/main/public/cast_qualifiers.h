#if defined( _INCLUDE_QUALIFIERS )
#define _CAstQualifier( __id )    static jobject __id;

#elif defined( _CPP_QUALIFIERS )
#define _CAstQualifier( __id )    jobject CAstWrapper::__id;

#elif defined( _CODE_QUALIFIERS )
#define _CAstQualifier( __id )						\
{									\
  jfieldID f##__id = env->GetStaticFieldID(CAstQualifier, #__id, "Lcom/ibm/wala/cast/tree/CAstQualifier;");	\
  jobject o##__id = env->GetStaticObjectField(CAstQualifier, f##__id);	\
  CAstWrapper::__id = env->NewGlobalRef(o##__id);			\
  THROW_ANY_EXCEPTION(exp);						\
}

#else 
#error "bad use of CAst qualifiers"

#endif

_CAstQualifier(STRICTFP)
_CAstQualifier(VOLATILE)
_CAstQualifier(ABSTRACT)
_CAstQualifier(INTERFACE)
_CAstQualifier(NATIVE)
_CAstQualifier(TRANSIENT)
_CAstQualifier(SYNCHRONIZED)
_CAstQualifier(FINAL)
_CAstQualifier(STATIC)
_CAstQualifier(PRIVATE)
_CAstQualifier(PROTECTED)
_CAstQualifier(PUBLIC)
_CAstQualifier(CONST)

#undef _CODE_QUALIFIERS
#undef _CPP_QUALIFIERS
#undef _INCLUDE_QUALIFIERS 
#undef _CAstQualifier

