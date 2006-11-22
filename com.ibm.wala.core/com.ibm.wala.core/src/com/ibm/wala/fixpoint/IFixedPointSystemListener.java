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

/**
 * This interface is used to report to interested parties (e.g. a
 * IFixedPointSolver updates to a
 * IFixedPointSystem.
 * <p>
 * For example, an IFixedPointSolver can
 * implement this interface and listen to changes its
 * IFixedPointSystem}so that it can take
 * appropriate actions as a result to the observed updates.
 */
public interface IFixedPointSystemListener {
  /**
   * call-back method that indicates that a given statement has been added to a
   * given system
   */
  public void statementAdded(IFixedPointStatement s, IFixedPointSystem system);

  /**
   * call-back method that indicates that a given statement has been removed
   * from a given system
   */
  public void statementRemoved(IFixedPointStatement s, IFixedPointSystem constraints);
}