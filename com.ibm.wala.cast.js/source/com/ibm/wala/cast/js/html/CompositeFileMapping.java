package com.ibm.wala.cast.js.html;

import java.io.PrintStream;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class CompositeFileMapping implements FileMapping {
  private final FileMapping a;
  private final FileMapping b;
    
  public CompositeFileMapping(FileMapping a, FileMapping b) {
    this.a = a;
    this.b = b;
  }

  @Override
  public IncludedPosition getIncludedPosition(Position line) {
    IncludedPosition p = a.getIncludedPosition(line);
    if (p != null) {
      return p;
    } else {
      return b.getIncludedPosition(line); 
    }
  }

  @Override
  public void dump(PrintStream ps) {
    // TODO Auto-generated method stub

  }

  public String toString() {
    return a + "," + b;
  }
}
