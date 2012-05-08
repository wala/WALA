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
import java.io.InputStream;
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

  public int compareTo(Object o) {
    Position other = (Position) o;
    if (startOffset != other.getFirstOffset()) {
      return startOffset - other.getFirstOffset();
    } else {
      return endOffset - other.getLastOffset();
    }
  }

  public int getFirstLine() {
    return line;
  }

  public int getLastLine() {
    return -1;
  }

  public int getFirstCol() {
    return -1;
  }

  public int getLastCol() {
    return -1;
  }

  public int getFirstOffset() {
    return startOffset;
  }

  public int getLastOffset() {
    return endOffset;
  }

  public URL getURL() {
    return url;
  }

  public InputStream getInputStream() throws IOException {
    return url.openStream();
  }
}
