#!/bin/bash

SCRIPT_NAME=`realpath $0`
C_DIR=`dirname $SCRIPT_NAME`
CAST_TEST_DIR=`realpath $C_DIR/../..`

pushd $CAST_TEST_DIR/harness-src/c

cat > $CAST_TEST_DIR/harness-src/c/Makefile.configuration <<EOF
CAST_TEST_BIN = $CAST_TEST_DIR/target/classes/
EOF

make

make main

popd
