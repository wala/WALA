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

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.*;

import java.io.*;
import java.net.*;

public class SourceBuffer {
  private String[] lines;
  private final Position p;

  public SourceBuffer(Position p) throws IOException {
    this.p = p;
    this.lines = new String[ p.getLastLine() - p.getFirstLine() + 1];
    BufferedReader r =
      new BufferedReader(new InputStreamReader(p.getInputStream()));
    int line = 1;
    while (line <= p.getLastLine()) {
      String theLine = r.readLine();
      if (line >= p.getFirstLine()) {
	lines[line-p.getFirstLine()] = 
	   line == p.getLastLine()?
	   theLine.substring(0, p.getLastCol()+1):
	   theLine;
      }
      line++;
    }
  }
    
  public String toString() {
    StringBuffer result = new StringBuffer();
    for(int i = 0; i < lines.length; i++) {
      if (i == 0) {
        result.append(lines[i].substring(p.getFirstCol())).append("\n");
      } else if (i == lines.length - 1) {
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
	public int getFirstLine() { return hack.getFirstLine(); }
	public int getLastLine() { return hack.getFirstLine(); }
	public int getFirstCol() { return hack.getFirstCol(); }
	public int getLastCol() { return hack.getFirstCol(); }
	public URL getURL() { return hack.getURL(); }
	public InputStream getInputStream() throws IOException { 
	  return hack.getInputStream();
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


