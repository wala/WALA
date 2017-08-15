/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.shrikeCT;

import java.io.IOException;

import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.sourcepos.CRTable;
import com.ibm.wala.sourcepos.MethodPositions;
import com.ibm.wala.sourcepos.Range;

public final class SourcePositionTableReader extends AttributeReader {

  protected SourcePositionTableReader(AttrIterator attr) throws InvalidClassFileException {
    super(attr, CRTable.ATTRIBUTE_NAME);
  }

  public static final class Position implements Comparable<Object> {
    
    public final int firstLine;
    public final int lastLine;
    public final int firstCol;
    public final int lastCol;
    
    private Position(int firstLine, int lastLine, int firstCol, int lastCol) {
      this.firstLine = firstLine;
      this.lastLine = lastLine;
      this.firstCol = firstCol;
      this.lastCol = lastCol;
    }
    
    @Override
    public int compareTo(Object o) {
      if (o instanceof Position) {
        Position p = (Position) o;
        if (firstLine != p.firstLine) {
          return firstLine - p.firstLine;
        } else if (firstCol != p.firstCol) {
          return firstCol - p.firstCol;
        } else if (lastLine != p.lastLine) {
          return lastLine - p.lastLine;
        } else if (lastCol != p.lastCol) {
          return lastCol - p.lastCol;
        } else {
          return 0;
        }
      } else {
        return -1;
      }
    } 
  }
  
  public static Position findParameterPosition(int methodNr, CodeReader code) throws InvalidClassFileException, IOException {
    if (code == null) {
      throw new IllegalArgumentException();
    }

    Position params = null;

    // read parameter positions
    {
      ClassReader.AttrIterator cIter = new ClassReader.AttrIterator();
      ClassReader cr = code.getClassReader();
      cr.initMethodAttributeIterator(methodNr, cIter);
      
      for (;cIter.isValid(); cIter.advance()) {
        if (MethodPositions.ATTRIBUTE_NAME.equals(cIter.getName())) {
          byte data[] = getData(cr, cIter.getRawOffset(), cIter.getRawSize());
          MethodPositions mPos = new MethodPositions(data);
          Range r = mPos.getMethodInfo();
          params = convert(r);
        }
      }
    }
  
    return params;
  }
  
  public static Position[] makeBytecodeToPositionMap(CodeReader code) throws InvalidClassFileException, IOException {
    if (code == null) {
      throw new IllegalArgumentException();
    }
    
    Position pos[] = null;
    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    code.initAttributeIterator(iter);
    
    for (; iter.isValid(); iter.advance()) {
      if (CRTable.ATTRIBUTE_NAME.equals(iter.getName())) {
        if (pos == null) {
          pos = new Position[code.getBytecodeLength()];
        }
        
        SourcePositionTableReader spRead = new SourcePositionTableReader(iter);
        spRead.fillBytecodeToPositionMap(pos);
      }
    }

    if (pos != null) {
      // fill in gaps
      Position last = new Position(0, 0, 0, 0);
      for (int i = 0; i < pos.length; i++) {
        Position cur = pos[i];
        if (cur == null) {
          pos[i] = last;
        } else {
          last = cur;
        }
      }
    }
    
    return pos;
  }

  private static final int ATTRIBUTE_HEADER_SIZE = 6;
  
  private static final byte[] getData(ClassReader cr, int rawOffset, int rawSize) {
    // prepare raw data of attribute to pass to sourceinfo
    byte klass[] = cr.getBytes();
    int size = rawSize - ATTRIBUTE_HEADER_SIZE;
    byte data[] = new byte[size];
    System.arraycopy(klass, rawOffset + ATTRIBUTE_HEADER_SIZE, data, 0, size);
    
    return data;
  }
  
  private void fillBytecodeToPositionMap(Position[] pos) throws IOException {
    byte tableData[] = getData(getClassReader(), getRawOffset(), getRawSize());
    
    CRTable crTable = new CRTable(tableData);
    
    for (int pc = 0; pc < pos.length; pc++) {
      Range r = crTable.getSourceInfo(pc);
      Position p = convert(r);
      pos[pc] = p;
    }
  }
  
  private final static Position convert(Range r) {
    Position pos = null;
    
    if (r != null) {
      com.ibm.wala.sourcepos.Position start = r.getStartPosition();
      com.ibm.wala.sourcepos.Position end = r.getEndPosition();
      
      if (start != null && !start.isUndefined()) {
        if (end != null && !end.isUndefined()) {
          pos = new Position(start.getLine(), end.getLine(), start.getColumn(), end.getColumn());
        } else {
          pos = new Position(start.getLine(), start.getLine(), -1, -1);
        }
      }
    }    

    return pos;
  }

  public static Position[] makeLineNumberToPositionMap(int[] lineNumberMap) {
    Position pos[] = new Position[lineNumberMap.length];
    
    for (int i = 0; i < pos.length; i++) {
      int line = lineNumberMap[i];
      pos[i] = new Position(line, line, -1, -1);
    }
    
    return pos;
  }

}
