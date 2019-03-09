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
package com.ibm.wala.cast.js.cfg;

import com.ibm.wala.cast.ir.cfg.AstInducedCFG;
import com.ibm.wala.cast.js.ssa.JSInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAInstruction;

public class JSInducedCFG extends AstInducedCFG {

  public JSInducedCFG(SSAInstruction[] instructions, IMethod method, Context context) {
    super(instructions, method, context);
  }

  class JSPEIVisitor extends AstPEIVisitor implements JSInstructionVisitor {

    JSPEIVisitor(boolean[] r) {
      super(r);
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke inst) {
      breakBasicBlock();
    }

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction inst) {}

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {}

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {}

    @Override
    public void visitSetPrototype(SetPrototype instruction) {}

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {}
  }

  class JSBranchVisitor extends AstBranchVisitor implements JSInstructionVisitor {

    JSBranchVisitor(boolean[] r) {
      super(r);
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke inst) {}

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction inst) {}

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {}

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {}

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {}

    @Override
    public void visitSetPrototype(SetPrototype instruction) {}

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {}
  }

  @Override
  protected BranchVisitor makeBranchVisitor(boolean[] r) {
    return new JSBranchVisitor(r);
  }

  @Override
  protected PEIVisitor makePEIVisitor(boolean[] r) {
    return new JSPEIVisitor(r);
  }
}
