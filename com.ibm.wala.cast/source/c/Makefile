
include Makefile.configuration

include Makefile.definitions

#
# rules
#

default:	$(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT) $(DOMO_AST_BIN)$(LIBPREFIX)cast.a

bindir:
	mkdir -p $(C_GENERATED)

$(CAPA_JNI_BRIDGE_HEADER):	$(DOMO_AST_BIN)com/ibm/wala/cast/ir/translator/NativeBridge.class bindir
	$(JAVA_SDK)bin/javah -classpath "$(DOMO_AST_BIN)$(JAVAH_CLASS_PATH)" -d "$(JAVAH_GENERATED)" com.ibm.wala.cast.ir.translator.NativeBridge

$(CAPA_JNI_XLATOR_HEADER):	$(DOMO_AST_BIN)com/ibm/wala/cast/ir/translator/NativeTranslatorToCAst.class bindir
	$(JAVA_SDK)bin/javah -classpath "$(DOMO_AST_BIN)$(JAVAH_CLASS_PATH)" -d "$(JAVAH_GENERATED)" com.ibm.wala.cast.ir.translator.NativeTranslatorToCAst

$(CAPA_OBJECTS): $(C_GENERATED)%.o:	%.cpp $(CAPA_JNI_HEADERS) bindir
	echo $(CAPA_OBJECTS)
	$(CC) $(ALL_FLAGS) -o $@ -c $<

$(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT):	$(CAPA_OBJECTS)
	$(CC) $(CC_LDFLAGS) $^ -o $@

$(DOMO_AST_BIN)$(LIBPREFIX)cast.a:	$(CAPA_OBJECTS)
	ar -r $@ $^

clean:
	rm -rf $(C_GENERATED) hs_err_pid*

