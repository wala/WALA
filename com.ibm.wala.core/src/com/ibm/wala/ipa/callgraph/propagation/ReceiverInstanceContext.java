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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/** This is a context which is customized for the {@link InstanceKey} of the receiver. */
public class ReceiverInstanceContext implements Context {

  private final InstanceKey ik;

  /** @param I the instance key that represents the receiver */
  public ReceiverInstanceContext(InstanceKey I) {
    if (I == null) {
      throw new IllegalArgumentException("null I");
    }
    this.ik = I;
  }

  @Override
  public ContextItem get(ContextKey name) {
    if (name == ContextKey.RECEIVER) return ik;
    else if (name == ContextKey.PARAMETERS[0])
      return new FilteredPointerKey.SingleInstanceFilter(ik);
    else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "ReceiverInstanceContext<" + ik + '>';
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ik == null) ? 0 : ik.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ReceiverInstanceContext other = (ReceiverInstanceContext) obj;
    if (ik == null) {
      if (other.ik != null) return false;
    } else if (!ik.equals(other.ik)) return false;
    return true;
  }

  public InstanceKey getReceiver() {
    return ik;
  }
}
