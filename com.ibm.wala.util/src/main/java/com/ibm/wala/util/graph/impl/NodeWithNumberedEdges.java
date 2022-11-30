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
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.INodeWithNumberedEdges;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.IntSet;

/** Simple implementation of {@link INodeWithNumberedEdges} */
public class NodeWithNumberedEdges extends NodeWithNumber implements INodeWithNumberedEdges {

  private BimodalMutableIntSet predNumbers;

  private BimodalMutableIntSet succNumbers;

  @Override
  public IntSet getSuccNumbers() {
    return succNumbers;
  }

  @Override
  public IntSet getPredNumbers() {
    return predNumbers;
  }

  /**
   * Note that this variable appears on the RHS of an equation.
   *
   * @param eqNumber the equation number
   */
  @Override
  public void addSucc(int eqNumber) {
    if (succNumbers == null) {
      succNumbers = new BimodalMutableIntSet();
    }
    succNumbers.add(eqNumber);
  }

  /**
   * Note that this variable appears on the LHS of an equation.
   *
   * @param eqNumber the equation number
   */
  @Override
  public void addPred(int eqNumber) {
    if (predNumbers == null) {
      predNumbers = new BimodalMutableIntSet();
    }
    predNumbers.add(eqNumber);
  }

  /** remove the edge that indicates this variable is Succd by a certain equation */
  public void deleteSucc(int eqNumber) {
    if (succNumbers != null) {
      succNumbers.remove(eqNumber);
      if (succNumbers.size() == 0) {
        succNumbers = null;
      }
    }
  }

  /** remove the edge that indicates this variable is Predined by a certain equation */
  public void deletePred(int eqNumber) {
    if (predNumbers != null) {
      predNumbers.remove(eqNumber);
      if (predNumbers.size() == 0) {
        predNumbers = null;
      }
    }
  }

  @Override
  public void removeAllIncidentEdges() throws UnimplementedError {
    Assertions.UNREACHABLE("Implement me");
  }

  @Override
  public void removeIncomingEdges() throws UnimplementedError {
    Assertions.UNREACHABLE("Implement me");
  }

  @Override
  public void removeOutgoingEdges() throws UnimplementedError {
    Assertions.UNREACHABLE("Implement me");
  }
}
