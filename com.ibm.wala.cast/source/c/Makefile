
include Makefile.configuration

#
# in theory, these definitions should not need to be changed
#

ifeq (x$(PLATFORM),x)
  PLATFORM=$(shell uname -s)
endif

C_GENERATED=$(DOMO_AST_BIN)libcast/

JAVAH_GENERATED=$(C_GENERATED)
ifeq ($(PLATFORM),windows)
  CC=i686-mingw32-g++
else
  CC=g++
endif

vpath %.cpp	jni

JAVA_INCLUDES = -I$(JAVA_SDK)include

CAPA_INCLUDE_DIR = include/
CAPA_INCLUDES = -I$(CAPA_INCLUDE_DIR) -I$(C_GENERATED)

CAPA_JNI_BRIDGE_HEADER = $(C_GENERATED)com_ibm_wala_cast_ir_translator_NativeBridge.h
CAPA_JNI_XLATOR_HEADER = $(C_GENERATED)com_ibm_wala_cast_ir_translator_NativeTranslatorToCAst.h
CAPA_JNI_HEADERS = $(CAPA_JNI_BRIDGE_HEADER) $(CAPA_JNI_XLATOR_HEADER)

INCLUDES = $(CAPA_INCLUDES) $(JAVA_INCLUDES)

CAPA_SOURCES = $(notdir $(wildcard jni/*.cpp))
CAPA_OBJECTS = $(patsubst %.cpp,$(C_GENERATED)%.o,$(CAPA_SOURCES))

ifeq ($(PLATFORM),windows)
	ALL_FLAGS = -gstabs+ $(TRACE) $(INCLUDES) -DBUILD_CAST_DLL
	DLLEXT = dll
else
ifeq ($(PLATFORM),Darwin)
	ALL_FLAGS = -gstabs+ $(TRACE) $(INCLUDES) -fPIC
	DLLEXT = jnilib
else
	ALL_FLAGS = -pthread -gstabs+ $(TRACE) $(INCLUDES) -fPIC
	DLLEXT = so
endif
endif

ifeq ($(PLATFORM),windows)
	CC_LDFLAGS = -shared -Wl,--add-stdcall-alias -Wl,-export-all-symbols
	LIBPREFIX = 
else
ifeq ($(PLATFORM),Darwin)
	CC_LDFLAGS = -dynamiclib -Wl,-flat_namespace
	LIBPREFIX = lib
else
	CC_LDFLAGS = -pthread -shared
	LIBPREFIX = lib
endif
endif

#
# rules
#

default:	$(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT)

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

clean:
	rm -rf $(C_GENERATED) hs_err_pid*

