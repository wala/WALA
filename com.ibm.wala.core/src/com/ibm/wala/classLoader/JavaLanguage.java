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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ConstantPoolParser.ReferenceToken;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAAbstractBinaryInstruction;
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
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
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
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
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
    @Override
    public SSAArrayLengthInstruction ArrayLengthInstruction(int iindex, int result, int arrayref) {
      return new SSAArrayLengthInstruction(iindex, result, arrayref) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    @Override
    public SSAArrayLoadInstruction ArrayLoadInstruction(int iindex, int result, int arrayref, int index, TypeReference declaredType) {
      return new SSAArrayLoadInstruction(iindex, result, arrayref, index, declaredType) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getArrayAccessExceptions();
        }
      };
    }

    @Override
    public SSAArrayStoreInstruction ArrayStoreInstruction(int iindex, int arrayref, int index, int value, TypeReference declaredType) {
      return new SSAArrayStoreInstruction(iindex, arrayref, index, value, declaredType) {
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

    @Override
    public SSAAbstractBinaryInstruction BinaryOpInstruction(int iindex, IBinaryOpInstruction.IOperator operator, boolean overflow, boolean unsigned,
        int result, int val1, int val2, boolean mayBeInteger) {
      assert !overflow;
      // assert (!unsigned) : "BinaryOpInstuction: unsigned disallowed! iIndex: " + iindex + ", operation: " + val1 + " " + operator.toString() + " " + val2 ;
      return new SSABinaryOpInstruction(iindex, operator, result, val1, val2, mayBeInteger) {

        @Override
        public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
          return insts.BinaryOpInstruction(iindex, getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0],
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

    @Override
    public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, int[] typeValues, boolean isPEI) {
      throw new UnsupportedOperationException();
    }
       
    @Override
    public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, TypeReference[] types, boolean isPEI) {
       assert types.length == 1;
       assert isPEI;
      return new SSACheckCastInstruction(iindex, result, val, types, true) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getClassCastException();
        }
      };
    }
     
    @Override
    public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, int typeValue, boolean isPEI) {
      assert isPEI;
      return CheckCastInstruction(iindex, result, val, new int[]{ typeValue }, true);
    }

    @Override
    public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, TypeReference type, boolean isPEI) {
      assert isPEI;
      return CheckCastInstruction(iindex, result, val, new TypeReference[]{ type }, true);
    }

    @Override
    public SSAComparisonInstruction ComparisonInstruction(int iindex, IComparisonInstruction.Operator operator, int result, int val1, int val2) {
      return new SSAComparisonInstruction(iindex, operator, result, val1, val2);
    }

    @Override
    public SSAConditionalBranchInstruction ConditionalBranchInstruction(int iindex, IConditionalBranchInstruction.IOperator operator,
        TypeReference type, int val1, int val2, int target) {
      return new SSAConditionalBranchInstruction(iindex, operator, type, val1, val2, target);
    }

    @Override
    public SSAConversionInstruction ConversionInstruction(int iindex, int result, int val, TypeReference fromType, TypeReference toType,
        boolean overflow) {
      assert !overflow;
      return new SSAConversionInstruction(iindex, result, val, fromType, toType) {
        @Override
        public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) throws IllegalArgumentException {
          if (uses != null && uses.length == 0) {
            throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
          }
          return insts.ConversionInstruction(iindex, defs == null || defs.length == 0 ? getDef(0) : defs[0], uses == null ? getUse(0)
              : uses[0], getFromType(), getToType(), false);
        }
      };
    }

    @Override
    public SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int iindex, int bbNumber, int exceptionValueNumber) {
      return new SSAGetCaughtExceptionInstruction(iindex, bbNumber, exceptionValueNumber);
    }

    @Override
    public SSAGetInstruction GetInstruction(int iindex, int result, FieldReference field) {
      return new SSAGetInstruction(iindex, result, field) {
      };
    }

    @Override
    public SSAGetInstruction GetInstruction(int iindex, int result, int ref, FieldReference field) {
      return new SSAGetInstruction(iindex, result, ref, field) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    @Override
    public SSAGotoInstruction GotoInstruction(int iindex, int target) {
      return new SSAGotoInstruction(iindex, target);
    }

    @Override
    public SSAInstanceofInstruction InstanceofInstruction(int iindex, int result, int ref, TypeReference checkedType) {
      return new SSAInstanceofInstruction(iindex, result, ref, checkedType);
    }

    @Override
    public SSAInvokeInstruction InvokeInstruction(int iindex, int result, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
      if (bootstrap != null) {
        return new SSAInvokeDynamicInstruction(iindex, result, params, exception, site, bootstrap) {
          @Override
          public Collection<TypeReference> getExceptionTypes() {
            if (!isStatic()) {
              return getNullPointerException();
            } else {
              return Collections.emptySet();
            }
          }
        };
      } else {
        return new SSAInvokeInstruction(iindex, result, params, exception, site) {
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
    }

    @Override
    public SSAInvokeInstruction InvokeInstruction(int iindex, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
      if (bootstrap != null) {
        return new SSAInvokeDynamicInstruction(iindex, params, exception, site, bootstrap) {
          @Override
          public Collection<TypeReference> getExceptionTypes() {
            if (!isStatic()) {
              return getNullPointerException();
            } else {
              return Collections.emptySet();
            }
          }
        };
      } else {
        return new SSAInvokeInstruction(iindex, params, exception, site) {
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
    }

    @Override
    public SSAMonitorInstruction MonitorInstruction(int iindex, int ref, boolean isEnter) {
      return new SSAMonitorInstruction(iindex, ref, isEnter) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    @Override
    public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site) {
      return new SSANewInstruction(iindex, result, site) {
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

    @Override
    public SSAPhiInstruction PhiInstruction(int iindex, int result, int[] params) throws IllegalArgumentException {
      return new SSAPhiInstruction(iindex, result, params) {
      };
    }

    @Override
    public SSAPutInstruction PutInstruction(int iindex, int ref, int value, FieldReference field) {
      return new SSAPutInstruction(iindex, ref, value, field) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    @Override
    public SSAPutInstruction PutInstruction(int iindex, int value, FieldReference field) {
      return new SSAPutInstruction(iindex, value, field) {
      };
    }

    @Override
    public SSAReturnInstruction ReturnInstruction(int iindex) {
      return new SSAReturnInstruction(iindex);
    }

    @Override
    public SSAReturnInstruction ReturnInstruction(int iindex, int result, boolean isPrimitive) {
      return new SSAReturnInstruction(iindex, result, isPrimitive);
    }

    @Override
    public SSASwitchInstruction SwitchInstruction(int iindex, int val, int defaultLabel, int[] casesAndLabels) {
      return new SSASwitchInstruction(iindex, val, defaultLabel, casesAndLabels);
    }

    @Override
    public SSAThrowInstruction ThrowInstruction(int iindex, int exception) {
      return new SSAThrowInstruction(iindex, exception) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNullPointerException();
        }
      };
    }

    @Override
    public SSAUnaryOpInstruction UnaryOpInstruction(int iindex, IUnaryOpInstruction.IOperator operator, int result, int val) {
      return new SSAUnaryOpInstruction(iindex, operator, result, val);
    }

    @Override
    public SSALoadMetadataInstruction LoadMetadataInstruction(int iindex, int lval, TypeReference entityType, Object token) {
      return new SSALoadMetadataInstruction(iindex, lval, entityType, token) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return loadClassExceptions;
        }
      };
    }

    @Override
    public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site, int[] params) {
      return new SSANewInstruction(iindex, result, site, params) {
        @Override
        public Collection<TypeReference> getExceptionTypes() {
          return getNewArrayExceptions();
        }
      };
    }

    @Override
    public SSAPiInstruction PiInstruction(int iindex, int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
      return new SSAPiInstruction(iindex, result, val, piBlock, successorBlock, cause);
    }

    @Override
    public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, int indexVal, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
   }

    @Override
    public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, FieldReference field, TypeReference pointeeType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SSALoadIndirectInstruction LoadIndirectInstruction(int iindex, int lval, TypeReference t, int addressVal) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SSAStoreIndirectInstruction StoreIndirectInstruction(int iindex, int addressVal, int rval, TypeReference pointeeType) {
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

  private static final Collection<TypeReference> newSafeArrayExceptions = Collections.unmodifiableCollection(Arrays
      .asList(new TypeReference[] { TypeReference.JavaLangOutOfMemoryError}));

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
  public static Collection<TypeReference> getNewSafeArrayExceptions() {
    return newSafeArrayExceptions;
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

  @Override
  public Atom getName() {
    return ClassLoaderReference.Java;
  }

  @Override
  public TypeReference getRootType() {
    return TypeReference.JavaLangObject;
  }

  @Override
  public TypeReference getThrowableType() {
    return TypeReference.JavaLangThrowable;
  }

  @Override
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
    } else if (o instanceof MethodHandle || o instanceof ReferenceToken) {
      return TypeReference.JavaLangInvokeMethodHandle;
    } else if (o instanceof MethodType) {
      return TypeReference.JavaLangInvokeMethodType;
    } else {
      assert false : "unknown constant " + o + ": " + o.getClass();
      return null;
    }
  }

  @Override
  public boolean isNullType(TypeReference type) {
    return type == null || type == TypeReference.Null;
  }

  @Override
  public TypeReference[] getArrayInterfaces() {
    return new TypeReference[] { TypeReference.JavaIoSerializable, TypeReference.JavaLangCloneable };
  }

  @Override
  public TypeName lookupPrimitiveType(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return {@link Collection}&lt;{@link TypeReference}&gt;, set of exception types a call to a declared target might throw.
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if target is null
   * @throws IllegalArgumentException if cha is null
   */
  @Override
  public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
      throws InvalidClassFileException {

    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    if (target == null) {
      throw new IllegalArgumentException("target is null");
    }
    ArrayList<TypeReference> set = new ArrayList<>(cha.getJavaLangRuntimeExceptionTypes());
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
  @Override
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

  @Override
  public SSAInstructionFactory instructionFactory() {
    return javaShrikeFactory;
  }

  private final static SSAInstructionFactory javaShrikeFactory = new JavaInstructionFactory();

  @Override
  public boolean isDoubleType(TypeReference type) {
    return type == TypeReference.Double;
  }

  @Override
  public boolean isFloatType(TypeReference type) {
    return type == TypeReference.Float;
  }

  @Override
  public boolean isIntType(TypeReference type) {
    return type == TypeReference.Int;
  }

  @Override
  public boolean isLongType(TypeReference type) {
    return type == TypeReference.Long;
  }
  
  @Override
  public boolean isVoidType(TypeReference type) {
    return type == TypeReference.Void;
  }

  @Override
  public boolean isMetadataType(TypeReference type) {
    return type == TypeReference.JavaLangClass ||
        type == TypeReference.JavaLangInvokeMethodHandle ||
        type == TypeReference.JavaLangInvokeMethodType;
  }
  
  @Override
  public boolean isStringType(TypeReference type) {
    return type == TypeReference.JavaLangString;
  }
  
  @Override
  public boolean isBooleanType(TypeReference type) {
    return type == TypeReference.Boolean;
  }
  
  @Override
  public boolean isCharType(TypeReference type) {
    return type == TypeReference.Char;
  }

  @Override
  public Object getMetadataToken(Object value) {
    if (value instanceof ClassToken) {
      return ShrikeUtil.makeTypeReference(ClassLoaderReference.Application, ((ClassToken) value).getTypeName());
    } else if (value instanceof ReferenceToken) {
      ReferenceToken tok = (ReferenceToken)value;
      TypeReference cls = ShrikeUtil.makeTypeReference(ClassLoaderReference.Application, "L" + tok.getClassName());
      return MethodReference.findOrCreate(cls, new Selector(Atom.findOrCreateUnicodeAtom(tok.getElementName()), Descriptor.findOrCreateUTF8(tok.getDescriptor())));
    } else if (value instanceof MethodHandle || value instanceof MethodType) {
      return value;
    } else {
      assert value instanceof TypeReference;
      return value;
    }
  }

  @Override
  public TypeReference getPointerType(TypeReference pointee) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java does not permit explicit pointers");
  }

  @Override
  public TypeReference getStringType() {
    return TypeReference.JavaLangString;
  }
  
  {
    JavaPrimitiveType.init();
  }
  
  @Override
  @SuppressWarnings("static-access")
  public PrimitiveType getPrimitive(TypeReference reference) {
    return JavaPrimitiveType.getPrimitive(reference);
  }

  @Override
  public MethodReference getInvokeMethodReference(ClassLoaderReference loader, IInvokeInstruction instruction) {
    return MethodReference.findOrCreate(this, loader, instruction.getClassType(), instruction.getMethodName(),
        instruction.getMethodSignature());
  }

  @Override
  public boolean methodsHaveDeclaredParameterTypes() {
    return true;
  }
}
