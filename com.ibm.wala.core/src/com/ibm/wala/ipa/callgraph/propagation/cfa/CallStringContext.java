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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

public class CallStringContext implements Context {
  private final CallString cs;

  public CallStringContext(CallString cs) {
    if (cs == null) {
      throw new IllegalArgumentException("null cs");
    }
    this.cs = cs;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof CallStringContext) && ((CallStringContext) o).cs.equals(cs);
  }

  @Override
  public int hashCode() {
    return cs.hashCode();
  }

  @Override
  public String toString() {
    return "CallStringContext: " + cs.toString();
  }

  @Override
  public ContextItem get(ContextKey name) {
    if (CallStringContextSelector.CALL_STRING.equals(name)) {
      return cs;
    } else {
      return null;
    }
  }
}
