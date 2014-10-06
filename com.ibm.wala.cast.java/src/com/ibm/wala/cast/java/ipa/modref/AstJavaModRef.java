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
package com.ibm.wala.cast.java.ipa.modref;

import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;

public class AstJavaModRef extends AstModRef {

  protected static class AstJavaRefVisitor 
      extends AstRefVisitor 
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h) {
      super(n, result, pa, (AstHeapModel)h);
    }

    @Override
    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {

    }

    @Override
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {

    }

  }

  @Override
  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h) {
    return new AstJavaRefVisitor(n, result, pa, h);
  }

  protected static class AstJavaModVisitor 
      extends AstModVisitor 
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis<InstanceKey> pa) {
      super(n, result, (AstHeapModel)h, pa);
    }


    @Override
    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {

    }

    @Override
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {

    }
  }
 
  @Override
  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstJavaModVisitor(n, result, h, pa);
  }

}

