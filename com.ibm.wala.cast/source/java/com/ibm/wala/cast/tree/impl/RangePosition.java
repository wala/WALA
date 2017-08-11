/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.tree.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class RangePosition extends AbstractSourcePosition {
  private final URL url;
  private final int startLine;
  private final int endLine;
  private final int startOffset;
  private final int endOffset;
  
  public RangePosition(URL url, int startLine, int endLine, int startOffset, int endOffset) {
    super();
    this.url = url;
    this.startLine = startLine;
    this.endLine = endLine;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public RangePosition(URL url, int line, int startOffset, int endOffset) {  
    this(url, line, -1, startOffset, endOffset);
  }
  
  @Override
  public int compareTo(Object o) {
    Position other = (Position) o;
    if (startOffset != other.getFirstOffset()) {
      return startOffset - other.getFirstOffset();
    } else {
      return endOffset - other.getLastOffset();
    }
  }

  @Override
  public int getFirstLine() {
    return startLine;
  }

  @Override
  public int getLastLine() {
    return endLine;
  }

  @Override
  public int getFirstCol() {
    return -1;
  }

  @Override
  public int getLastCol() {
    return -1;
  }

  @Override
  public int getFirstOffset() {
    return startOffset;
  }

  @Override
  public int getLastOffset() {
    return endOffset;
  }

  @Override
  public URL getURL() {
    return url;
  }

  @Override
  public Reader getReader() throws IOException {
    return new InputStreamReader(url.openStream());
  }
}
