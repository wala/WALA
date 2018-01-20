
include Makefile.configuration

include ../../../com.ibm.wala.cast/source/c/Makefile.configuration

include ../../../com.ibm.wala.cast/source/c/Makefile.definitions

TEST_JNI_BRIDGE_HEADER = $(C_GENERATED)com_ibm_wala_cast_test_TestNativeTranslator.h

CAST_OBJS = $(wildcard $(C_GENERATED)/*.o)

#
# rules
#

default:	$(CAST_TEST_BIN)$(LIBPREFIX)xlator_test.$(DLLEXT)

$(TEST_JNI_BRIDGE_HEADER):	$(CAST_TEST_BIN)com/ibm/wala/cast/test/TestNativeTranslator.class
	$(JAVA_HOME)/bin/javah -classpath "$(CAST_TEST_BIN)$(JAVAH_CLASS_PATH)" -d "$(JAVAH_GENERATED)" com.ibm.wala.cast.test.TestNativeTranslator

$(C_GENERATED)/smoke.o:	$(TEST_JNI_BRIDGE_HEADER) smoke.cpp
	$(CC) $(ALL_FLAGS) -o $@ -c smoke.cpp

$(C_GENERATED)/smoke_main.o:	$(TEST_JNI_BRIDGE_HEADER) smoke_main.cpp
	$(CC) $(ALL_FLAGS) -o $@ -c smoke_main.cpp

$(CAST_TEST_BIN)$(LIBPREFIX)xlator_test.$(DLLEXT):	$(C_GENERATED)/smoke.o
	$(CC) $(CC_LDFLAGS) -Wl,-rpath -Wl,$(DOMO_AST_BIN) $(CAST_OBJS) -o $@

main:	$(C_GENERATED)/smoke_main

$(C_GENERATED)/smoke_main:	$(TEST_JNI_BRIDGE_HEADER) $(DOMO_AST_BIN)/$(LIBPREFIX)cast/launch.o $(C_GENERATED)/smoke_main.o
	$(CC) $(ALL_FLAGS) $(DOMO_AST_BIN)/$(LIBPREFIX)cast/launch.o $(C_GENERATED)/smoke_main.o $(CC_LD_PATHS) -o $@ 

