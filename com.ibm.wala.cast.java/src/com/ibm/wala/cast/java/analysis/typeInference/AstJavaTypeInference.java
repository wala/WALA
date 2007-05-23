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
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.debug.Assertions;

public class AstJavaTypeInference extends AstTypeInference {

  protected final IClass stringClass;

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
	  result = new PrimAndStringOp();
    	}
      }
    }
    
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      TypeReference type = inst.getEnclosingType();
      IClass klass = cha.lookupClass(type);
      if (klass == null) {
	Assertions.UNREACHABLE();
      } else {
        result = new DeclaredTypeOperator(new ConeType(klass));
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
          result = new DeclaredTypeOperator(new ConeType(klass));
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

  public class AstJavaTypeVarFactory extends TypeVarFactory {

    public IVariable makeVariable(int valueNumber) {
      SymbolTable st = ir.getSymbolTable();
      if (st.isStringConstant(valueNumber)) {
	IClass klass = cha.lookupClass(TypeReference.JavaLangString);
	TypeAbstraction stringTypeAbs = new PointType(klass);
	return new TypeVariable(stringTypeAbs, 797 * valueNumber);
      } else {
	return super.makeVariable(valueNumber);
      }
    }

  }

  public AstJavaTypeInference(IR ir, ClassHierarchy cha, boolean doPrimitives) {
    super(ir, cha, PrimitiveType.BOOLEAN, doPrimitives);
    this.stringClass = cha.lookupClass(TypeReference.JavaLangString);
  }

  protected void initialize() {
    init(ir, new AstJavaTypeVarFactory(), new AstJavaTypeOperatorFactory());
  }

  public TypeAbstraction getConstantPrimitiveType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (st.isBooleanConstant(valueNumber)) {
      return PrimitiveType.BOOLEAN;
    } else {
      return super.getConstantPrimitiveType(valueNumber);
    }
  }

  protected class PrimAndStringOp extends PrimitivePropagateOperator {

    private PrimAndStringOp() {
    }

    public byte evaluate(IVariable lhs, IVariable[] rhs) {
      TypeAbstraction meet = null;

      for (int i = 0; i < rhs.length; i++) {
	if (rhs[i] != null) {
	  TypeVariable r = (TypeVariable) rhs[i];
	  TypeAbstraction ta = r.getType();
	  if (ta instanceof PointType) {
	    if (ta.getType().equals(stringClass)) {
	      meet = new PointType(ta.getType());
	      break;
	    }
	  } else if (ta instanceof ConeType) {
	    if (ta.getType().equals(stringClass)) {
	      meet = new PointType(ta.getType());
	      break;
	    }
	  }
	}
      }

      if (meet == null) {
	return super.evaluate(lhs, rhs);
      } else {
	TypeVariable L = (TypeVariable) lhs;
	TypeAbstraction lhsType = L.getType();

	if (lhsType.equals(meet)) {
	  return NOT_CHANGED;
	} else {
	  L.setType(meet);
	  return CHANGED;
	}
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.Operator#hashCode()
     */
    public int hashCode() {
      return 71292;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
      return o != null && o.getClass().equals(getClass());
    }
  }

}

