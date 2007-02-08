#!/bin/bash

REAL_NAME=`realpath $0`
MY_DIR=`dirname $REAL_NAME`

TMP=$MY_DIR/tmp

if [[ ! -e $MY_DIR/polyglot.jar ]]; then
  mkdir -p $TMP

  cd $TMP

  wget -O polyglot-2.0.2-src.tar.gz http://www.cs.cornell.edu/Projects/polyglot/src/polyglot-2.0.2-src.tar.gz

  tar xzf polyglot-2.0.2-src.tar.gz

  cd polyglot-2.0.2-src

  ant jar

  cp lib/polyglot.jar $MY_DIR/polyglot.jar
  cp lib/java_cup.jar $MY_DIR/java_cup.jar

  cd $MY_DIR
  rm -rf $TMP
fi

