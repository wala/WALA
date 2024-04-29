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

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import java.net.URL;

public abstract class AbstractSourcePosition implements Position {

  @Override
  public boolean equals(Object o) {
    if (o instanceof Position) {
      Position p = (Position) o;
      return getFirstLine() == p.getFirstLine()
          && getLastLine() == p.getLastLine()
          && getFirstCol() == p.getFirstCol()
          && getLastCol() == p.getLastCol()
          && getFirstOffset() == p.getFirstOffset()
          && getLastOffset() == p.getLastOffset()
          && ((getURL() != null)
              ? ((Object) getURL()).toString().equals(((Object) p.getURL()).toString())
              : p.getURL() == null);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getFirstLine() * getLastLine() * getFirstCol() * getLastCol();
  }

  @Override
  public int compareTo(SourcePosition o) {
    if (o instanceof Position) {
      Position p = (Position) o;
      if (getFirstLine() != p.getFirstLine()) {
        return getFirstLine() - p.getFirstLine();
      } else if (getFirstCol() != p.getFirstCol()) {
        return getFirstCol() - p.getFirstCol();
      } else if (getLastLine() != p.getLastLine()) {
        return getLastLine() - p.getLastLine();
      } else {
        return getLastCol() - p.getLastCol();
      }
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    URL x = getURL();
    String xf = x.toString();
    if (xf.indexOf('/') >= 0) {
      xf = xf.substring(xf.lastIndexOf('/') + 1);
    }
    String pos;
    if (getFirstCol() != -1) {
      pos =
          "["
              + getFirstLine()
              + ':'
              + getFirstCol()
              + "] -> ["
              + getLastLine()
              + ':'
              + getLastCol()
              + ']';
    } else if (getFirstOffset() != -1) {
      pos = "[" + getFirstOffset() + "->" + getLastOffset() + "] (line " + getFirstLine() + ')';
    } else {
      pos = "(line " + getFirstLine() + ')';
    }
    return xf + ' ' + pos;
  }
}
