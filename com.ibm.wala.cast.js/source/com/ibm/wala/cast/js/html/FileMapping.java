package com.ibm.wala.cast.js.html;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * Maps line numbers to lines of other files (fileName + line).
 */
public class FileMapping{
  protected Map<Integer, Pair<String, Integer>> lineNumberToFileAndLine = HashMapFactory.make(); 
  
  /**
   * @param line
   * @return Null if no mapping for the given line.
   */
  public Pair<String,Integer> getAssociatedFileAndLine(int line){
    return lineNumberToFileAndLine.get(line);
  }
  
  public void dump(PrintStream ps){
    Set<Integer> lines = new TreeSet<Integer>(lineNumberToFileAndLine.keySet());
    for (Integer line : lines){
      Pair<String, Integer> fnAndln = lineNumberToFileAndLine.get(line);
      ps.println(line + ": " + fnAndln.snd + "@" + fnAndln.fst);
    }
  }
}