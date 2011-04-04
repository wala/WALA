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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.StringTokenizer;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.functions.Function;

public class SourceRegion {

  private final StringBuilder source = new StringBuilder();
  private final MutableFileMapping fileMapping = new MutableFileMapping();
  private int currentLine = 1;
  
  public SourceRegion() {
  }

  public void print(String text, Function<Integer,Position> originalPos){
    source.append(text);
    int ln = 0;
    int numberOfLineDrops = getNumberOfLineDrops(text);
    if (originalPos != null){
      for (int i = 0; i < numberOfLineDrops; i++){
        fileMapping.map(currentLine++, originalPos.apply(ln++));
      }
      if (! text.endsWith("\n")){ // avoid mapping one line too much
        fileMapping.map(currentLine, originalPos.apply(ln)); // required for handling text with no CRs.
      }
    } else {
      currentLine += numberOfLineDrops;
    }
  }

  public void println(String text, Function<Integer,Position> originalPos){
    print(text + "\n", originalPos);
  }
  
  public void print(String text){
    print(text, null);
  }

  public void println(String text){
    print(text + "\n");
  }
  
  public FileMapping writeToFile(PrintStream ps){
    ps.print(source.toString());
    return fileMapping;
  }
  
  public void write(SourceRegion otherRegion){
    BufferedReader br = new BufferedReader(new StringReader(otherRegion.source.toString()));
    int lineNum = 0;
    String line;
    try {
      while ((line = br.readLine()) != null){
        lineNum++;
        
        Position fileAndLine = otherRegion.fileMapping.getAssociatedFileAndLine(lineNum);
        if (fileAndLine!= null){
          fileMapping.map(currentLine, fileAndLine);
        }
 
        this.println(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
      assert false;
    }
  }
  
  public void dump(PrintStream ps){
    StringTokenizer st = new StringTokenizer(source.toString(),"\n");
    int lineNum = 0;
    while (st.hasMoreElements()){
      String line = (String) st.nextElement();
      lineNum++;
      
      Position fileAndLine = fileMapping.getAssociatedFileAndLine(lineNum);
      if (fileAndLine!= null){
        ps.print(fileAndLine + "\t:");
      } else {
        ps.print("N/A \t\t:");
      }
      
      ps.println(line);
    }
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
