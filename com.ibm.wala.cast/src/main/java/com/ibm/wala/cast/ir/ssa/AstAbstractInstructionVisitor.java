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

public abstract class AstAbstractInstructionVisitor extends SSAInstruction.Visitor
    implements AstInstructionVisitor {

  @Override
  public void visitPropertyRead(AstPropertyRead instruction) {}

  @Override
  public void visitPropertyWrite(AstPropertyWrite instruction) {}

  @Override
  public void visitAstLexicalRead(AstLexicalRead instruction) {}

  @Override
  public void visitAstLexicalWrite(AstLexicalWrite instruction) {}

  @Override
  public void visitAstGlobalRead(AstGlobalRead instruction) {}

  @Override
  public void visitAstGlobalWrite(AstGlobalWrite instruction) {}

  @Override
  public void visitAssert(AstAssertInstruction instruction) {}

  @Override
  public void visitEachElementGet(EachElementGetInstruction inst) {}

  @Override
  public void visitEachElementHasNext(EachElementHasNextInstruction inst) {}

  @Override
  public void visitIsDefined(AstIsDefinedInstruction inst) {}

  @Override
  public void visitEcho(AstEchoInstruction inst) {}
}
