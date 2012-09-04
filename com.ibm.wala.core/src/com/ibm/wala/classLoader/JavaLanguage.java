/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 */
package com.ibm.wala.classLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.analysis.typeInference.JavaPrimitiveType;
import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction.ClassToken;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAAddressOfInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadIndirectInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAStoreIndirectInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.shrike.Exceptions.MethodResolutionFailure;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warnings;

/**
 * The implementation of {@link Language} which defines Java semantics.
 *
 */
public class JavaLanguage extends LanguageImpl implements BytecodeLanguage, Constants {

  public static class JavaInstructionFactory implements SSAInstructionFactory {
    public SSAArrayLengthInstruction ArrayLengthInstruction(int result, int arrayref) {
      return new SSAArrayLengthInstruction(result, arrayref) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    public SSAArrayLoadInstruction ArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType) {
      return new SSAArrayLoadInstruction(result, arrayref, index, declaredType) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getArrayAccessExceptions();
        }
      };
    }

    public SSAArrayStoreInstruction ArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType) {
      return new SSAArrayStoreInstruction(arrayref, index, value, declaredType) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          if (typeIsPrimitive()) {
            return getArrayAccessExceptions();
          } else {
            return getAaStoreExceptions();
          }
        }
      };
    }

    public SSABinaryOpInstruction BinaryOpInstruction(IBinaryOpInstruction.IOperator operator, boolean overflow, boolean unsigned,
        int result, int val1, int val2, boolean mayBeInteger) {
      assert !overflow;
      assert !unsigned;
      return new SSABinaryOpInstruction(operator, result, val1, val2, mayBeInteger) {

        @Override
        public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
          return insts.BinaryOpInstruction(getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0],
              uses == null ? getUse(0) : uses[0], uses == null ? getUse(1) : uses[1], mayBeIntegerOp());
        }

        @Override
        public Collection<TypeReference> getExceptionTypes() {
          if (isPEI()) {
            return getArithmeticException();
          } else {
            return Collections.emptySet();
          }
        }
      };
    }

    public SSACheckCastInstruction CheckCastInstruction(int result, int val, int[] typeValues, boolean isPEI) {
      throw new UnsupportedOperationException();
    }
       
    public SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference[] types, boolean isPEI) {
       assert types.length == 1;
       assert isPEI;
      return new SSACheckCastInstruction(result, val, types, true) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getClassCastException();
        }
      };
    }
     
    public SSACheckCastInstruction CheckCastInstruction(int result, int val, int typeValue, boolean isPEI) {
      assert isPEI;
      return CheckCastInstruction(result, val, new int[]{ typeValue }, true);
    }

    public SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference type, boolean isPEI) {
      assert isPEI;
      return CheckCastInstruction(result, val, new TypeReference[]{ type }, true);
    }

    public SSAComparisonInstruction ComparisonInstruction(IComparisonInstruction.Operator operator, int result, int val1, int val2) {
      return new SSAComparisonInstruction(operator, result, val1, val2);
    }

    public SSAConditionalBranchInstruction ConditionalBranchInstruction(IConditionalBranchInstruction.IOperator operator,
        TypeReference type, int val1, int val2) {
      return new SSAConditionalBranchInstruction(operator, type, val1, val2);
    }

    public SSAConversionInstruction ConversionInstruction(int result, int val, TypeReference fromType, TypeReference toType,
        boolean overflow) {
      assert !overflow;
      return new SSAConversionInstruction(result, val, fromType, toType) {
        @Override
        public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) throws IllegalArgumentException {
          if (uses != null && uses.length == 0) {
            throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
          }
          return insts.ConversionInstruction(defs == null || defs.length == 0 ? getDef(0) : defs[0], uses == null ? getUse(0)
              : uses[0], getFromType(), getToType(), false);
        }
      };
    }

    public SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
      return new SSAGetCaughtExceptionInstruction(bbNumber, exceptionValueNumber);
    }

    public SSAGetInstruction GetInstruction(int result, FieldReference field) {
      return new SSAGetInstruction(result, field) {
      };
    }

    public SSAGetInstruction GetInstruction(int result, int ref, FieldReference field) {
      return new SSAGetInstruction(result, ref, field) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    public SSAGotoInstruction GotoInstruction() {
      return new SSAGotoInstruction();
    }

    public SSAInstanceofInstruction InstanceofInstruction(int result, int ref, TypeReference checkedType) {
      return new SSAInstanceofInstruction(result, ref, checkedType);
    }

    public SSAInvokeInstruction InvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
      return new SSAInvokeInstruction(result, params, exception, site) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          if (!isStatic()) {
            return getNullPointerException();
          } else {
            return Collections.emptySet();
          }
        }
      };
    }

    public SSAInvokeInstruction InvokeInstruction(int[] params, int exception, CallSiteReference site) {
      return new SSAInvokeInstruction(params, exception, site) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          if (!isStatic()) {
            return getNullPointerException();
          } else {
            return Collections.emptySet();
          }
        }
      };
    }

    public SSAMonitorInstruction MonitorInstruction(int ref, boolean isEnter) {
      return new SSAMonitorInstruction(ref, isEnter) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    public SSANewInstruction NewInstruction(int result, NewSiteReference site) {
      return new SSANewInstruction(result, site) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          if (getNewSite().getDeclaredType().isArrayType()) {
            return getNewArrayExceptions();
          } else {
            return getNewScalarExceptions();
          }
        }
      };
    }

    public SSAPhiInstruction PhiInstruction(int result, int[] params) throws IllegalArgumentException {
      return new SSAPhiInstruction(result, params) {
      };
    }

    public SSAPutInstruction PutInstruction(int ref, int value, FieldReference field) {
      return new SSAPutInstruction(ref, value, field) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    public SSAPutInstruction PutInstruction(int value, FieldReference field) {
      return new SSAPutInstruction(value, field) {
      };
    }

    public SSAReturnInstruction ReturnInstruction() {
      return new SSAReturnInstruction();
    }

    public SSAReturnInstruction ReturnInstruction(int result, boolean isPrimitive) {
      return new SSAReturnInstruction(result, isPrimitive);
    }

    public SSASwitchInstruction SwitchInstruction(int val, int defaultLabel, int[] casesAndLabels) {
      return new SSASwitchInstruction(val, defaultLabel, casesAndLabels);
    }

    public SSAThrowInstruction ThrowInstruction(int exception) {
      return new SSAThrowInstruction(exception) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    public SSAUnaryOpInstruction UnaryOpInstruction(IUnaryOpInstruction.IOperator operator, int result, int val) {
      return new SSAUnaryOpInstruction(operator, result, val);
    }

    public SSALoadMetadataInstruction LoadMetadataInstruction(int lval, TypeReference entityType, Object token) {
      return new SSALoadMetadataInstruction(lval, entityType, token) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return loadClassExceptions;
        }
      };
    }

    public SSANewInstruction NewInstruction(int result, NewSiteReference site, int[] params) {
      return new SSANewInstruction(result, site, params) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNewArrayExceptions();
        }
      };
    }

    public SSAPiInstruction PiInstruction(int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
      return new SSAPiInstruction(result, val, piBlock, successorBlock, cause);
    }

    public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
    }

    public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, int indexVal, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
   }

    public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, FieldReference field, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
    }

    public SSALoadIndirectInstruction LoadIndirectInstruction(int lval, TypeReference t, int addressVal) {
      throw new UnsupportedOperationException();
    }

    public SSAStoreIndirectInstruction StoreIndirectInstruction(int addressVal, int rval, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
    }
  }

  private static final Collection<TypeReference> arrayAccessExceptions = Collections.unmodifiableCollection(Arrays
      .asList(new TypeReference[] { TypeReference.JavaLangNullPointerException,
          TypeReference.JavaLangArrayIndexOutOfBoundsException }));

  private static final Collection<TypeReference> aaStoreExceptions = Collections.unmodifiableCollection(Arrays
      .asList(new TypeReference[] { TypeReference.JavaLangNullPointerException,
          TypeReference.JavaLangArrayIndexOutOfBoundsException, TypeReference.JavaLangArrayStoreException }));

  private static final Collection<TypeReference> newScalarExceptions = Collections.unmodifiableCollection(Arrays
      .asList(new TypeReference[] { TypeReference.JavaLangExceptionInInitializerError, TypeReference.JavaLangOutOfMemoryError }));

  private static final Collection<TypeReference> newArrayExceptions = Collections.unmodifiableCollection(Arrays
      .asList(new TypeReference[] { TypeReference.JavaLangOutOfMemoryError, TypeReference.JavaLangNegativeArraySizeException }));

  private static final Collection<TypeReference> exceptionInInitializerError = Collections
      .singleton(TypeReference.JavaLangExceptionInInitializerError);

  private static final Collection<TypeReference> nullPointerException = Collections
      .singleton(TypeReference.JavaLangNullPointerException);

  private static final Collection<TypeReference> arithmeticException = Collections
      .singleton(TypeReference.JavaLangArithmeticException);

  private static final Collection<TypeReference> classCastException = Collections
      .singleton(TypeReference.JavaLangClassCastException);

  private static final Collection<TypeReference> classNotFoundException = Collections
      .singleton(TypeReference.JavaLangClassNotFoundException);

  private static final Collection<TypeReference> loadClassExceptions = Collections
      .singleton(TypeReference.JavaLangClassNotFoundException);

  public static Collection<TypeReference> getAaStoreExceptions() {
    return aaStoreExceptions;
  }

  public static Collection<TypeReference> getArithmeticException() {
    return arithmeticException;
  }

  public static Collection<TypeReference> getArrayAccessExceptions() {
    return arrayAccessExceptions;
  }

  public static Collection<TypeReference> getClassCastException() {
    return classCastException;
  }

  public static Collection<TypeReference> getClassNotFoundException() {
    return classNotFoundException;
  }

  public static Collection<TypeReference> getNewArrayExceptions() {
    return newArrayExceptions;
  }

  public static Collection<TypeReference> getNewScalarExceptions() {
    return newScalarExceptions;
  }

  public static Collection<TypeReference> getNullPointerException() {
    return nullPointerException;
  }

  public static Collection<TypeReference> getExceptionInInitializerError() {
    return exceptionInInitializerError;
  }

  public Atom getName() {
    return ClassLoaderReference.Java;
  }

  public TypeReference getRootType() {
    return TypeReference.JavaLangObject;
  }

  public TypeReference getThrowableType() {
    return TypeReference.JavaLangThrowable;
  }

  public TypeReference getConstantType(Object o) {
    if (o == null) {
      // TODO: do we really want null here instead of TypeReference.Null?
      // lots of code seems to depend on this being null.
      return null;
    } else if (o instanceof Boolean) {
      return TypeReference.Boolean;
    } else if (o instanceof Long) {
      return TypeReference.Long;
    } else if (o instanceof Double) {
      return TypeReference.Double;
    } else if (o instanceof Float) {
      return TypeReference.Float;
    } else if (o instanceof Number) {
      return TypeReference.Int;
    } else if (o instanceof String) {
      return TypeReference.JavaLangString;
    } else if (o instanceof ClassToken || o instanceof TypeReference) {
      return TypeReference.JavaLangClass;
    } else if (o instanceof IMethod) {
      IMethod m = (IMethod) o;
      return m.isInit() ? TypeReference.JavaLangReflectConstructor : TypeReference.JavaLangReflectMethod;
    } else {
      assert false : "unknown constant " + o + ": " + o.getClass();
      return null;
    }
  }

  public boolean isNullType(TypeReference type) {
    return type == null || type == TypeReference.Null;
  }

  public TypeReference[] getArrayInterfaces() {
    return new TypeReference[] { TypeReference.JavaIoSerializable, TypeReference.JavaLangCloneable };
  }

  public TypeName lookupPrimitiveType(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return Collection<TypeReference>, set of exception types a call to a declared target might throw.
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if target is null
   * @throws IllegalArgumentException if cha is null
   */
  public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
      throws InvalidClassFileException {

    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    if (target == null) {
      throw new IllegalArgumentException("target is null");
    }
    ArrayList<TypeReference> set = new ArrayList<TypeReference>(cha.getJavaLangRuntimeExceptionTypes());
    set.addAll(cha.getJavaLangErrorTypes());

    IClass klass = cha.lookupClass(target.getDeclaringClass());
    if (klass == null) {
      Warnings.add(MethodResolutionFailure.moderate(target));
    }
    if (klass != null) {
      IMethod M = klass.getMethod(target.getSelector());
      if (M == null) {
        Warnings.add(MethodResolutionFailure.severe(target));
      } else {
        TypeReference[] exceptionTypes = M.getDeclaredExceptions();
        if (exceptionTypes != null) {
          set.addAll(Arrays.asList(exceptionTypes));
        }
      }
    }
    return set;
  }

  /**
   * @param pei a potentially-excepting instruction
   * @return the exception types that pei may throw, independent of the class hierarchy. null if none.
   * 
   *         Notes
   *         <ul>
   *         <li>this method will <em>NOT</em> return the exception type explicitly thrown by an athrow
   *         <li>this method will <em>NOT</em> return the exception types that a called method may throw
   *         <li>this method ignores OutOfMemoryError
   *         <li>this method ignores linkage errors
   *         <li>this method ignores IllegalMonitorState exceptions
   *         </ul>
   * 
   * @throws IllegalArgumentException if pei is null
   */
  public Collection<TypeReference> getImplicitExceptionTypes(IInstruction pei) {
    if (pei == null) {
      throw new IllegalArgumentException("pei is null");
    }
    switch (((Instruction) pei).getOpcode()) {
    case OP_iaload:
    case OP_laload:
    case OP_faload:
    case OP_daload:
    case OP_aaload:
    case OP_baload:
    case OP_caload:
    case OP_saload:
    case OP_iastore:
    case OP_lastore:
    case OP_fastore:
    case OP_dastore:
    case OP_bastore:
    case OP_castore:
    case OP_sastore:
      return getArrayAccessExceptions();
    case OP_aastore:
      return getAaStoreExceptions();
    case OP_getfield:
    case OP_putfield:
    case OP_invokevirtual:
    case OP_invokespecial:
    case OP_invokeinterface:
      return getNullPointerException();
    case OP_idiv:
    case OP_irem:
    case OP_ldiv:
    case OP_lrem:
      return getArithmeticException();
    case OP_new:
      return newScalarExceptions;
    case OP_newarray:
    case OP_anewarray:
    case OP_multianewarray:
      return newArrayExceptions;
    case OP_arraylength:
      return getNullPointerException();
    case OP_athrow:
      // N.B: the caller must handle the explicitly-thrown exception
      return getNullPointerException();
    case OP_checkcast:
      return getClassCastException();
    case OP_monitorenter:
    case OP_monitorexit:
      // we're currently ignoring MonitorStateExceptions, since J2EE stuff
      // should be
      // logically single-threaded
      return getNullPointerException();
    case OP_ldc_w:
      if (((ConstantInstruction) pei).getType().equals(TYPE_Class))
        return getClassNotFoundException();
      else
        return null;
    case OP_getstatic:
    case OP_putstatic:
      return getExceptionInInitializerError();
    default:
      return Collections.emptySet();
    }
  }

  public SSAInstructionFactory instructionFactory() {
    return javaShrikeFactory;
  }

  private final static SSAInstructionFactory javaShrikeFactory = new JavaInstructionFactory();

  public boolean isDoubleType(TypeReference type) {
    return type == TypeReference.Double;
  }

  public boolean isFloatType(TypeReference type) {
    return type == TypeReference.Float;
  }

  public boolean isIntType(TypeReference type) {
    return type == TypeReference.Int;
  }

  public boolean isLongType(TypeReference type) {
    return type == TypeReference.Long;
  }
  
  public boolean isVoidType(TypeReference type) {
    return type == TypeReference.Void;
  }

  public boolean isMetadataType(TypeReference type) {
    return type == TypeReference.JavaLangClass;
  }
  
  public boolean isStringType(TypeReference type) {
    return type == TypeReference.JavaLangString;
  }
  
  public boolean isBooleanType(TypeReference type) {
    return type == TypeReference.Boolean;
  }
  
  public boolean isCharType(TypeReference type) {
    return type == TypeReference.Char;
  }

  public Object getMetadataToken(Object value) {
    if (value instanceof ClassToken) {
      return ShrikeUtil.makeTypeReference(ClassLoaderReference.Primordial, ((ClassToken) value).getTypeName());
    } else {
      assert value instanceof TypeReference;
      return value;
    }
  }

  public TypeReference getPointerType(TypeReference pointee) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java does not permit explicit pointers");
  }

  public TypeReference getMetadataType() {
    return TypeReference.JavaLangClass;
  }

  public TypeReference getStringType() {
    return TypeReference.JavaLangString;
  }
  
  {
    JavaPrimitiveType.init();
  }
  
  @SuppressWarnings("static-access")
  public PrimitiveType getPrimitive(TypeReference reference) {
    return JavaPrimitiveType.getPrimitive(reference);
  }
}
