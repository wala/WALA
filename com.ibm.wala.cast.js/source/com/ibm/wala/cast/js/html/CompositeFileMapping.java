package com.ibm.wala.cast.js.html;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class CompositeFileMapping implements FileMapping {
  private final List<FileMapping> mappings = new ArrayList<FileMapping>(2);
    
  public CompositeFileMapping(FileMapping a, FileMapping b) {
    addMapping(a);
    addMapping(b);    
  }

  private void addMapping(FileMapping fm) {
    if (fm instanceof CompositeFileMapping) {
      mappings.addAll(((CompositeFileMapping)fm).mappings);
    } else {
      mappings.add(fm);
    }
  }
  @Override
  public IncludedPosition getIncludedPosition(Position line) {
    for (FileMapping fm : mappings) {
      IncludedPosition result = fm.getIncludedPosition(line);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public void dump(PrintStream ps) {
    // TODO Auto-generated method stub

  }

  public String toString() {
    return mappings.toString();
  }
}
