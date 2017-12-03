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
package com.ibm.wala.fixpoint;

import java.util.Iterator;

import com.ibm.wala.util.graph.INodeWithNumber;

/**
 * Represents a set of {@link IFixedPointStatement}s to be solved by a {@link IFixedPointSolver}
 */
public interface IFixedPointSystem<T extends IVariable<T>> {

  /**
   * removes a given statement
   */
  void removeStatement(IFixedPointStatement<T> statement);

  /**
   * Add a statement to the system
   */
  public void addStatement(IFixedPointStatement<T> statement);

  /**
   * Return an Iterator of the {@link IFixedPointStatement}s in this system
   * 
   * @return {@link Iterator}&lt;Constraint&gt;
   */
  public Iterator<? extends INodeWithNumber> getStatements();

  /**
   * Return an Iterator of the variables in this graph
   * 
   * @return {@link Iterator}&lt;{@link IVariable}&gt;
   */
  public Iterator<? extends INodeWithNumber> getVariables();

  /**
   * @return true iff this system already contains an equation that is equal() to s
   */
  boolean containsStatement(IFixedPointStatement<T> s);

  /**
   * @return true iff this system already contains a variable that is equal() to v.
   */
  boolean containsVariable(T v);

  /**
   * @return {@link Iterator}&lt;statement&gt;, the statements that use the variable
   */
  Iterator<? extends INodeWithNumber> getStatementsThatUse(T v);

  /**
   * @return {@link Iterator}&lt;statement&gt;, the statements that def the variable
   */
  Iterator<? extends INodeWithNumber> getStatementsThatDef(T v);

  int getNumberOfStatementsThatUse(T v);

  int getNumberOfStatementsThatDef(T v);

  /**
   * reorder the statements in this system
   */
  void reorder();

}
