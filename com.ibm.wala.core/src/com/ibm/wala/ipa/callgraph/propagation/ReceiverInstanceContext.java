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

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * This is a context which is customized for the instance key of the receiver.
 * 
 * @author sfink
 */
public final class ReceiverInstanceContext implements Context {

  private final InstanceKey I;

  /**
   * @param I
   *          the instance key that represents the receiver
   */
  public ReceiverInstanceContext(InstanceKey I) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(I != null);
    }
    this.I = I;
  }

  public ContextItem get(ContextKey name) {
    if (name == ContextKey.RECEIVER)
      return I;
    else if (name == ContextKey.FILTER)
      return new FilteredPointerKey.SingleInstanceFilter(I);
    else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "ReceiverInstanceContext<" + I + ">";
  }

  public int hashCode() {
    return I.hashCode() * 8747;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final.
    if (obj instanceof ReceiverInstanceContext) {
      ReceiverInstanceContext other = (ReceiverInstanceContext) obj;
      return I.equals(other.I);
    } else {
      return false;
    }
  }

  public InstanceKey getReceiver() {
    return I;
  }
}
