#!/bin/bash

if [[ `uname` = "Linux" ]]; then
  REAL_NAME=`realpath $0`
  MY_DIR=`dirname $REAL_NAME`
else 
  MY_DIR=`pwd`
fi

TMP=$MY_DIR/tmp

if [[ ! -e $MY_DIR/JLex/Main.java ]]; then
  mkdir -p $TMP

  cd $TMP

  mkdir JLex
  wget -O JLex/Main.java http://www.cs.princeton.edu/~appel/modern/java/JLex/current/Main.java

  cp -r JLex $MY_DIR

  cd $MY_DIR
  rm -rf $TMP
fi

