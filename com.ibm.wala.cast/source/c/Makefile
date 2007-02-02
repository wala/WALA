# 2006/08/03 Naoshi Tabuchi: tabee@jp.ibm.com
# Adjusted to building with MSVC++.
# NOTE: PLATFORM=cygwin now implies using MSVC,
# thus pathnames must be given in "Windows style,"
# e.g. "c:/path/to/something", path separators can be
# slashes, though.

include Makefile.configuration

#
# in theory, these definitions should not need to be changed
#

# 2006/08/03 Naoshi Tabuchi: tabee@jp.ibm.com
# Recent versions of Cygwin's uname return "Cygwin", instead of "cygwin."
PLATFORM=$(shell uname -o | sed -e 's/C/c/')

C_GENERATED=$(DOMO_AST_BIN)libcast/

ifeq ($(PLATFORM),cygwin)
  JAVAH_GENERATED=$(shell cygpath -w $(DOMO_AST_BIN)/libcast)
  CC=cl
else
  JAVAH_GENERATED=$(C_GENERATED)
  CC=g++
endif

vpath %.cpp	jni

ifeq ($(PLATFORM),cygwin)
  PLATFORM_JNI_DIR = win32
else
  PLATFORM_JNI_DIR = $(shell cd $(JAVA_SDK); find include -mindepth 1 -maxdepth 1 -type d)
endif
JAVA_INCLUDES = -I$(JAVA_SDK)include -I$(JAVA_SDK)$(PLATFORM_JNI_DIR) -I$(JAVA_SDK)include/$(PLATFORM_JNI_DIR)

CAPA_INCLUDE_DIR = include/
CAPA_INCLUDES = -I$(CAPA_INCLUDE_DIR) -I$(C_GENERATED)

CAPA_JNI_HEADER = $(C_GENERATED)com_ibm_wala_cast_ir_translator_NativeBridge.h

INCLUDES = $(CAPA_INCLUDES) $(JAVA_INCLUDES)

CAPA_SOURCES = $(notdir $(wildcard jni/*.cpp))
CAPA_OBJECTS = $(patsubst %.cpp,$(C_GENERATED)%.o,$(CAPA_SOURCES))

ifeq ($(PLATFORM),cygwin)
	ALL_FLAGS = /WL /MD /EHsc /D ZTS /D YY_NO_UNISTD_H /D _USE_32BIT_TIME_T /D __WIN32__ /D WIN32 /D BUILD_CAST_DLL $(INCLUDES)
	DLLEXT = dll
else
	ALL_FLAGS = -pthread -gstabs+ $(TRACE) $(INCLUDES) -fpic
	DLLEXT = so
endif

ifeq ($(PLATFORM),cygwin)
	CC_OUTFLAG = /Fo
	LD_OUTFLAG = /link /IMPLIB:$(DOMO_AST_BIN)cast.lib /OUT:
	CC_LDFLAGS = /LDd /MD 
	LIBPREFIX = 
	POSTPROCESS = && cd $(DOMO_AST_BIN) && mt /manifest $(LIBPREFIX)cast.$(DLLEXT).manifest /outputresource:"$(LIBPREFIX)cast.$(DLLEXT);\#2"
else
	CC_OUTFLAG = -o
	CC_LDFLAGS = -pthread -shared
	LD_OUTFLAG = -o
	LIBPREFIX = lib
	POSTPROCESS =
endif

#
# rules
#

default:	$(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT)

bindir:
	mkdir -p $(C_GENERATED)

$(CAPA_JNI_HEADER):	$(DOMO_AST_BIN)com/ibm/wala/cast/ir/translator/NativeBridge.class bindir
	$(JAVA_SDK)bin/javah -classpath "$(DOMO_AST_BIN)$(JAVAH_CLASS_PATH)" -d "$(JAVAH_GENERATED)" com.ibm.wala.cast.ir.translator.NativeBridge

$(CAPA_OBJECTS): $(C_GENERATED)%.o:	%.cpp $(CAPA_JNI_HEADER) bindir
	echo $(CAPA_OBJECTS)
	$(CC) $(ALL_FLAGS) $(CC_OUTFLAG)$@ -c $<

$(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT):	$(CAPA_OBJECTS)
	$(CC) $(CC_LDFLAGS) $^ $(LD_OUTFLAG)$@ $(POSTPROCESS)

clean:
	rm -rf $(DOMO_AST_BIN)$(LIBPREFIX)cast.$(DLLEXT) $(C_GENERATED) hs_err_pid*

