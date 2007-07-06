/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.cfg;

import com.ibm.wala.cast.ir.cfg.AstInducedCFG;
import com.ibm.wala.cast.js.ssa.InstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAInstruction;

public class JSInducedCFG extends AstInducedCFG {

  public JSInducedCFG(SSAInstruction[] instructions, IMethod method, Context context) {
    super(instructions, method, context);
  }

  class JSPEIVisitor extends AstPEIVisitor implements InstructionVisitor {

    JSPEIVisitor(boolean[] r) {
      super(r);
    }

    public void visitJavaScriptInvoke(JavaScriptInvoke inst) {
      breakBasicBlock();
    }

    public void visitJavaScriptPropertyRead(JavaScriptPropertyRead inst) {
    }

    public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite inst) {
    }

    public void visitTypeOf(JavaScriptTypeOfInstruction inst) {
    }
  }
    
  class JSBranchVisitor extends AstBranchVisitor implements InstructionVisitor {

    JSBranchVisitor(boolean[] r) {
      super(r);
    }

    public void visitJavaScriptInvoke(JavaScriptInvoke inst) {
    }

    public void visitJavaScriptPropertyRead(JavaScriptPropertyRead inst) {
    }

    public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite inst) {
    }

    public void visitTypeOf(JavaScriptTypeOfInstruction inst) {
    }
  }
    
  protected BranchVisitor makeBranchVisitor(boolean[] r) {
    return new JSBranchVisitor(r);
  }

  protected PEIVisitor makePEIVisitor(boolean[] r) {
    return new JSPEIVisitor(r);
  }

}
