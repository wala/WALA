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
package com.ibm.wala.stringAnalysis.js.ssa;

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.stringAnalysis.ssa.SAProcessingInstructionVisitor;

public class SAJSProcessingInstructionVisitor 
  extends SAProcessingInstructionVisitor 
  implements com.ibm.wala.cast.js.ssa.InstructionVisitor
{
    public interface Processor 
      extends SAProcessingInstructionVisitor.Processor 
    {
	public void onJavaScriptPropertyRead(JavaScriptPropertyRead instruction);
	public void onJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction);
    }

    public SAJSProcessingInstructionVisitor(Processor processor) {
      super(processor);
    }

    public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
        ((Processor)processor).onSSAAbstractInvokeInstruction(instruction);
    }

    public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
        ((Processor)processor).onUnsupportedInstruction(instruction);
    }

    public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
        ((Processor)processor).onJavaScriptPropertyRead(instruction);
    }

    public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
        ((Processor)processor).onJavaScriptPropertyWrite(instruction);
    }
}
