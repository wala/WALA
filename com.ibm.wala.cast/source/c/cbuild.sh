#!/bin/bash

SCRIPT=`realpath $0`
C_DIR=`dirname $SCRIPT`
CAST_DIR=`realpath $C_DIR/../..`

pushd $CAST_DIR/source/c

cat > /tmp/JrePath.java <<EOF
class JrePath {
  public static void main(String[] args) {
    System.out.println(System.getProperty("java.home"));
  } 
}
EOF

pushd /tmp
javac JrePath.java
JRE_DIR=`java JrePath`
JDK_DIR=`realpath $JRE_DIR/..`
popd

JNI_MD_H=`ls $JDK_DIR/include/*/jni_md.h`
JNI_MD_PATH=`dirname $JNI_MD_H`
JNI_MD_DIR=`basename $JNI_MD_PATH`

cat > $CAST_DIR/source/c/Makefile.configuration <<EOF
JAVA_SDK = $JDK_DIR/
DOMO_AST_BIN = $CAST_DIR/target/classes/
JAVAH_CLASS_PATH = :$CAST_DIR/target/classes/
TRACE =
JNI_MD_DIR = $JNI_MD_DIR
EOF

if (uname | grep -i "cygwin"); then
	# This should be the default for most of cases;
	# adjust to your environment if necessary.
	MSVC="C:\Program Files\Microsoft Visual Studio 8\VC"
	ARCH=x86
	
	cmd.exe /c "call \"$MSVC\\vcvarsall.bat\" $ARCH && make"
else
	make
fi

popd
