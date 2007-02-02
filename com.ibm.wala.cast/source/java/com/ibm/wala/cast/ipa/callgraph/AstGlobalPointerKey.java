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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.*;

public class AstGlobalPointerKey extends AbstractPointerKey {
  private final String globalName;
  public String getName() {
    return globalName;
  }

  public AstGlobalPointerKey(String globalName) {
    this.globalName = globalName;
  }

  public boolean equals(Object x) {
    return (x instanceof AstGlobalPointerKey) &&
      ((AstGlobalPointerKey)x).globalName.equals(globalName);
  }

  public int hashCode() {
    return globalName.hashCode();
  }

  public String toString() {
    return "[global: " + globalName + "]";
  }
}
    
