package com.ibm.wala.cast.js.html;

import java.io.PrintStream;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class EmptyFileMapping implements FileMapping {

  @Override
  public IncludedPosition getIncludedPosition(Position line) {
    return null;
  }

  @Override
  public void dump(PrintStream ps) {
    ps.println("empty mapping");
  }

}
