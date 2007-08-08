package com.ibm.wala.cast.java.ipa.modref;

import com.ibm.wala.cast.ipa.modref.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.java.ssa.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.modref.*;

import java.util.*;

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
 
  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new AstJavaModVisitor(n, result, h, pa);
  }

}

