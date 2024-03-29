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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;

public interface JSInstructionVisitor extends AstInstructionVisitor {

  void visitJavaScriptInvoke(JavaScriptInvoke instruction);

  void visitTypeOf(JavaScriptTypeOfInstruction instruction);

  void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction);

  void visitWithRegion(JavaScriptWithRegion instruction);

  void visitCheckRef(JavaScriptCheckReference instruction);

  void visitSetPrototype(SetPrototype instruction);

  void visitPrototypeLookup(PrototypeLookup instruction);
}
