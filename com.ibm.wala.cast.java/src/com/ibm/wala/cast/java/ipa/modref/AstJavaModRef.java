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

public class AstJavaModRef<T extends InstanceKey> extends AstModRef<T> {

  protected static class AstJavaRefVisitor<T extends InstanceKey>
      extends AstRefVisitor<T>
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
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
  protected RefVisitor<T, ? extends ExtendedHeapModel> makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
    return new AstJavaRefVisitor<>(n, result, pa, h);
  }

  protected static class AstJavaModVisitor<T extends InstanceKey> 
      extends AstModVisitor<T> 
      implements AstJavaInstructionVisitor
  {
      
    protected AstJavaModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis<T> pa) {
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
  protected ModVisitor<T, ? extends ExtendedHeapModel> makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
    return new AstJavaModVisitor<>(n, result, h, pa);
  }

}

