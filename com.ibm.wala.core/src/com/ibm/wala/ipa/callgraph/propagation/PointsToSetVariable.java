/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class PointsToSetVariable extends IntSetVariable<PointsToSetVariable> {
  private PointerKey pointerKey;

  public PointsToSetVariable(PointerKey key) {
    super();
    if (Assertions.verifyAssertions) {
      Assertions._assert(key != null);
    }
    this.pointerKey = key;
  }

  /**
   * @return Returns the pointerKey.
   */
  public PointerKey getPointerKey() {
    return pointerKey;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PointsToSetVariable) {
      return pointerKey.equals(((PointsToSetVariable) obj).pointerKey);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return pointerKey.hashCode();
  }

  /**
   * Use this with extreme care, to add filters to this variable..
   * 
   * @param pointerKey
   *          The pointerKey to set.
   */
  void setPointerKey(PointerKey pointerKey) {
    // check that we haven't modified the hash code!!! this is crucial
    if (Assertions.verifyAssertions) {
      Assertions._assert(this.pointerKey.hashCode() == pointerKey.hashCode());
    }
    this.pointerKey = pointerKey;
  }

  @Override
  public String toString() {
    return pointerKey.toString() + ":" + super.toString();
  }
}
