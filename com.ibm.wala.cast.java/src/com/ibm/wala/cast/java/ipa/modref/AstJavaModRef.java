package com.ibm.wala.cast.java.ipa.modref;

import java.util.Collection;

import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;

public class AstJavaModRef extends AstModRef {

  protected static class AstJavaRefVisitor 
      extends AstRefVisitor 
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
      super(n, result, pa, h);
    }

    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {

    }

    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {

    }

  }

  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new AstJavaRefVisitor(n, result, pa, h);
  }

  protected static class AstJavaModVisitor 
      extends AstModVisitor 
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis pa) {
      super(n, result, h, pa);
    }


    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {

    }

    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {

    }
  }
 
  @Override
  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstJavaModVisitor(n, result, h, pa);
  }

}

