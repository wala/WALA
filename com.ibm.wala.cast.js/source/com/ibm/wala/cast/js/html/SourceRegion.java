/******************************************************************************
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.html;

import java.io.PrintStream;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class SourceRegion {

  private final StringBuilder source = new StringBuilder();
  private FileMapping fileMapping;
  private int currentLine = 1;
  
  public SourceRegion() {
  }

  public void print(String text, Position originalPos, URL url){
    int startOffset = source.length();
    source.append(text);
    int endOffset = source.length();

    int numberOfLineDrops = getNumberOfLineDrops(text);

    if (originalPos != null) {
      RangeFileMapping map = new RangeFileMapping(startOffset, endOffset, currentLine, currentLine+numberOfLineDrops, originalPos, url);
      if (fileMapping == null) {
        fileMapping = map;
      } else {
        fileMapping = new CompositeFileMapping(map, fileMapping);
      }
    }
    
    currentLine += numberOfLineDrops;
  }

  public void println(String text, Position originalPos, URL url){
    print(text + "\n", originalPos, url);
  }
  
  public void print(String text){
    print(text, null, null);
  }

  public void println(String text){
    print(text + "\n");
  }
  
  public FileMapping writeToFile(PrintStream ps){
    ps.print(source.toString());
    return fileMapping;
  }
  
  public void write(SourceRegion otherRegion){
    int rangeStart = source.length();
    String text = otherRegion.source.toString();
    source.append(text);
    int rangeEnd = source.length();

    int numberOfLineDrops = getNumberOfLineDrops(text);

    if (otherRegion.fileMapping != null) {
      FileMapping map = new NestedRangeMapping(rangeStart, rangeEnd, currentLine, currentLine+numberOfLineDrops, otherRegion.fileMapping);
      if (fileMapping == null) {
        fileMapping = map;
      } else {
        fileMapping = new CompositeFileMapping(map, fileMapping);
      }
    }

    currentLine += numberOfLineDrops;
  }
  
  public void dump(PrintStream ps){
    ps.println(source.toString());
  }
  
  private static int getNumberOfLineDrops(String text) {
    int ret = 0;
    int i = text.indexOf('\n');
    while (i != -1){
      ret++;
      if (i < text.length()-1){
        i = text.indexOf('\n', i + 1);
      } else {
        break; // CR was the the last character.
      }
    }
    return ret;
  }
}
