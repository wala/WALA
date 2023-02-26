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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;

@SuppressWarnings("unused")
public interface AstInstructionVisitor extends SSAInstruction.IVisitor {

  default void visitAstLexicalRead(AstLexicalRead instruction) {}

  default void visitAstLexicalWrite(AstLexicalWrite instruction) {}

  default void visitAstGlobalRead(AstGlobalRead instruction) {}

  default void visitAstGlobalWrite(AstGlobalWrite instruction) {}

  default void visitAssert(AstAssertInstruction instruction) {}

  default void visitEachElementGet(EachElementGetInstruction inst) {}

  default void visitEachElementHasNext(EachElementHasNextInstruction inst) {}

  default void visitIsDefined(AstIsDefinedInstruction inst) {}

  default void visitEcho(AstEchoInstruction inst) {}

  default void visitYield(AstYieldInstruction inst) {}

  default void visitPropertyRead(AstPropertyRead instruction) {}

  default void visitPropertyWrite(AstPropertyWrite instruction) {}
}
