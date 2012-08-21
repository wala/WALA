package com.ibm.wala.cast.js.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

import com.ibm.wala.cast.js.html.RangeFileMapping.Range;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

public class NestedRangeMapping implements FileMapping {
  private final Range range;
  private final FileMapping innerMapping;
  
  public NestedRangeMapping(int rangeStart, int rangeEnd, int rangeStartingLine,  int rangeEndingLine, FileMapping innerMapping) {
    assert innerMapping != null;
    this.range = new Range(rangeStart, rangeEnd, rangeStartingLine, rangeEndingLine);
    this.innerMapping = innerMapping;
  }

  @Override
  public IncludedPosition getIncludedPosition(final Position pos) {
    if (range.includes(pos)) {
      return innerMapping.getIncludedPosition(
          new AbstractSourcePosition() {
            @Override
            public int getFirstLine() {
              return pos.getFirstLine()-range.getStartingLine()+1;
            }

            @Override
            public int getLastLine() {
              return pos.getLastLine()==-1? -1: (pos.getLastLine()-range.getStartingLine()+1);
            }

            @Override
            public int getFirstCol() {
               return pos.getFirstCol();
            }

            @Override
            public int getLastCol() {
              return pos.getLastCol();
            }

            @Override
            public int getFirstOffset() {
              return pos.getFirstOffset()==-1? -1: (pos.getFirstOffset()-range.getStart());
            }

            @Override
            public int getLastOffset() {
              return pos.getLastOffset()==-1? -1: (pos.getLastOffset()-range.getStart());
            }

            @Override
            public URL getURL() {
              return pos.getURL();
            }

            @Override
            public InputStream getInputStream() throws IOException {
             return pos.getInputStream();
            }
          });
    } else {
      return null;
    }
  }

  @Override
  public void dump(PrintStream ps) {
    // TODO Auto-generated method stub

  }

  public String toString() {
    return range + "(" + innerMapping + ")";
  }
}
