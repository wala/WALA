/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

public final class RootPos extends NodePos {
  @Override
  public <A> A accept(PosSwitch<A> ps) {
    return ps.caseRootPos(this);
  }

  @Override
  public int hashCode() {
    return 3741;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof RootPos;
  }
}
