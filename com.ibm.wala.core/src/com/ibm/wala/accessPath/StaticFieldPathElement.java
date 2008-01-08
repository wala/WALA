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

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;

/**
 * A static field in an access-path string.
 * 
 * @author Eran Yahav (yahave)
 * @author Stephen Fink
 */
public class StaticFieldPathElement extends FieldPathElement implements PointerPathElement {

  private final StaticFieldKey sfk;

  public StaticFieldPathElement(StaticFieldKey sfk) {
    super(sfk.getField());
    this.sfk = sfk;
  }

  public boolean isAnchor() {
    return true;
  }

  public PointerKey getPointerKey() {
    return sfk;
  }

}
