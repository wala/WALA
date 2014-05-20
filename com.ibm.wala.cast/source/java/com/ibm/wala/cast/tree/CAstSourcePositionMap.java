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
package com.ibm.wala.cast.tree;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;

import com.ibm.wala.classLoader.IMethod.SourcePosition;

/**
 *  The assumption is that a typical CAst is derived from some kind of
 * textual source file, for which it makes sense to record source
 * position in terms of line and column numbers.  This interface
 * encapsulates a mapping from CAstNodes of the an ast to such source
 * positions. 
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public interface CAstSourcePositionMap {

  /**
   *  This interface encapsulates the source position of an ast node
   * in its source file.  Since different parsers record different
   * degrees of source position information, any client of these
   * Positions must be prepared to expect -1---symbolizing no
   * information---to be returned by some or all of its accessors.
   *
   * @author Julian Dolby (dolby@us.ibm.com)
   */
  public interface Position extends SourcePosition {
    URL getURL();
    Reader getReader() throws IOException;
  }

  /** 
   *  Returns the position of a given node in its source file, or
   * null if the position is not known or does not exist.
   */
  Position getPosition(CAstNode n);

  /**
   *  Returns an iterator of all CAstNodes for which this map contains
   * source mapping information.
   */
  Iterator<CAstNode> getMappedNodes();

}
