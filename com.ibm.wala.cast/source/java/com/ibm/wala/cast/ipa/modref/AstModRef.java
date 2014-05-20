/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.modref;

import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;

public class AstModRef extends ModRef {

  protected static class AstRefVisitor 
      extends RefVisitor 
      implements AstInstructionVisitor
  {
      
    protected AstRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h) {
      super(n, result, pa, h);
    }

    @Override
    public void visitAstLexicalRead(AstLexicalRead instruction) {

    }
    
    @Override
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
    
    }
    
    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {

    }
    
    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
    
    }
    
    @Override
    public void visitAssert(AstAssertInstruction instruction) {

    }    

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {

    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
    
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  @Override
  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h) {
    return new AstRefVisitor(n, result, pa, h);
  }

  protected static class AstModVisitor 
      extends ModVisitor 
      implements AstInstructionVisitor
  {
      
    protected AstModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis<InstanceKey> pa) {
      super(n, result, h, pa, true);
    }

    @Override
    public void visitAstLexicalRead(AstLexicalRead instruction) {

    }
    
    @Override
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
    
    }
    
    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {

    }
    
    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
    
    }
    
    @Override
    public void visitAssert(AstAssertInstruction instruction) {

    }    

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {

    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
    
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  @Override
  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstModVisitor(n, result, h, pa);
  }

}

