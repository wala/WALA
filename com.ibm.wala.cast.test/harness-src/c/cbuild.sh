#!/bin/bash

<<<<<<< HEAD
C_DIR=`realpath $0`
CAST_TEST_DIR=`realpath $C_DIR/../../..`
=======
SCRIPT_NAME=`realpath $0`
C_DIR=`dirname $SCRIPT_NAME`
CAST_TEST_DIR=`realpath $C_DIR/../..`
>>>>>>> f3a38f50d825bd4e2fa6a5fb230a9664fce831c5

pushd $CAST_TEST_DIR/harness-src/c

cat > $CAST_TEST_DIR/harness-src/c/Makefile.configuration <<EOF
CAST_TEST_BIN = $CAST_TEST_DIR/target/classes/
EOF

make

make main

popd
