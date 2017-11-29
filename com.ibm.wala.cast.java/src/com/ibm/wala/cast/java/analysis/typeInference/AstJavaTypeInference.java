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

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.JavaPrimitiveType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeVariable;
import com.ibm.wala.cast.analysis.typeInference.AstTypeInference;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class AstJavaTypeInference extends AstTypeInference {

  protected IClass stringClass;

  protected class AstJavaTypeOperatorFactory extends AstTypeOperatorFactory implements AstJavaInstructionVisitor {
    @Override
    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
      if (doPrimitives) {
        IBinaryOpInstruction.IOperator op = instruction.getOperator();
        if (op == CAstBinaryOp.EQ || op == CAstBinaryOp.NE || op == CAstBinaryOp.LT
            || op == CAstBinaryOp.GE || op == CAstBinaryOp.GT || op == CAstBinaryOp.LE) {
          result = new DeclaredTypeOperator(language.getPrimitive(language.getConstantType(Boolean.TRUE)));
        } else {
          result = new PrimAndStringOp();
        }
      }
    }

    @Override
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      TypeReference type = inst.getEnclosingType();
      IClass klass = cha.lookupClass(type);
      if (klass == null) {
        Assertions.UNREACHABLE();
      } else {
        result = new DeclaredTypeOperator(new ConeType(klass));
      }
    }

    @Override
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

    @Override
    public IVariable makeVariable(int valueNumber) {
      SymbolTable st = ir.getSymbolTable();
      if (st.isStringConstant(valueNumber)) {
        IClass klass = cha.lookupClass(TypeReference.JavaLangString);
        TypeAbstraction stringTypeAbs = new PointType(klass);
        return new TypeVariable(stringTypeAbs);
      } else {
        return super.makeVariable(valueNumber);
      }
    }

  }

  public AstJavaTypeInference(IR ir, boolean doPrimitives) {
    super(ir, JavaPrimitiveType.BOOLEAN, doPrimitives);
  }

  IClass getStringClass() {
    if (stringClass == null) {
      this.stringClass = cha.lookupClass(TypeReference.JavaLangString);
    }
    return stringClass;
  }

  @Override
  protected void initialize() {
    init(ir, new AstJavaTypeVarFactory(), new AstJavaTypeOperatorFactory());
  }

  @Override
  public TypeAbstraction getConstantPrimitiveType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (st.isBooleanConstant(valueNumber)) {
      return language.getPrimitive(language.getConstantType(Boolean.TRUE));
    } else {
      return super.getConstantPrimitiveType(valueNumber);
    }
  }

  protected class PrimAndStringOp extends PrimitivePropagateOperator {

    private PrimAndStringOp() {
    }

    @Override
    public byte evaluate(TypeVariable lhs, TypeVariable[] rhs) {
      TypeAbstraction meet = null;

      for (TypeVariable r : rhs) {
        if (r != null) {
          TypeAbstraction ta = r.getType();
          if (ta instanceof PointType) {
            if (ta.getType().equals(getStringClass())) {
              meet = new PointType(ta.getType());
              break;
            }
          } else if (ta instanceof ConeType) {
            if (ta.getType().equals(getStringClass())) {
              meet = new PointType(ta.getType());
              break;
            }
          }
        }
      }

      if (meet == null) {
        return super.evaluate(lhs, rhs);
      } else {
        TypeVariable L = lhs;
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
    @Override
    public int hashCode() {
      return 71292;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      return o != null && o.getClass().equals(getClass());
    }
  }

}
