package com.ibm.wala.cast.js.html;

import com.ibm.wala.util.collections.Pair;

public class MutableFileMapping extends FileMapping {

  void map(int line, String originalFile, int originalLine){
    lineNumberToFileAndLine.put(line, Pair.<String, Integer> make(originalFile, originalLine));
  }
  
}
