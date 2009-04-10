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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;

public interface AstInstructionVisitor extends SSAInstruction.IVisitor {

  public void visitAstLexicalRead(AstLexicalRead instruction);
    
  public void visitAstLexicalWrite(AstLexicalWrite instruction);
    
  public void visitAstGlobalRead(AstGlobalRead instruction);
    
  public void visitAstGlobalWrite(AstGlobalWrite instruction);

  public void visitAssert(AstAssertInstruction instruction);    

  public void visitEachElementGet(EachElementGetInstruction inst);

  public void visitEachElementHasNext(EachElementHasNextInstruction inst);

  public void visitIsDefined(AstIsDefinedInstruction inst);

  public void visitEcho(AstEchoInstruction inst);
}

