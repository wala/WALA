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
package com.ibm.wala.cast.ir.cfg;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.AstPropertyRead;
import com.ibm.wala.cast.ir.ssa.AstPropertyWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAInstruction;

public class AstInducedCFG extends InducedCFG {

  public AstInducedCFG(SSAInstruction[] instructions, IMethod method, Context context) {
    super(instructions, method, context);
  }

  protected class AstPEIVisitor extends PEIVisitor implements AstInstructionVisitor {

    protected AstPEIVisitor(boolean[] r) {
      super(r);
    }

    @Override
    public void visitPropertyRead(AstPropertyRead inst) {}

    @Override
    public void visitPropertyWrite(AstPropertyWrite inst) {}

    @Override
    public void visitAstLexicalRead(AstLexicalRead inst) {}

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite inst) {}

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {}

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {}

    @Override
    public void visitAssert(AstAssertInstruction instruction) {}

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {}

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {}

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {}

    @Override
    public void visitEcho(AstEchoInstruction inst) {}
  }

  protected class AstBranchVisitor extends BranchVisitor implements AstInstructionVisitor {

    protected AstBranchVisitor(boolean[] r) {
      super(r);
    }

    @Override
    public void visitPropertyRead(AstPropertyRead inst) {}

    @Override
    public void visitPropertyWrite(AstPropertyWrite inst) {}

    @Override
    public void visitAstLexicalRead(AstLexicalRead inst) {}

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite inst) {}

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {}

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {}

    @Override
    public void visitAssert(AstAssertInstruction instruction) {}

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {}

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {}

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {}

    @Override
    public void visitEcho(AstEchoInstruction inst) {}
  }

  @Override
  protected BranchVisitor makeBranchVisitor(boolean[] r) {
    return new AstBranchVisitor(r);
  }

  @Override
  protected PEIVisitor makePEIVisitor(boolean[] r) {
    return new AstPEIVisitor(r);
  }
}
