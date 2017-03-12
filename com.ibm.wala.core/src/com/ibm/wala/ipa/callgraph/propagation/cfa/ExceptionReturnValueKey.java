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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;

/**
 * A key which represents the return value for a node.
 */
public final class ExceptionReturnValueKey extends ReturnValueKey {
  public ExceptionReturnValueKey(CGNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return "[Exc-Ret-V:" + getNode() + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return (obj.getClass() == ExceptionReturnValueKey.class) && super.internalEquals(obj);
  }

  @Override
  public int hashCode() {
    return 1201 * super.internalHashCode();
  }
}
