/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class LineNumberPosition extends AbstractSourcePosition {
  private final URL url;
  private final URL localFile;
  private final int lineNumber;

  public LineNumberPosition(URL url, URL localFile, int lineNumber) {
    this.url = url;
    this.localFile = localFile;
    this.lineNumber = lineNumber;
  }

  @Override
  public int getFirstLine() {
    return lineNumber;
  }

  @Override
  public int getLastLine() {
    return lineNumber;
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
    return -1;
  }

  @Override
  public int getLastOffset() {
    return -1;
  }

  @Override
  public URL getURL() {
    return url;
  }

  @Override
  public Reader getReader() throws IOException {
    return new InputStreamReader(localFile.openConnection().getInputStream());
  }

  @Override
  public String toString() {
    String nm = url.getFile();
    nm = nm.substring(nm.lastIndexOf('/') + 1);
    return '[' + nm + ':' + lineNumber + ']';
  }
}
