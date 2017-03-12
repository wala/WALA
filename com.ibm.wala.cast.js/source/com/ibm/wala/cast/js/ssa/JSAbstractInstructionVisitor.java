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

import com.ibm.wala.cast.ir.ssa.AstAbstractInstructionVisitor;

public class JSAbstractInstructionVisitor 
    extends AstAbstractInstructionVisitor 
    implements JSInstructionVisitor
{

  @Override
  public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
  
  }
    
  @Override
  public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
  
  }
  
  @Override
  public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
  
  }

  @Override
  public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
      
  }

  @Override
  public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {
     
  }

  @Override
  public void visitCheckRef(JavaScriptCheckReference instruction) {
 
  }

  @Override
  public void visitWithRegion(JavaScriptWithRegion instruction) {
    
  }

  @Override
  public void visitSetPrototype(SetPrototype instruction) {
    
  }

  @Override
  public void visitPrototypeLookup(PrototypeLookup instruction) {
    
  }

}

