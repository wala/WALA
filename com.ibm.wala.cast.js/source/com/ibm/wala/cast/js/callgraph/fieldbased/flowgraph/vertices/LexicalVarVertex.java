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

/**
 * A lexical access vertex represents a lexical variable, i.e., a local variable that is accessed
 * from within a nested function. It is identified by the name of its defining function, and its own
 * name.
 *
 * @author mschaefer
 */
public class LexicalVarVertex extends Vertex {
  // name of the function defining this lexical variable
  private final String definer;

  // name of the lexical variable itself
  private final String name;

  LexicalVarVertex(String definer, String name) {
    this.definer = definer;
    this.name = name;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitLexicalAccessVertex(this);
  }

  @Override
  public String toString() {
    return "LexVar(" + definer.substring(definer.lastIndexOf('/') + 1) + ", " + name + ')';
  }
}
