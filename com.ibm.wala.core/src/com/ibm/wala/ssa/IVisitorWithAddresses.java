/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

import com.ibm.wala.ssa.SSAInstruction.IVisitor;

/**
 * @author omert
 *     <p>Temporary interface to accomodate the newly added {@link SSAAddressOfInstruction}
 *     instruction. Ultimately, this interface should be merged into {@link IVisitor}.
 *     <p>TODO: Add 'visitAddressOf' to {@link IVisitor}.
 */
public interface IVisitorWithAddresses extends IVisitor {

  void visitAddressOf(SSAAddressOfInstruction instruction);

  void visitLoadIndirect(SSALoadIndirectInstruction instruction);

  void visitStoreIndirect(SSAStoreIndirectInstruction instruction);
}
