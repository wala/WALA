package com.ibm.wala.cast.js.html;

import java.io.PrintStream;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public interface FileMapping {

  /**
   * @param line
   * @return Null if no mapping for the given line.
   */
  public abstract IncludedPosition getIncludedPosition(Position line);

  public abstract void dump(PrintStream ps);

}