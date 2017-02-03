/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cfg;

import com.ibm.wala.cfg.IBasicBlock;

/**
 * This class is used by the PrunedCFG to determine which edges in a given CFG should be kept in the pruned version.
 */
public interface EdgeFilter<T extends IBasicBlock> {

  /**
   * This method must return true if and only if a normal edge from src to dst exists in the original CFG and should be kept for the
   * pruned version of that CFG. Note that this must _must_ return false for any normal edge that is not in the original CFG.
   */
  boolean hasNormalEdge(T src, T dst);

  /**
   * This method must return true if and only if an exceptional edge from src to dst exists in the original CFG and should be kept
   * for the pruned version of that CFG. Note that this must _must_ return false for any exceptional edge that is not in the
   * original CFG.
   */
  boolean hasExceptionalEdge(T src, T dst);

}
