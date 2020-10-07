/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

/**
 * A return vertex represents the 'arguments' array of a given function.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class ArgVertex extends Vertex implements PointerKey {
  private final FuncVertex func;

  ArgVertex(FuncVertex func) {
    this.func = func;
  }

  public FuncVertex getFunc() {
    return func;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitArgVertex(this);
  }

  @Override
  public String toString() {
    return "Args(" + func + ')';
  }

  @Override
  public String toSourceLevelString(IAnalysisCacheView cache) {
    return "Args(" + func.toSourceLevelString(cache) + ')';
  }
}
