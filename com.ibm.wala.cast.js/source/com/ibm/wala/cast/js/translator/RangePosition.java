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
package com.ibm.wala.cast.js.translator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

public class RangePosition extends AbstractSourcePosition implements Position {
  private final URL url;
  private final int line;
  private final int startOffset;
  private final int endOffset;
  
  
  public RangePosition(URL url, int line, int startOffset, int endOffset) {
    super();
    this.url = url;
    this.line = line;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
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
    return line;
  }

  @Override
  public int getLastLine() {
    return -1;
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
