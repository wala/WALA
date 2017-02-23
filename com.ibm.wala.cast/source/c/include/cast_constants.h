#if defined( _INCLUDE_CONSTANTS )
#define _CAstNodeType( __id )    static jint __id;

#elif defined( _CPP_CONSTANTS )
#define _CAstNodeType( __id )    jint CAstWrapper::__id;

#elif defined( _CODE_CONSTANTS )
#define _CAstNodeType( __id )						\
{									\
  jfieldID f##__id = env->GetStaticFieldID(CAstNode, #__id, "I");	\
  CAstWrapper::__id = env->GetStaticIntField(CAstNode, f##__id);	\
  THROW_ANY_EXCEPTION(exp);						\
}

#else 
#error "bad use of CAst constants"

#endif

#if __WIN32__
#undef VOID
#undef CONST
#undef ERROR
#undef THIS
#endif

_CAstNodeType(ASSERT)
_CAstNodeType(SWITCH)
_CAstNodeType(LOOP)
_CAstNodeType(BLOCK_STMT)
_CAstNodeType(TRY)
_CAstNodeType(EXPR_STMT)
_CAstNodeType(DECL_STMT)
_CAstNodeType(RETURN)
_CAstNodeType(GOTO)
_CAstNodeType(BREAK)
_CAstNodeType(CONTINUE)
_CAstNodeType(IF_STMT)
_CAstNodeType(THROW)
_CAstNodeType(FUNCTION_STMT)
_CAstNodeType(ASSIGN)
_CAstNodeType(ASSIGN_PRE_OP)
_CAstNodeType(ASSIGN_POST_OP)
_CAstNodeType(LABEL_STMT)
_CAstNodeType(IFGOTO)
_CAstNodeType(EMPTY)
_CAstNodeType(RETURN_WITHOUT_BRANCH)
_CAstNodeType(CATCH)
_CAstNodeType(UNWIND)
_CAstNodeType(MONITOR_ENTER)
_CAstNodeType(MONITOR_EXIT)
_CAstNodeType(FUNCTION_EXPR)
_CAstNodeType(EXPR_LIST)
_CAstNodeType(CALL)
_CAstNodeType(GET_CAUGHT_EXCEPTION)
_CAstNodeType(BLOCK_EXPR)
_CAstNodeType(BINARY_EXPR)
_CAstNodeType(UNARY_EXPR)
_CAstNodeType(IF_EXPR)
_CAstNodeType(ANDOR_EXPR)
_CAstNodeType(NEW)
_CAstNodeType(OBJECT_LITERAL)
_CAstNodeType(VAR)
_CAstNodeType(OBJECT_REF)
_CAstNodeType(CHOICE_EXPR)
_CAstNodeType(CHOICE_CASE)
_CAstNodeType(SUPER)
_CAstNodeType(THIS)
_CAstNodeType(ARRAY_LITERAL)
_CAstNodeType(CAST)
_CAstNodeType(INSTANCEOF)
_CAstNodeType(ARRAY_REF)
_CAstNodeType(ARRAY_LENGTH)
_CAstNodeType(TYPE_OF)
_CAstNodeType(LOCAL_SCOPE)
_CAstNodeType(CONSTANT)
_CAstNodeType(OPERATOR)
_CAstNodeType(PRIMITIVE)
_CAstNodeType(ERROR)
_CAstNodeType(VOID) 
_CAstNodeType(ECHO) 
_CAstNodeType(EACH_ELEMENT_GET) 
_CAstNodeType(EACH_ELEMENT_HAS_NEXT) 
_CAstNodeType(LIST_EXPR);
_CAstNodeType(EMPTY_LIST_EXPR);
_CAstNodeType(IS_DEFINED_EXPR);
_CAstNodeType(INCLUDE)
_CAstNodeType(NAMED_ENTITY_REF);
_CAstNodeType(MACRO_VAR);

#undef _CODE_CONSTANTS
#undef _CPP_CONSTANTS
#undef _INCLUDE_CONSTANTS 
#undef _CAstNodeType

