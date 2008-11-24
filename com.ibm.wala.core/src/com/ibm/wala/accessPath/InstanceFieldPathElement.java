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


import com.ibm.wala.classLoader.IField;
import com.ibm.wala.util.debug.Assertions;

/**
 * An instance field in an access-path string.
 * 
 * @author Eran Yahav (yahave)
 * @author Stephen Fink
 */
@Deprecated
public class InstanceFieldPathElement extends FieldPathElement {

  /**
   * @param fld
   */
  public InstanceFieldPathElement(IField fld) {
    super(fld);
    if (Assertions.verifyAssertions) {
      Assertions._assert(!fld.isStatic());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.safe.typestate.accesspaths.PathElement#isAnchor()
   */
  public boolean isAnchor() {
    return false;
  }
}
