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
package com.ibm.wala.ssa;

import java.util.Iterator;

import com.ibm.wala.cfg.IBasicBlock;

/**
 * Common interface to all SSA BasicBlocks
 * 
 * @author Eran Yahav (yahave)
 */
public interface ISSABasicBlock extends IBasicBlock {
  public boolean isCatchBlock();

  public boolean isExitBlock();

  public boolean isEntryBlock();

  public Iterator iteratePhis();

  public Iterator iteratePis();

  public SSAInstruction getLastInstruction();
}
