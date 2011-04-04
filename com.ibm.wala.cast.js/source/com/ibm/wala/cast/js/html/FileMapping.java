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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * Maps line numbers to lines of other files (fileName + line).
 */
public class FileMapping{
  protected Map<Integer, Position> lineNumberToFileAndLine = HashMapFactory.make(); 
  
  /**
   * @param line
   * @return Null if no mapping for the given line.
   */
  public Position getAssociatedFileAndLine(int line){
    return lineNumberToFileAndLine.get(line);
  }
  
  public void dump(PrintStream ps){
    Set<Integer> lines = new TreeSet<Integer>(lineNumberToFileAndLine.keySet());
    for (Integer line : lines){
      Position fnAndln = lineNumberToFileAndLine.get(line);
      ps.println(line + ": " + fnAndln);
    }
  }
}