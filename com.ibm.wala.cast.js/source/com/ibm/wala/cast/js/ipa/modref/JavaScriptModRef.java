/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.modref;

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.js.ssa.JSInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import java.util.Collection;

public class JavaScriptModRef<T extends InstanceKey> extends AstModRef<T> {

  public static class JavaScriptRefVisitor<T extends InstanceKey> extends AstRefVisitor<T>
      implements JSInstructionVisitor {

    public JavaScriptRefVisitor(
        CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
      super(n, result, pa, (AstHeapModel) h);
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
      // do nothing
    }

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
      // do nothing
    }

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {
      // do nothing
    }

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {
      // do nothing
    }

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {
      // do nothing
    }

    @Override
    public void visitSetPrototype(SetPrototype instruction) {
      // do nothing
    }

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {
      // TODO Auto-generated method stub

    }
  }

  @Override
  protected RefVisitor<T, ? extends ExtendedHeapModel> makeRefVisitor(
      CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
    return new JavaScriptRefVisitor<>(n, result, pa, h);
  }

  public static class JavaScriptModVisitor<T extends InstanceKey> extends AstModVisitor<T>
      implements JSInstructionVisitor {

    public JavaScriptModVisitor(
        CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis<T> pa) {
      super(n, result, (AstHeapModel) h, pa);
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
      // do nothing
    }

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
      // do nothing
    }

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {
      // do nothing
    }

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {
      // do nothing
    }

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {
      // do nothing
    }

    @Override
    public void visitSetPrototype(SetPrototype instruction) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {
      // do nothing
    }
  }

  @Override
  protected ModVisitor<T, ? extends ExtendedHeapModel> makeModVisitor(
      CGNode n,
      Collection<PointerKey> result,
      PointerAnalysis<T> pa,
      ExtendedHeapModel h,
      boolean ignoreAllocHeapDefs) {
    return new JavaScriptModVisitor<>(n, result, h, pa);
  }
}
