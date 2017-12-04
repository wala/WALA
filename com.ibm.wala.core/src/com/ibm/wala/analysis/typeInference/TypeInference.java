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
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.dataflow.ssa.SSAInference;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IVisitorWithAddresses;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAddressOfInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadIndirectInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAStoreIndirectInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class performs intraprocedural type propagation on an SSA IR.
 */
public class TypeInference extends SSAInference<TypeVariable> implements FixedPointConstants {

  private static final boolean DEBUG = false;

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
  
  final protected Language language;

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
    this.language = ir.getMethod().getDeclaringClass().getClassLoader().getLanguage();
    this.cha = ir.getMethod().getDeclaringClass().getClassHierarchy();
    this.ir = ir;
    this.doPrimitives = doPrimitives;
    this.BOTTOM = new ConeType(cha.getRootClass());
    initialize();
    solve();
  }

  public boolean solve() {
    return solve(null);
  }

  @Override
  public boolean solve(IProgressMonitor monitor) {
    try {
      if (solved) {
        return false;
      } else {
        boolean result = super.solve(null);
        solved = true;
        return result;
      }
    } catch (CancelException e) {
      throw new CancelRuntimeException(e);
    }
  }

  protected void initialize() {
    init(ir, this.new TypeVarFactory(), this.new TypeOperatorFactory());
  }

  @Override
  protected void initializeVariables() {

    if (DEBUG) {
      System.err.println("initializeVariables " + ir.getMethod());
    }

    int[] parameterValueNumbers = ir.getParameterValueNumbers();
    for (int i = 0; i < parameterValueNumbers.length; i++) {
      TypeVariable v = getVariable(parameterValueNumbers[i]);
      TypeReference t = ir.getParameterType(i);

      if (DEBUG) {
        System.err.println("parameter " + parameterValueNumbers[i] + " " + t);
      }

      if (t.isReferenceType()) {
        IClass klass = cha.lookupClass(t);
        if (DEBUG) {
          System.err.println("klass " + klass);
        }
        if (klass != null) {
          v.setType(new ConeType(klass));
        } else {
          // give up .. default to java.lang.Object (BOTTOM)
          v.setType(BOTTOM);
        }
      } else if (doPrimitives) {
        v.setType(language.getPrimitive(t));
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

    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
      if (s instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
        TypeVariable v = getVariable(call.getException());
        Collection<TypeReference> defaultExceptions = call.getExceptionTypes();
        if (defaultExceptions.size() == 0) {
          continue;
        }

        Iterator<TypeReference> types = defaultExceptions.iterator();
        TypeReference t = types.next();
        IClass klass = cha.lookupClass(t);
        if (klass == null) {
          v.setType(BOTTOM);
        } else {
          v.setType(new PointType(klass));
        }
        
        while(types.hasNext()) {
          t = types.next();
          klass = cha.lookupClass(t);
          if (klass != null) {
            v.setType(v.getType().meet(new PointType(klass)));
          }        
        }

        IMethod m = cha.resolveMethod(call.getDeclaredTarget());
        if (m != null) {
          TypeReference[] x = null;
          try {
            x = m.getDeclaredExceptions();
          } catch (InvalidClassFileException e) {
            e.printStackTrace();
            Assertions.UNREACHABLE();
          } catch (UnsupportedOperationException e) {
            x = new TypeReference[]{ language.getThrowableType() };
          }
          if (x != null) {
            for (TypeReference tx : x) {
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
  protected static final class DeclaredTypeOperator extends NullaryOperator<TypeVariable> {
    private final TypeAbstraction type;

    public DeclaredTypeOperator(TypeAbstraction type) {
      assert type != null;
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

    public static boolean isNullary() {
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
    public byte evaluate(TypeVariable lhs, TypeVariable[] rhs) {

      if (DEBUG) {
        System.err.print("PhiOperator.meet " + lhs + " ");
        for (IVariable v : rhs) {
          System.err.print(v + " ");
        }
        System.err.println();
      }

      TypeAbstraction lhsType = lhs.getType();
      TypeAbstraction meet = TypeAbstraction.TOP;
      for (TypeVariable r : rhs) {
        if (r != null && r.getType() != null) {
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
    public byte evaluate(TypeVariable lhs, TypeVariable[] rhsOperands) {
      TypeAbstraction lhsType = lhs.getType();

      TypeVariable rhs = rhsOperands[0];
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
    public byte evaluate(TypeVariable lhs, TypeVariable[] rhs) {
      TypeAbstraction lhsType = lhs.getType();
      TypeAbstraction meet = TypeAbstraction.TOP;
      for (TypeVariable r : rhs) {
        if (r != null  && r.getType() != null) {
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
   * This operator will extract the element type from an arrayref in an array access instruction
   * 
   * TODO: why isn't this a nullary operator?
   */
  private final class GetElementType extends AbstractOperator<TypeVariable> {
    private final SSAArrayLoadInstruction load;

    GetElementType(SSAArrayLoadInstruction load) {
      this.load = load;
    }

    @Override
    public byte evaluate(TypeVariable lhs, TypeVariable[] rhs) {
      TypeAbstraction arrayType = getType(load.getArrayRef());
      if (arrayType == null || arrayType.equals(TypeAbstraction.TOP)) {
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
          assert klass != null;
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

  protected class TypeOperatorFactory extends SSAInstruction.Visitor implements IVisitorWithAddresses, OperatorFactory<TypeVariable> {

    protected AbstractOperator<TypeVariable> result = null;

    @Override
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
        result = new DeclaredTypeOperator(language.getPrimitive(language.getConstantType(new Integer(1))));
      }
    }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
      IClass jlClassKlass = cha.lookupClass(instruction.getType());
      assert jlClassKlass != null;
      result = new DeclaredTypeOperator(new ConeType(jlClassKlass));
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      TypeReference type = instruction.getDeclaredFieldType();

      if (doPrimitives && type.isPrimitiveType()) {
        PrimitiveType p = language.getPrimitive(type);
        assert p != null : "no type for " + type;
        result = new DeclaredTypeOperator(p);
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
        result = new DeclaredTypeOperator(language.getPrimitive(type));
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
      TypeAbstraction typeAbs = null;
      for (TypeReference type : instruction.getDeclaredResultTypes()) {
        IClass klass = cha.lookupClass(type);
        if (klass == null) {
          // a type that cannot be loaded.
          // be pessimistic
          typeAbs = BOTTOM;
        } else {
          TypeAbstraction x = null;
          if (doPrimitives && type.isPrimitiveType()) {
            x = language.getPrimitive(type);
          } else if (type.isReferenceType()) {
            x = new ConeType(klass);
          }
          if (x != null) {
            if (typeAbs == null) {
              typeAbs = x;
            } else {
              typeAbs = typeAbs.meet(x);           
            }
          }
        }
      }
      
      result = new DeclaredTypeOperator(typeAbs);
    }

    @Override
    public void visitConversion(SSAConversionInstruction instruction) {
      if (doPrimitives) {
        result = new DeclaredTypeOperator(language.getPrimitive(instruction.getToType()));
      }
    }

    @Override
    public void visitComparison(SSAComparisonInstruction instruction) {
      if (doPrimitives) {
        result = new DeclaredTypeOperator(language.getPrimitive(language.getConstantType(new Integer(0))));
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
        result = new DeclaredTypeOperator(language.getPrimitive(language.getConstantType(Boolean.TRUE)));
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
      Iterator<TypeReference> it = bb.getCaughtExceptionTypes();
      TypeReference t = it.next();
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
        t = it.next();
        IClass tClass = cha.lookupClass(t);
        if (tClass == null) {
          result = BOTTOM;
        } else {
          result = result.meet(new ConeType(tClass));
        }
      }
      return result;
    }

    private DeclaredTypeOperator getPointerTypeOperator(TypeReference type) {
      if (type.isPrimitiveType()) {
        return new DeclaredTypeOperator(language.getPrimitive(type));
      } else {
        IClass klass = cha.lookupClass(type);
        if (klass == null) {
          // a type that cannot be loaded.
          // be pessimistic
          return new DeclaredTypeOperator(BOTTOM);
        } else {
          return new DeclaredTypeOperator(new ConeType(klass));
        }
      }
    }
    
    @Override
    public void visitAddressOf(SSAAddressOfInstruction instruction) {
      TypeReference type = language.getPointerType(instruction.getType());
      result = getPointerTypeOperator(type);
     }

    @Override
    public void visitLoadIndirect(SSALoadIndirectInstruction instruction) {
      result = getPointerTypeOperator(instruction.getLoadedType());
    }

    @Override
    public void visitStoreIndirect(SSAStoreIndirectInstruction instruction) {
      Assertions.UNREACHABLE();
    }
  }

  public class TypeVarFactory implements VariableFactory {

    @Override
    public IVariable makeVariable(int valueNumber) {
      if (doPrimitives) {
        SymbolTable st = ir.getSymbolTable();
        if (st.isConstant(valueNumber)) {
          if (st.isBooleanConstant(valueNumber)) {
            return new TypeVariable(language.getPrimitive(language.getConstantType(Boolean.TRUE)));
          }
        }
      }

      return new TypeVariable(TypeAbstraction.TOP);
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
    TypeVariable variable = getVariable(valueNumber);
    assert variable != null : "null variable for value number " + valueNumber;
    return variable.getType();
  }

  public TypeAbstraction getConstantType(int valueNumber) {
    if (ir.getSymbolTable().isStringConstant(valueNumber)) {
      return new PointType(cha.lookupClass(language.getStringType()));
    } else {
      return getConstantPrimitiveType(valueNumber);
    }
  }

  public TypeAbstraction getConstantPrimitiveType(int valueNumber) {
    SymbolTable st = ir.getSymbolTable();
    if (!st.isConstant(valueNumber) || st.isNullConstant(valueNumber)) {
      return TypeAbstraction.TOP;
    } else {
      return language.getPrimitive(language.getConstantType(st.getConstantValue(valueNumber)));
    }
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

  /**
   * Extract all results of the type inference analysis.
   * 
   * @return an array, where the i'th variable holds the type abstraction of the i'th value number.
   */
  public TypeAbstraction[] extractAllResults() {
    int numberOfVars = ir.getSymbolTable().getMaxValueNumber() + 1;
    TypeAbstraction[] ret = new TypeAbstraction[numberOfVars];

    for (int i = 0; i < numberOfVars; ++i) {
      TypeVariable var = getVariable(i);
      ret[i] = var == null ? null : var.getType();
    }

    return ret;
  }

  @Override
  protected TypeVariable[] makeStmtRHS(int size) {
    return new TypeVariable[size];
  }
}
