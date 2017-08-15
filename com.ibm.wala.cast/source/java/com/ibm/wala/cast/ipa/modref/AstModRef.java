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

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
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

public class AstModRef<T extends InstanceKey> extends ModRef<T> {

  @Override
  public ExtendedHeapModel makeHeapModel(PointerAnalysis<T> pa) {
    return (AstHeapModel)pa.getHeapModel();
  }

  protected static class AstRefVisitor<T extends InstanceKey> extends RefVisitor<T, AstHeapModel> implements AstInstructionVisitor {
      
    protected AstRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, AstHeapModel h) {
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
  protected RefVisitor<T, ? extends ExtendedHeapModel> makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
    return new AstRefVisitor<>(n, result, pa, (AstHeapModel)h);
  }

  protected static class AstModVisitor<T extends InstanceKey> 
      extends ModVisitor<T, AstHeapModel>
      implements AstInstructionVisitor
  {
      
    protected AstModVisitor(CGNode n, Collection<PointerKey> result, AstHeapModel h, PointerAnalysis<T> pa) {
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
  protected ModVisitor<T, ? extends ExtendedHeapModel> makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstModVisitor<>(n, result, (AstHeapModel)h, pa);
  }

}

