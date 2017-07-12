/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.html;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

public class RangeFileMapping implements FileMapping {

  public final static class Range {
    private final int rangeStart;
    private final int rangeEnd;
    private final int rangeStartingLine;
    private final int rangeEndingLine;

    public boolean includes(Position offset) {
      return 
        offset.getFirstOffset() != -1? 
        rangeStart <= offset.getFirstOffset() && offset.getLastOffset() <= rangeEnd:
        rangeStartingLine <= offset.getFirstLine() && 
          (offset.getLastLine() == -1? offset.getFirstLine(): offset.getLastLine()) <= rangeEndingLine;
    }

    public Range(int rangeStart, int rangeEnd, int rangeStartingLine, int rangeEndingLine) {
      super();
      this.rangeStart = rangeStart;
      this.rangeEnd = rangeEnd;
      this.rangeStartingLine = rangeStartingLine;
      this.rangeEndingLine = rangeEndingLine;
    }

    public int getStart() {
      return rangeStart;
    }

    public int getEnd() {
      return rangeEnd;
    }

    public int getStartingLine() {
      return rangeStartingLine;
    }

    public int getEndingLine() {
      return rangeEndingLine;
    }
     
    @Override
    public String toString() {
      return "{"+rangeStart+"->"+rangeEnd+"}";
    }
  }

  private final Range range;
  private final URL includedURL;
  private final Position includePosition;
    
  public RangeFileMapping(int rangeStart, int rangeEnd, int rangeStartingLine, int rangeEndingLine, Position parentPosition, URL url) {
    assert parentPosition != null;
    this.range = new Range(rangeStart, rangeEnd, rangeStartingLine, rangeEndingLine);
    this.includePosition = parentPosition;
    includedURL = url;
  }

  @Override
  public String toString() {
    return range + ":" + includePosition;
  }
  
  @Override
  public IncludedPosition getIncludedPosition(final Position offset) {
    if (range.includes(offset)) {
      class Pos extends AbstractSourcePosition implements IncludedPosition {
        @Override
        public int getFirstLine() {
          // line numbers are decreed to start at 1, rather than 0
          return offset.getFirstLine() - range.getStartingLine() + 1;
        }
        @Override
        public int getLastLine() {
          return offset.getLastLine() == -1? -1: offset.getLastLine()-range.getStartingLine()+1;
        }
        @Override
        public int getFirstCol() {
          return offset.getFirstCol();
        }
        @Override
        public int getLastCol() {
          return offset.getLastCol();
        }
        @Override
        public int getFirstOffset() {
          return offset.getFirstOffset() == -1? -1: offset.getFirstOffset()-range.getStart();
        }
        @Override
        public int getLastOffset() {
          return offset.getLastOffset() == -1? -1: offset.getLastOffset()-range.getStart();
        }
        @Override
        public URL getURL() {
          return includedURL;
        }
        @Override
        public Reader getReader() throws IOException {
          return RangeFileMapping.this.getInputStream();
         }
        @Override
        public Position getIncludePosition() {
          return includePosition;
        }
        @Override
        public String toString() {
          return "[include:"+includePosition+"]"+super.toString();
        }
      }
      
      return new Pos();
    } else {
      return null;
    }
  }

   public Reader getInputStream() throws IOException {
     return new InputStreamReader(includedURL.openStream());
  }

}
