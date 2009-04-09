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
import com.ibm.wala.cast.ir.ssa.NonExceptingThrowInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;

public class AstModRef extends ModRef {

  protected static class AstRefVisitor 
      extends RefVisitor 
      implements AstInstructionVisitor
  {
      
    protected AstRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
      super(n, result, pa, h);
    }

    public void visitAstLexicalRead(AstLexicalRead instruction) {

    }
    
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
    
    }
    
    public void visitAstGlobalRead(AstGlobalRead instruction) {

    }
    
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
    
    }
    
    public void visitNonExceptingThrow(NonExceptingThrowInstruction inst) {

    }

    public void visitAssert(AstAssertInstruction instruction) {

    }    

    public void visitEachElementGet(EachElementGetInstruction inst) {

    }

    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
    
    }

    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new AstRefVisitor(n, result, pa, h);
  }

  protected static class AstModVisitor 
      extends ModVisitor 
      implements AstInstructionVisitor
  {
      
    protected AstModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis pa) {
      super(n, result, h, pa, true);
    }

    public void visitAstLexicalRead(AstLexicalRead instruction) {

    }
    
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
    
    }
    
    public void visitAstGlobalRead(AstGlobalRead instruction) {

    }
    
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
    
    }
    
    public void visitNonExceptingThrow(NonExceptingThrowInstruction inst) {

    }

    public void visitAssert(AstAssertInstruction instruction) {

    }    

    public void visitEachElementGet(EachElementGetInstruction inst) {

    }

    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
    
    }

    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  @Override
  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstModVisitor(n, result, h, pa);
  }

}

