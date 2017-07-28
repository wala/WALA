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
package com.ibm.wala.cast.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.ssa.IR;

public abstract class AstTypeInference extends TypeInference {

  private final TypeAbstraction booleanType;

  protected class AstTypeOperatorFactory extends TypeOperatorFactory implements AstInstructionVisitor {
    @Override
    public void visitAstLexicalRead(AstLexicalRead inst) {
      result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
    }

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite inst) {
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {
      result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
    }

    @Override
    public void visitAssert(AstAssertInstruction instruction) {
    }

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {
      result = new DeclaredTypeOperator(new ConeType(cha.getRootClass()));
    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {
      if (doPrimitives) {
        result = new DeclaredTypeOperator(booleanType);
      }
    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  public AstTypeInference(IR ir, TypeAbstraction booleanType, boolean doPrimitives) {
    super(ir, doPrimitives);
    this.booleanType = booleanType;
  }

  @Override
  protected void initialize() {
    init(ir, new TypeVarFactory(), new AstTypeOperatorFactory());
  }

}
