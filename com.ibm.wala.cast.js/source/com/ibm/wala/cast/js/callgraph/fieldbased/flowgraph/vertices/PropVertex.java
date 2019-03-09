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

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

/**
 * A property vertex represents all properties with a given name.
 *
 * @author mschaefer
 */
public class PropVertex extends Vertex implements PointerKey {
  private final String propName;

  PropVertex(String propName) {
    this.propName = propName;
  }

  public String getPropName() {
    return propName;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitPropVertex(this);
  }

  @Override
  public String toString() {
    return "Prop(" + propName + ')';
  }
}
