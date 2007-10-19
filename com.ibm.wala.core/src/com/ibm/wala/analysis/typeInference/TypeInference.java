/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.typeInference;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.ssa.SSAInference;
import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * This class performs intraprocedural type propagation on an SSA IR.
 * 
 * @author sfink
 */
public class TypeInference extends SSAInference<TypeVariable> implements FixedPointConstants {

  public static TypeInference make(IR ir, boolean doPrimitives) {
    return new TypeInference(ir, doPrimitives);
  }

  /**
   * The governing SSA form
   */
  final protected IR ir;

  /**
   * The governing class hierarchy
   */
  final protected IClassHierarchy cha;

  /**
   * A singleton instance of the phi operator.
   */
  private final static AbstractOperator<TypeVariable> phiOp = new PhiOperator();

  private final static AbstractOperator<TypeVariable> primitivePropagateOp = new PrimitivePropagateOperator();

  /**
   * A cone type for java.lang.Object
   */
  protected final TypeAbstraction BOTTOM;

  /**
   * A singleton instance of the pi operator.
   */
  private final static PiOperator piOp = new PiOperator();

  /**
   * should type inference track primitive types?
   */
  protected final boolean doPrimitives;

  private boolean solved = false;

  protected TypeInference(IR ir, boolean doPrimitives) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    this.cha = ir.getMethod().getDeclaringClass().getClassHierarchy();
    this.ir = ir;
    this.doPrimitives = doPrimitives;
    this.BOTTOM = new ConeType(cha.getRootClass());
    initialize();
    solve();
  }

  @Override
  public boolean solve() {
    if (solved) {
      return false;
    } else {
      boolean result = super.solve();
      solved = true;
      return result;
    }
  }

  protected void initialize() {
    init(ir, this.new TypeVarFactory(), this.new TypeOperatorFactory());
  }

  @Override
  protected void initializeVariables() {
    int[] parameterValueNumbers = ir.getParameterValueNumbers();
    for (int i = 0; i < parameterValueNumbers.length; i++) {
      TypeVariable v = getVariable(parameterValueNumbers[i]);
      TypeReference t = ir.getParameterType(i);

      if (t.isReferenceType()) {
        IClass klass = cha.lookupClass(t);
        if (klass != null) {
          v.setType(new ConeType(klass));
        } else {
          v.setType(TypeAbstraction.TOP);
          // v.setType(BOTTOM);
        }
      } else if (doPrimitives) {
        v.setType(PrimitiveType.getPrimitive(t));
      }
    }

    SymbolTable st = ir.getSymbolTable();
    if (st != null) {
      for (int i = 0; i <= st.getMaxValueNumber(); i++) {
        if (st.isConstant(i)) {
          TypeVariable v = getVariable(i);
          v.setType(getConstantType(i));
        }
      }
    }

    for (Iterator<SSAInstruction> it = ir.iterateNormalInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();
      if (s instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
        TypeVariable v = getVariable(call.getException());
        Collection<TypeReference> defaultExceptions = call.getExceptionTypes();
        if (Assertions.verifyAssertions) {
          Assertions._assert(defaultExceptions.size() == 1);
        }
        // t should be NullPointerException
        TypeReference t = defaultExceptions.iterator().next();
        IClass klass = cha.lookupClass(t);
        if (Assertions.verifyAssertions) {
          Assertions._assert(klass != null);
        }
        v.setType(new PointType(klass));

        IMethod m = cha.resolveMethod(call.getDeclaredTarget());
        if (m != null) {
          TypeReference[] x = null;
          try {
            x = m.getDeclaredExceptions();
          } catch (InvalidClassFileException e) {
            e.printStackTrace();
            Assertions.UNREACHABLE();
          }
          if (x != null) {
            for (int i = 0; i < x.length; i++) {
              TypeReference tx = x[i];
              IClass tc = cha.lookupClass(tx);
              if (tc != null) {
                v.setType(v.getType().meet(new ConeType(tc)));
              }
            }
          }
        }
      }
    }
  }

  @Override
  protected void initializeWorkList() {
    addAllStatementsToWorkList();
  }

  /**
   * An operator which initializes a type to a declared type.
   */
  protected final class DeclaredTypeOperator extends NullaryOperator<TypeVariable> {
    private final TypeAbstraction type;

    public DeclaredTypeOperator(TypeAbstraction type) {
      this.type = type;
    }

    /**
     * Note that we need evaluate this operator at most once
     */
    @Override
    public byte evaluate(TypeVariable lhs) {
      if (lhs.type.equals(type)) {
        return NOT_CHANGED_AND_FIXED;
      } else {
        lhs.setType(type);
        return CHANGED_AND_FIXED;
      }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "delared type := " + type;
    }

    public boolean isNullary() {
      return true;
    }

    @Override
    public int hashCode() {
      return 9931 * type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DeclaredTypeOperator) {
        DeclaredTypeOperator d = (DeclaredTypeOperator) o;
        return type.equals(d.type);
      } else {
        return false;
      }
    }
  }

  private static final class PhiOperator extends AbstractOperator<TypeVariable> {

    private PhiOperator() {
    }

    /**
     * TODO: work on efficiency shortcuts for this.
     */
    @Override
    public byte evaluate(TypeVariable lhs, IVariable[] rhs) {
      TypeAbstraction lhsType = lhs.getType();
      TypeAbstraction meet = TypeAbstraction.TOP;
      for (int i = 0; i < rhs.length; i++) {
        if (rhs[i] != null) {
          TypeVariable r = (TypeVariable) rhs[i];
          meet = meet.meet(r.getType());
        }
      }
      if (lhsType.equals(meet)) {
        return NOT_CHANGED;
      } else {
        lhs.setType(meet);
        return CHANGED;
      }
    }

    @Override
    public String toString() {
      return "phi meet";
    }

    @Override
    public int hashCode() {
      return 9929;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof PhiOperator);
    }
  }

  private static final class PiOperator extends AbstractOperator<TypeVariable> {

    private PiOperator() {
    }

    /**
     * TODO: work on efficiency shortcuts for this.
     */
    @Override
    public byte evaluate(TypeVariable lhs, IVariable[] rhsOperands) {
      TypeAbstraction lhsType = lhs.getType();

      TypeVariable rhs = (TypeVariable) rhsOperands[0];
      TypeAbstraction rhsType = rhs.getType();

      if (lhsType.equals(rhsType)) {
        return NOT_CHANGED;
      } else {
        lhs.setType(rhsType);
        return CHANGED;
      }
    }

    @Override
    public String toString() {
      return "pi";
    }

    @Override
    public int hashCode() {
      return 9929 * 13;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof PiOperator);
    }
  }

  protected static class PrimitivePropagateOperator extends AbstractOperator<TypeVariable> {

    protected PrimitivePropagateOperator() {
    }

    @Override
    public byte evaluate(TypeVariable lhs, IVariable[] rhs) {
      TypeAbstraction lhsType = lhs.getType();
      TypeAbstraction meet = TypeAbstraction.TOP;
      for (int i = 0; i < rhs.length; i++) {
        if (rhs[i] != null) {
          TypeVariable r = (TypeVariable) rhs[i];
          meet = meet.meet(r.getType());
        }
      }
      if (lhsType.equals(meet)) {
        return NOT_CHANGED;
      } else {
        lhs.setType(meet);
        return CHANGED;
      }
    }

    @Override
    public String toString() {
      return "propagate";
    }

    @Override
    public int hashCode() {
      return 99292;
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o.getClass().equals(getClass());
    }
  }

  /**
   * This operator will extract the element type from an arrayref in an array
   * access instruction
   * 
   * TODO: why isn't this a nullary operator?
   */
  private final class GetElementType extends AbstractOperator<TypeVariable> {
    private final SSAArrayLoadInstruction load;

    GetElementType(SSAArrayLoadInstruction load) {
      this.load = load;
    }

    @Override
    public byte evaluate(TypeVariable lhs, IVariable[] rhs) {
      TypeAbstraction arrayType = getType(load.getArrayRef());
      if (arrayType.equals(TypeAbstraction.TOP)) {
        return NOT_CHANGED;
      }
      TypeReference elementType = null;

      if (arrayType instanceof PointType) {
        elementType = ((PointType) arrayType).getType().getReference().getArrayElementType();
      } else if (arrayType instanceof ConeType) {
        elementType = ((ConeType) arrayType).getType().getReference().getArrayElementType();
      } else {
        Assertions.UNREACHABLE("Unexpected type " + arrayType.getClass());
      }
      if (elementType.isPrimitiveType()) {
        if (doPrimitives && lhs.getType() == TypeAbstraction.TOP) {
          lhs.setType(PrimitiveType.getPrimitive(elementType));
          return CHANGED;
        }
        return NOT_CHANGED;
      }

      if (lhs.getType() != TypeAbstraction.TOP) {
        TypeReference tType = null;
        if (lhs.getType() instanceof PointType) {
          tType = ((PointType) lhs.getType()).getType().getReference();
        } else if (lhs.getType() instanceof ConeType) {
          tType = ((ConeType) lhs.getType()).getType().getReference();
        } else {
          Assertions.UNREACHABLE("Unexpected type " + lhs.getType().getClass());
        }
        if (tType.equals(elementType)) {
          return NOT_CHANGED;
        } else {
          IClass klass = cha.lookupClass(elementType);
          if (Assertions.verifyAssertions) {
            Assertions._assert(klass != null);
          }
          lhs.setType(new ConeType(klass));
          return CHANGED;
        }
      } else {
        IClass klass = cha.lookupClass(elementType);
        if (klass != null) {
          lhs.setType(new ConeType(klass));
        } else {
          lhs.setType(TypeAbstraction.TOP);
        }

        return CHANGED;
      }
    }

    @Override
    public String toString() {
      return "getElementType " + load;
    }

    @Override
    public int hashCode() {
      return 9923 * load.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof GetElementType) {
        GetElementType other = (GetElementType) o;
        return load.equals(other.load);
      } else {
        return false;
      }
    }
  }

  protected class TypeOperatorFactory extends SSAInstruction.Visitor implements OperatorFactory<TypeVariable> {

    protected AbstractOperator<TypeVariable> result = null;

    public AbstractOperator<TypeVariable> get(SSAInstruction instruction) {
      instruction.visit(this);
      AbstractOperator<TypeVariable> temp = result;
      result = null;
      return temp;
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      result = new GetElementType(instruction);
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
      if (!doPrimitives) {
        result = null;
      } else {
        result = new DeclaredTypeOperator(PrimitiveType.INT);
      }
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      TypeReference type = instruction.getDeclaredFieldType();

      if (doPrimitives && type.isPrimitiveType()) {
        result = new DeclaredTypeOperator(PrimitiveType.getPrimitive(type));
      } else {
        IClass klass = cha.lookupClass(type);
        if (klass == null) {
          // get from a field of a type that cannot be loaded.
          // be pessimistic
          result = new DeclaredTypeOperator(BOTTOM);
        } else {
          result = new DeclaredTypeOperator(new ConeType(klass));
        }
      }
    }

    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {
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
      } else if (doPrimitives && type.isPrimitiveType()) {
        result = new DeclaredTypeOperator(PrimitiveType.getPrimitive(type));
      } else {
        result = null;
      }
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      TypeReference type = instruction.getConcreteType();
      IClass klass = cha.lookupClass(type);
      if (klass == null) {
        // a type that cannot be loaded.
        // be pessimistic
        result = new DeclaredTypeOperator(BOTTOM);
      } else {
        result = new DeclaredTypeOperator(new PointType(klass));
      }
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
      TypeReference type = instruction.getDeclaredResultType();
      IClass klass = cha.lookupClass(type);
      if (klass == null) {
        // a type that cannot be loaded.
        // be pessimistic
        result = new DeclaredTypeOperator(BOTTOM);
      } else {
        result = new DeclaredTypeOperator(new ConeType(klass));
      }
    }

    @Override
    public void visitConversion(SSAConversionInstruction instruction) {
      if (doPrimitives) {
        result = new DeclaredTypeOperator(PrimitiveType.getPrimitive(instruction.getToType()));
      }
    }

    @Override
    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
      if (doPrimitives) {
        result = primitivePropagateOp;
      }
    }

    @Override
    public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
      if (doPrimitives) {
        result = primitivePropagateOp;
      }
    }

    @Override
    public void visitInstanceof(SSAInstanceofInstruction instruction) {
      if (doPrimitives) {
        result = new DeclaredTypeOperator(PrimitiveType.BOOLEAN);
      }
    }

    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
      TypeAbstraction type = meetDeclaredExceptionTypes(instruction);
      result = new DeclaredTypeOperator(type);
    }

    @Override
    public void visitPhi(SSAPhiInstruction instruction) {
      result = phiOp;
    }

    @Override
    public void visitPi(SSAPiInstruction instruction) {
      result = piOp;
    }

    private TypeAbstraction meetDeclaredExceptionTypes(SSAGetCaughtExceptionInstruction s) {
      ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) ir.getControlFlowGraph().getNode(s.getBasicBlockNumber());
      Iterator it = bb.getCaughtExceptionTypes();
      TypeReference t = (TypeReference) it.next();
      IClass klass = cha.lookupClass(t);
      TypeAbstraction result = null;
      if (klass == null) {
        // a type that cannot be loaded.
        // be pessimistic
        result = BOTTOM;
      } else {
        result = new ConeType(klass);
      }
      while (it.hasNext()) {
        t = (TypeReference) it.next();
        IClass tClass = cha.lookupClass(t);
        if (tClass == null) {
          result = BOTTOM;
        } else {
          result = result.meet(new ConeType(tClass));
        }
      }
      return result;
    }
  }

  public class TypeVarFactory implements VariableFactory {

    public IVariable makeVariable(int valueNumber) {
      SymbolTable st = ir.getSymbolTable();
      if (doPrimitives) {
        if (st.isConstant(valueNumber)) {
          if (st.isBooleanConstant(valueNumber)) {
            return new TypeVariable(PrimitiveType.BOOLEAN, 797 * valueNumber);
          }
        }
      }

      return new TypeVariable(TypeAbstraction.TOP, 797 * valueNumber);
    }

  }

  public IR getIR() {
    return ir;
  }

  /**
   * Return the type computed for a particular value number
   */
  public TypeAbstraction getType(int valueNumber) {
    if (valueNumber < 0) {
      throw new IllegalArgumentException("bad value number " + valueNumber);
    }
    if (Assertions.verifyAssertions) {
      if (getVariable(valueNumber) == null) {
        Assertions._assert(getVariable(valueNumber) != null, "null variable for value number " + valueNumber);
      }
    }
    return getVariable(valueNumber).getType();
  }

  public TypeAbstraction getConstantType(int valueNumber) {
    if (ir.getSymbolTable().isStringConstant(valueNumber)) {
      return new PointType(cha.lookupClass(TypeReference.JavaLangString));
    } else {
      return getConstantPrimitiveType(valueNumber);
    }
  }

  public TypeAbstraction getConstantPrimitiveType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (!st.isConstant(valueNumber)) {
      return TypeAbstraction.TOP;
    } else if (st.isIntegerConstant(valueNumber)) {
      return PrimitiveType.INT;
    } else if (st.isFloatConstant(valueNumber)) {
      return PrimitiveType.FLOAT;
    } else if (st.isDoubleConstant(valueNumber)) {
      return PrimitiveType.DOUBLE;
    }
    return TypeAbstraction.TOP;
  }

  public boolean isUndefined(int valueNumber) {
    // TODO: Julian, you seem to be using BOTTOM in the European style.
    // Steve's code assumes American style (god forbid), so what you're getting
    // here
    // is not undefined, but java.lang.Object [NR/EY]
    if (getVariable(valueNumber) == null) {
      return true;
    }
    TypeAbstraction ta = getVariable(valueNumber).getType();
    return ta == BOTTOM || ta.getType() == null;
  }
}
