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

import com.ibm.wala.fixpoint.UnaryOperator;

/**
 * A SideEffect is a constraint which carries a points-to-set which is def'fed or used in created
 * constraints.
 *
 * <p>The side effect doesn't actually def or use the fixedSet itself ... rather, the side effect
 * creates <em>new</em> constraints that def or use the fixed set.
 *
 * <p>A "load" operator generates defs of the fixed set. A "store" operator generates uses of the
 * fixed set.
 */
public abstract class UnarySideEffect extends UnaryOperator<PointsToSetVariable> {
  private PointsToSetVariable fixedSet;

  public UnarySideEffect(PointsToSetVariable fixedSet) {
    this.fixedSet = fixedSet;
  }

  @Override
  public final byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
    return evaluate(rhs);
  }

  public abstract byte evaluate(PointsToSetVariable rhs);

  /** @return Returns the fixed points-to-set associated with this side effect. */
  PointsToSetVariable getFixedSet() {
    return fixedSet;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (getClass().equals(o.getClass())) {
      UnarySideEffect other = (UnarySideEffect) o;
      return fixedSet.equals(other.fixedSet);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 8059 * fixedSet.hashCode();
  }

  /**
   * A "load" operator generates defs of the fixed set. A "store" operator generates uses of the
   * fixed set.
   */
  protected abstract boolean isLoadOperator();

  /** Update the fixed points-to-set associated with this side effect. */
  public void replaceFixedSet(PointsToSetVariable p) {
    fixedSet = p;
  }
}
