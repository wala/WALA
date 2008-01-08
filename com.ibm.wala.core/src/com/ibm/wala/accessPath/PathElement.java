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
package com.ibm.wala.accessPath;


/**
 * In principle, we could have used pointer-keys directly as components of the
 * access path. Nevertheless, I prefer having another layer of abstraction that
 * separates the actual implementation details (i.e., PointerKeys) from the
 * representation of the access paths.
 * 
 * @author Eran Yahav (yahave)
 * @author Stephen Fink
 */
public interface PathElement {

  /**
   * special element EMPTY_ELEMENT to avoid explicit null elements
   */
  public static final PathElement EMPTY_ELEMENT = new PathElement() {
    public boolean isAnchor() {
      return false;
    }

    @Override
    public String toString() {
      return "EmptyElement";
    }
  };

  /**
   * Is this path element constrained to be the first element in an access path?
   */
  public abstract boolean isAnchor();

}
