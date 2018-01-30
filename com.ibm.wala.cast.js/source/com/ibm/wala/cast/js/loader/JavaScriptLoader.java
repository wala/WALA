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
package com.ibm.wala.cast.js.loader;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.js.analysis.typeInference.JSPrimitiveType;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.cast.js.translator.JSAstTranslator;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.LanguageImpl;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IComparisonInstruction.Operator;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
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
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class JavaScriptLoader extends CAstAbstractModuleLoader {

  public final static Language JS = new LanguageImpl() {

    {
      JSPrimitiveType.init();
    }

    @Override
    public Atom getName() {
      return Atom.findOrCreateUnicodeAtom("JavaScript");
    }

    @Override
    public TypeReference getRootType() {
      return JavaScriptTypes.Root;
    }

    @Override
    public TypeReference getThrowableType() {
      return JavaScriptTypes.Root;
    }

    @Override
    public TypeReference getConstantType(Object o) {
      if (o == null) {
        return JavaScriptTypes.Null;
      } else {
        Class<?> c = o.getClass();
        if (c == Boolean.class) {
          return JavaScriptTypes.Boolean;
        } else if (c == String.class) {
          return JavaScriptTypes.String;
        } else if (c == Integer.class) {
          return JavaScriptTypes.Number;
        } else if (c == Float.class) {
          return JavaScriptTypes.Number;
        } else if (c == Double.class) {
          return JavaScriptTypes.Number;
        } else {
          assert false : "cannot determine type for " + o + " of class " + c;
          return null;
        }
      }
    }

    @Override
    public boolean isNullType(TypeReference type) {
      return type.equals(JavaScriptTypes.Undefined) || type.equals(JavaScriptTypes.Null);
    }

    @Override
    public TypeReference[] getArrayInterfaces() {
      return new TypeReference[0];
    }

    @Override
    public TypeName lookupPrimitiveType(String name) {
      if ("Boolean".equals(name)) {
        return JavaScriptTypes.Boolean.getName();
      } else if ("Number".equals(name)) {
        return JavaScriptTypes.Number.getName();
      } else if ("String".equals(name)) {
        return JavaScriptTypes.String.getName();
      } else if ("Date".equals(name)) {
        return JavaScriptTypes.Date.getName();
      } else {
        assert "RegExp".equals(name);
        return JavaScriptTypes.RegExp.getName();
      }
    }

    @Override
    public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
        throws InvalidClassFileException {
      return Collections.singleton(JavaScriptTypes.Root);
    }

    @Override
    public Object getMetadataToken(Object value) {
      assert false;
      return null;
    }

    @Override
    public TypeReference getPointerType(TypeReference pointee) throws UnsupportedOperationException {
      throw new UnsupportedOperationException("JavaScript does not permit explicit pointers");
    }

    @Override
    public boolean methodsHaveDeclaredParameterTypes() {
      return false;
    }

    @Override
    public JSInstructionFactory instructionFactory() {
      return new JSInstructionFactory() {

        @Override
        public JavaScriptCheckReference CheckReference(int iindex, int ref) {
          return new JavaScriptCheckReference(iindex, ref);
        }

        @Override
        public SSAGetInstruction GetInstruction(int iindex, int result, int ref, String field) {
          return GetInstruction(iindex, result, ref,
              FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(field), JavaScriptTypes.Root));
        }

        @Override
        public JavaScriptInstanceOf InstanceOf(int iindex, int result, int objVal, int typeVal) {
          return new JavaScriptInstanceOf(iindex, result, objVal, typeVal);
        }

        @Override
        public JavaScriptInvoke Invoke(int iindex, int function, int[] results, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, results, params, exception, site);
        }

        @Override
        public JavaScriptInvoke Invoke(int iindex, int function, int result, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, result, params, exception, site);
        }

        @Override
        public JavaScriptInvoke Invoke(int iindex, int function, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, params, exception, site);
        }

        @Override
        public JavaScriptPropertyRead PropertyRead(int iindex, int result, int objectRef, int memberRef) {
          return new JavaScriptPropertyRead(iindex, result, objectRef, memberRef);
        }

        @Override
        public JavaScriptPropertyWrite PropertyWrite(int iindex, int objectRef, int memberRef, int value) {
          return new JavaScriptPropertyWrite(iindex, objectRef, memberRef, value);
        }

        @Override
        public SSAPutInstruction PutInstruction(int iindex, int ref, int value, String field) {
          try {
            byte[] utf8 = field.getBytes("UTF-8");
            return PutInstruction(iindex, ref, value, 
                FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreate(utf8, 0, utf8.length), JavaScriptTypes.Root));
          } catch (UnsupportedEncodingException e) {
            Assertions.UNREACHABLE();
            return null;
          }
        }

        @Override
        public JavaScriptTypeOfInstruction TypeOfInstruction(int iindex, int lval, int object) {
          return new JavaScriptTypeOfInstruction(iindex, lval, object);
        }

        @Override
        public JavaScriptWithRegion WithRegion(int iindex, int expr, boolean isEnter) {
          return new JavaScriptWithRegion(iindex, expr, isEnter);
        }

        @Override
        public AstAssertInstruction AssertInstruction(int iindex, int value, boolean fromSpecification) {
          return new AstAssertInstruction(iindex, value, fromSpecification);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.AssignInstruction AssignInstruction(int iindex, int result, int val) {
          return new AssignInstruction(iindex, result, val);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.EachElementGetInstruction EachElementGetInstruction(int iindex, int value, int objectRef, int prevProp) {
          return new EachElementGetInstruction(iindex, value, objectRef, prevProp);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction EachElementHasNextInstruction(int iindex, int value, int objectRef, int prop) {
          return new EachElementHasNextInstruction(iindex, value, objectRef, prop);
        }

        @Override
        public AstEchoInstruction EchoInstruction(int iindex, int[] rvals) {
          return new AstEchoInstruction(iindex, rvals);
        }

        @Override
        public AstGlobalRead GlobalRead(int iindex, int lhs, FieldReference global) {
          return new AstGlobalRead(iindex, lhs, global);
        }

        @Override
        public AstGlobalWrite GlobalWrite(int iindex, FieldReference global, int rhs) {
          return new AstGlobalWrite(iindex, global, rhs);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, int fieldVal, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldVal, fieldRef);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldRef);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, int fieldVal) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldVal);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval) {
          return new AstIsDefinedInstruction(iindex, lval, rval);
        }

        @Override
        public AstLexicalRead LexicalRead(int iindex, Access[] accesses) {
          return new AstLexicalRead(iindex, accesses);
        }

        @Override
        public AstLexicalRead LexicalRead(int iindex, Access access) {
          return new AstLexicalRead(iindex, access);
        }

        @Override
        public AstLexicalRead LexicalRead(int iindex, int lhs, String definer, String globalName, TypeReference type) {
          return new AstLexicalRead(iindex, lhs, definer, globalName, type);
        }

        @Override
        public AstLexicalWrite LexicalWrite(int iindex, Access[] accesses) {
          return new AstLexicalWrite(iindex, accesses);
        }

        @Override
        public AstLexicalWrite LexicalWrite(int iindex, Access access) {
          return new AstLexicalWrite(iindex, access);
        }

        @Override
        public AstLexicalWrite LexicalWrite(int iindex, String definer, String globalName, TypeReference type, int rhs) {
          return new AstLexicalWrite(iindex, definer, globalName, type, rhs);
        }

        @Override
        public SSAArrayLengthInstruction ArrayLengthInstruction(int iindex, int result, int arrayref) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAArrayLoadInstruction ArrayLoadInstruction(int iindex, int result, int arrayref, int index, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAArrayStoreInstruction ArrayStoreInstruction(int iindex, int arrayref, int index, int value, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAAbstractBinaryInstruction BinaryOpInstruction(int iindex, IOperator operator, boolean overflow, boolean unsigned, int result,
            int val1, int val2, boolean mayBeInteger) {
          return new SSABinaryOpInstruction(iindex, operator, result, val1, val2, mayBeInteger) {
            @Override
            public boolean isPEI() {
              return false;
            }

            @Override
            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
              return insts.BinaryOpInstruction(iindex, getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0],
                  uses == null ? getUse(0) : uses[0], uses == null ? getUse(1) : uses[1], mayBeIntegerOp());
            }
          };
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, TypeReference[] types, boolean isPEI) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, int[] typeValues, boolean isPEI) {
          throw new UnsupportedOperationException();
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
        public SSAComparisonInstruction ComparisonInstruction(int iindex, Operator operator, int result, int val1, int val2) {
          return new SSAComparisonInstruction(iindex, operator, result, val1, val2);
        }

        @Override
        public SSAConditionalBranchInstruction ConditionalBranchInstruction(int iindex,
            com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2, int target) {
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
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAGetInstruction GetInstruction(int iindex, int result, int ref, FieldReference field) {
          return new SSAGetInstruction(iindex, result, ref, field) {
            @Override
            public boolean isPEI() {
              return false;
            }
          };
        }

        @Override
        public SSAGotoInstruction GotoInstruction(int iindex, int target) {
          return new SSAGotoInstruction(iindex, target);
        }

        @Override
        public SSAInstanceofInstruction InstanceofInstruction(int iindex, int result, int ref, TypeReference checkedType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAInvokeInstruction InvokeInstruction(int iindex, int result, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAInvokeInstruction InvokeInstruction(int iindex, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSALoadMetadataInstruction LoadMetadataInstruction(int iindex, int lval, TypeReference entityType, Object token) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAMonitorInstruction MonitorInstruction(int iindex, int ref, boolean isEnter) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site) {
          return new SSANewInstruction(iindex, result, site) {
            @Override
            public boolean isPEI() {
              return true;
            }

            @Override
            public Collection<TypeReference> getExceptionTypes() {
              return Collections.singleton(JavaScriptTypes.TypeError);
            }
          };
        }

        @Override
        public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site, int[] params) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAPhiInstruction PhiInstruction(int iindex, int result, int[] params) {
          return new SSAPhiInstruction(iindex, result, params);
        }

        @Override
        public SSAPiInstruction PiInstruction(int iindex, int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
          return new SSAPiInstruction(iindex, result, val, piBlock, successorBlock, cause);
        }

        @Override
        public SSAPutInstruction PutInstruction(int iindex, int ref, int value, FieldReference field) {
          return new SSAPutInstruction(iindex, ref, value, field) {
            @Override
            public boolean isPEI() {
              return false;
            }
          };
        }

        @Override
        public SSAPutInstruction PutInstruction(int iindex, int value, FieldReference field) {
          throw new UnsupportedOperationException();
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
            public boolean isPEI() {
              return true;
            }

            @Override
            public Collection<TypeReference> getExceptionTypes() {
              return Collections.emptySet();
            }
          };
        }

        @Override
        public SSAUnaryOpInstruction UnaryOpInstruction(int iindex, com.ibm.wala.shrikeBT.IUnaryOpInstruction.IOperator operator, int result,
            int val) {
          return new SSAUnaryOpInstruction(iindex, operator, result, val);
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
        public SSAStoreIndirectInstruction StoreIndirectInstruction(int iindex, int addressVal, int rval, TypeReference t) {
          throw new UnsupportedOperationException();
        }

        @Override
        public PrototypeLookup PrototypeLookup(int iindex, int lval, int object) {
          return new PrototypeLookup(iindex, lval, object);
        }

        @Override
        public SetPrototype SetPrototype(int iindex, int object, int prototype) {
          return new SetPrototype(iindex, object, prototype);
        }

      };
    }

    @Override
    public boolean isDoubleType(TypeReference type) {
      return type == JavaScriptTypes.Number || type == JavaScriptTypes.NumberObject;
    }

    @Override
    public boolean isFloatType(TypeReference type) {
      return false;
    }

    @Override
    public boolean isIntType(TypeReference type) {
      return false;
    }

    @Override
    public boolean isLongType(TypeReference type) {
      return false;
    }

    @Override
    public boolean isMetadataType(TypeReference type) {
      return false;
    }

    @Override
    public boolean isStringType(TypeReference type) {
      return type == JavaScriptTypes.String || type == JavaScriptTypes.StringObject;
    }

    @Override
    public boolean isVoidType(TypeReference type) {
      return false;
    }

    @Override
    public TypeReference getStringType() {
      return JavaScriptTypes.String;
    }

    @Override
    public PrimitiveType getPrimitive(TypeReference reference) {
      return PrimitiveType.getPrimitive(reference);
    }

    @Override
    public boolean isBooleanType(TypeReference type) {
      return JavaScriptTypes.Boolean.equals(type);
    }

    @Override
    public boolean isCharType(TypeReference type) {
      return false;
    }

  };

  private final JavaScriptTranslatorFactory translatorFactory;
  
  private final CAstRewriterFactory<?, ?> preprocessor;
  
  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory) {
    this(cha, translatorFactory, null);
  }

  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory, CAstRewriterFactory<?, ?> preprocessor) {
    super(cha);
    this.translatorFactory = translatorFactory;
    this.preprocessor = preprocessor;
  }

  private static final Set<CAstQualifier> functionQualifiers;

  static {
    functionQualifiers = HashSetFactory.make();
    functionQualifiers.add(CAstQualifier.PUBLIC);
    functionQualifiers.add(CAstQualifier.FINAL);
  }

  public IClass makeCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
    return new DynamicCodeBody(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName(name)), P, this,
        sourcePosition, entity, context);
  }

  public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
    return makeCodeBodyType(name, JavaScriptTypes.Function, pos, entity, context);
  }

  public IClass defineScriptType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
    return makeCodeBodyType(name, JavaScriptTypes.Script, pos, entity, context);
  }

  public IMethod defineCodeBodyCode(String clsName, AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
      Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    DynamicCodeBody C = (DynamicCodeBody) lookupClass(clsName, cha);
    assert C != null : clsName;
    return C.setCodeBody(makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, C));
  }

  public DynamicMethodObject makeCodeBodyCode(AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
      Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
      IClass C) {
    return new DynamicMethodObject(C, functionQualifiers, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo,
        debugInfo);
  }

  final CoreClass ROOT = new CoreClass(AstTypeReference.rootTypeName, null, this, null);

  final CoreClass UNDEFINED = new CoreClass(JavaScriptTypes.Undefined.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass PRIMITIVES = new CoreClass(JavaScriptTypes.Primitives.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass FAKEROOT = new CoreClass(JavaScriptTypes.FakeRoot.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass STRING = new CoreClass(JavaScriptTypes.String.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass NULL = new CoreClass(JavaScriptTypes.Null.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass ARRAY = new CoreClass(JavaScriptTypes.Array.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass OBJECT = new CoreClass(JavaScriptTypes.Object.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass TYPE_ERROR = new CoreClass(JavaScriptTypes.TypeError.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass CODE_BODY = new CoreClass(JavaScriptTypes.CodeBody.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass FUNCTION = new CoreClass(JavaScriptTypes.Function.getName(), JavaScriptTypes.CodeBody.getName(), this, null);

  final CoreClass SCRIPT = new CoreClass(JavaScriptTypes.Script.getName(), JavaScriptTypes.CodeBody.getName(), this, null);

  final CoreClass BOOLEAN = new CoreClass(JavaScriptTypes.Boolean.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass NUMBER = new CoreClass(JavaScriptTypes.Number.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass DATE = new CoreClass(JavaScriptTypes.Date.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass REGEXP = new CoreClass(JavaScriptTypes.RegExp.getName(), JavaScriptTypes.Root.getName(), this, null);

  final CoreClass BOOLEAN_OBJECT = new CoreClass(JavaScriptTypes.BooleanObject.getName(), JavaScriptTypes.Object.getName(), this, null);

  final CoreClass NUMBER_OBJECT = new CoreClass(JavaScriptTypes.NumberObject.getName(), JavaScriptTypes.Object.getName(), this, null);

  final CoreClass DATE_OBJECT = new CoreClass(JavaScriptTypes.DateObject.getName(), JavaScriptTypes.Object.getName(), this, null);

  final CoreClass REGEXP_OBJECT = new CoreClass(JavaScriptTypes.RegExpObject.getName(), JavaScriptTypes.Object.getName(), this, null);

  final CoreClass STRING_OBJECT = new CoreClass(JavaScriptTypes.StringObject.getName(), JavaScriptTypes.Object.getName(), this, null);

  @Override
  public Language getLanguage() {
    return JS;
  }

  @Override
  public ClassLoaderReference getReference() {
    return JavaScriptTypes.jsLoader;
  }

  @Override
  public SSAInstructionFactory getInstructionFactory() {
    return JS.instructionFactory();
  }

  /**
   * JavaScript files with code to model various aspects of the language
   * semantics. See com.ibm.wala.cast.js/dat/prologue.js.
   */
  public static final Set<String> bootstrapFileNames;

  private static String prologueFileName = "prologue.js";

  public static void resetPrologueFile() {
    prologueFileName = "prologue.js";
  }

  public static void setPrologueFile(String name) {
    prologueFileName = name;
  }

  public static void addBootstrapFile(String fileName) {
    bootstrapFileNames.add(fileName);
  }

  static {
    bootstrapFileNames = HashSetFactory.make();
    bootstrapFileNames.add(prologueFileName);
  }

  @Override
  protected TranslatorToCAst getTranslatorToCAst(final CAst ast, ModuleEntry module) {
    TranslatorToCAst translator = translatorFactory.make(ast, module);
    if(preprocessor != null)
      translator.addRewriter(preprocessor, true);
    return translator;
  }

  @Override
  protected TranslatorToIR initTranslator() {
    return new JSAstTranslator(this);
  }

  @Override
  protected boolean shouldTranslate(CAstEntity entity) {
    return true;
  }
}
