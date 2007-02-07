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
package com.ibm.wala.cast.java.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.*;
import com.ibm.wala.cast.analysis.typeInference.*;
import com.ibm.wala.cast.ir.ssa.AstConstants;
import com.ibm.wala.cast.java.ssa.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.debug.Assertions;

public class AstJavaTypeInference extends AstTypeInference {

  protected class AstJavaTypeOperatorFactory
      extends AstTypeOperatorFactory
      implements AstJavaInstructionVisitor 
  {
    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
      if (doPrimitives) {
        BinaryOpInstruction.IOperator op = instruction.getOperator();
    	if (op == AstConstants.BinaryOp.EQ ||
    		op == AstConstants.BinaryOp.NE ||
    		op == AstConstants.BinaryOp.LT ||
    		op == AstConstants.BinaryOp.GE ||
    		op == AstConstants.BinaryOp.GT ||
    		op == AstConstants.BinaryOp.LE) {
    		result = new DeclaredTypeOperator(PrimitiveType.BOOLEAN);
    	} else {
    		super.visitBinaryOp(instruction);
    	}
      }
    }
    
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      TypeReference type = inst.getEnclosingType();
      IClass klass = cha.lookupClass(type);
      if (klass == null) {
	Assertions.UNREACHABLE();
      } else {
        result = new DeclaredTypeOperator(new ConeType(klass, cha));
      }
    }

    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      TypeReference type = instruction.getDeclaredResultType();
      if (type.isReferenceType()) {
        IClass klass = cha.lookupClass(type);
	    if (klass == null) {
          // a type that cannot be loaded.
          // be pessimistic
          result = new DeclaredTypeOperator(BOTTOM);
	    } else {
          result = new DeclaredTypeOperator(new ConeType(klass, cha));
	    }
      } else {
        if (doPrimitives && type.isPrimitiveType()) {
    	  result = new DeclaredTypeOperator(PrimitiveType.getPrimitive(type));
    	} else {
    	  result = null;
    	}
      }
    }
  }

  public AstJavaTypeInference(IR ir, ClassHierarchy cha, boolean doPrimitives) {
    super(ir, cha, doPrimitives);
  }

  protected void initialize() {
    init(ir, new TypeVarFactory(), new AstJavaTypeOperatorFactory());
  }

  public TypeAbstraction getConstantPrimitiveType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
 	if (st.isIntegerConstant(valueNumber)) {
 	  int val = ((Number)st.getConstantValue(valueNumber)).intValue();
 	  if (val < 2) {
 	    return PrimitiveType.BOOLEAN;
 	  } else if (val < 256) {
 		return PrimitiveType.BYTE;
 	  } else if (val < 16384) {
 		return PrimitiveType.SHORT; 
 	  } else {
 		return PrimitiveType.INT;
 	  }
 	} else {
      return super.getConstantPrimitiveType(valueNumber);
 	}
  }
}

