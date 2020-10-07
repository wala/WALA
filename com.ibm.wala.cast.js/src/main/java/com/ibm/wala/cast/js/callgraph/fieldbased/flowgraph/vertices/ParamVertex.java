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

/**
 * A parameter vertex represents a positional parameter of a function. It doesn't necessarily need
 * to correspond to a named parameter.
 *
 * <p>Numbering of positional parameters is 1-based, with parameter 0 being the {@code this} value.
 *
 * <p>A named parameter is an ordinary SSA variable, hence it is represented as a {@link VarVertex}.
 * The flow graph builder sets up edges between parameter vertices and their corresponding variable
 * vertices for named parameters.
 *
 * @author mschaefer
 */
public class ParamVertex extends Vertex {
  private final FuncVertex func;
  private final int index;

  ParamVertex(FuncVertex func, int index) {
    this.func = func;
    this.index = index;
  }

  public FuncVertex getFunc() {
    return func;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitParamVertex(this);
  }

  @Override
  public String toString() {
    return "Param(" + func + ", " + index + ')';
  }

  @Override
  public String toSourceLevelString(IAnalysisCacheView cache) {
    return "Param(" + func.toSourceLevelString(cache) + ", " + index + ')';
  }
}
