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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;

public interface JSInstructionVisitor extends AstInstructionVisitor {

  public void visitJavaScriptInvoke(JavaScriptInvoke instruction);
    
  public void visitTypeOf(JavaScriptTypeOfInstruction instruction);
    
  public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction);
  
  public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction);
  
  public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction);
  
  public void visitWithRegion(JavaScriptWithRegion instruction);
  
  public void visitCheckRef(JavaScriptCheckReference instruction);
  
  public void visitSetPrototype(SetPrototype instruction);
  
  public void visitPrototypeLookup(PrototypeLookup instruction);
  
}

