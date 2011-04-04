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

import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;

public class IncludedLineNumberPosition extends LineNumberPosition implements IncludedPosition {
  private final Position includePosition;
  
  public IncludedLineNumberPosition(URL url, URL localFile, int lineNumber, Position includePosition) {
    super(url, localFile, lineNumber);
    this.includePosition = includePosition;
  }

  public Position getIncludePosition() {
    return includePosition;
  }

  public String toString() {
    return super.toString() + "(from " + includePosition +")";
  }
}
