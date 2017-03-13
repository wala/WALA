/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

public class SourceBuffer {
  private String[] lines;
  private final Position p;

  public SourceBuffer(Position p) throws IOException {
    this.p = p;

    BufferedReader reader = new BufferedReader(p.getReader());
    
    String currentLine = null;
    List<String> lines = new ArrayList<>();
    int offset = 0, line = 0;
    do { 
      currentLine = reader.readLine();
      if (currentLine == null) {
        this.lines = new String[0];
        return;
      }
      offset += (currentLine.length() + 1);
      line++;
    } while (p.getLastOffset()>=0? p.getFirstOffset() > offset: p.getFirstLine() > line);
    
    // partial first line
    if (p.getLastOffset() >= 0) {
      if (p.getFirstOffset() == offset) {
        lines.add("\n");
      } else {
        int startOffset = p.getFirstOffset() - (offset-currentLine.length()-1);
        if (offset > p.getLastOffset()) {
          int endOffset = p.getLastOffset() - (offset-currentLine.length()-1);
          lines.add(currentLine.substring(startOffset, endOffset));
        } else {
          lines.add(currentLine.substring(startOffset));
        }
      }
    } else {
      lines.add(currentLine.substring(p.getFirstCol()));
    }
    
    while (p.getLastOffset()>=0? p.getLastOffset() >= offset: p.getLastLine() >= line) {
      currentLine = reader.readLine();
      
      if (currentLine == null) {
        offset = p.getLastOffset();
        break;
      } else {
        offset += currentLine.length() + 1;
      }
      line++;
      if (p.getLastOffset() >= 0) {
        if (offset > p.getLastOffset()) {
          lines.add(currentLine.substring(0, currentLine.length() - (offset - p.getLastOffset()) + 1));
        } else {
          lines.add(currentLine);
        }
      } else {
        if (p.getLastLine() == line) {
          lines.add(currentLine.substring(0, p.getLastCol()));
        } else {
          lines.add(currentLine);
        }
      }
    }

    this.lines = lines.toArray(new String[ lines.size() ]);
  }
  
     
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for(int i = 0; i < lines.length; i++) {
      if (i == lines.length - 1) {
        result.append(lines[i]);
      } else {
        result.append(lines[i]).append("\n");
      }
    }
    
    return result.toString();
  }

  public void substitute(Position range, String newText) {
    int startLine = range.getFirstLine() - p.getFirstLine();
    int endLine = range.getLastLine() - p.getFirstLine();

    if (startLine != endLine) {
      String newLines[] = new String[ lines.length - (endLine-startLine) ];
      int i = 0;
      while (i < startLine) {
	newLines[i] = lines[i];
	i++;
      }
      newLines[i++] = 
	lines[startLine].substring(0, range.getFirstCol()) +
	lines[endLine].substring(range.getLastCol());
      while(i < newLines.length) {
	newLines[i] = lines[i+ (endLine-startLine)];
	i++;
      }
      
      lines = newLines;
      endLine = startLine;

      final Position hack = range;
      range = new AbstractSourcePosition() {
	@Override
  public int getFirstLine() { return hack.getFirstLine(); }
	@Override
  public int getLastLine() { return hack.getFirstLine(); }
	@Override
  public int getFirstCol() { return hack.getFirstCol(); }
	@Override
  public int getLastCol() { return hack.getFirstCol(); }
	@Override
  public int getFirstOffset() { return hack.getFirstOffset(); }
	@Override
  public int getLastOffset() { return hack.getFirstOffset(); }
	@Override
  public URL getURL() { return hack.getURL(); }
	@Override
  public Reader getReader() throws IOException { 
	  return hack.getReader();
	}
      };
    }
    
    String[] newTextLines = newText.split("\n");

    if (newTextLines.length == 1) {
      lines[startLine] = 
	lines[startLine].substring(0, range.getFirstCol()) +
	newTextLines[0] +
	lines[startLine].substring(range.getLastCol()+1);
    } else {
      String[] newLines =
	new String[ lines.length + newTextLines.length - 1 ];
      int i = 0;
      while (i < startLine) {
	newLines[i] = lines[i];
	i++;
      }
      
      newLines[i++] = 	  
        lines[startLine].substring(0, range.getFirstCol()) +
	newTextLines[0];

      for(int j = 1; j < newTextLines.length - 1; j++) {
	lines[i++] = newTextLines[j];
      }

      newLines[i++] =
	newTextLines[newTextLines.length-1] +
	lines[endLine].substring(range.getLastCol()+1);

      while (i < newLines.length) {
	newLines[i] = lines[i - newTextLines.length + 1];
	i++;
      }
      
      lines = newLines;
    }
  }
}


