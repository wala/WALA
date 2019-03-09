/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

/**
 * A {@link Position} for source code that has been included in some enclosing file, e.g.,
 * JavaScript code included in an HTML file via a script node.
 */
public interface IncludedPosition extends Position {

  /**
   * get the position of the containing script within the enclosing file. E.g., for a position in
   * JavaScript code included in an HTML file, returns the position of the relevant {@code <script>}
   * tag in the HTML
   */
  Position getIncludePosition();
}
