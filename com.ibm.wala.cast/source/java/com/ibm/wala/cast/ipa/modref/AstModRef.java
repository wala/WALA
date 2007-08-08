package com.ibm.wala.cast.ipa.modref;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.modref.*;

import java.util.*;

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
  }

  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new AstRefVisitor(n, result, pa, h);
  }

  protected static class AstModVisitor 
      extends ModVisitor 
      implements AstInstructionVisitor
  {
      
    protected AstModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis pa) {
      super(n, result, h, pa);
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
  }

  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new AstModVisitor(n, result, h, pa);
  }

}

