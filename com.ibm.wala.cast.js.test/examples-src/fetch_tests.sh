#!/bin/bash

if [[ `uname` = "Linux" ]]; then
  REAL_NAME=`realpath $0`
  MY_DIR=`dirname $REAL_NAME`
else 
  MY_DIR=`pwd`
fi

TMP=$MY_DIR/tmp

if [[ ! -e $MY_DIR/ajaxslt ]]; then
  mkdir -p $TMP

  cd $TMP

  wget -O ajaxslt-0-7.tar.gz http://ajaxslt.googlecode.com/files/ajaxslt-0-7.tar.gz

  tar xzf ajaxslt-0-7.tar.gz

  mv ajaxslt $MY_DIR/ajaxslt

  cd $MY_DIR
  rm -rf $TMP
fi

