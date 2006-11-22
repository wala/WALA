/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * Identifier of a statement in an SDG.
 * 
 * @author sjfink
 * 
 */
public abstract class Statement {
  public static enum Kind {
    NORMAL, PHI, PI, CATCH, PARAM_CALLER, PARAM_CALLEE, NORMAL_RET_CALLER, NORMAL_RET_CALLEE, EXC_RET_CALLER, EXC_RET_CALLEE,
    HEAP_PARAM_CALLER, HEAP_PARAM_CALLEE, HEAP_RET_CALLER, HEAP_RET_CALLEE, METHOD_ENTRY
  }

  private final CGNode node;

  public Statement(final CGNode node) {
    super();
    this.node = node;
  }

  public abstract Kind getKind();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  public CGNode getNode() {
    return node;
  }
}
