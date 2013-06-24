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
import java.util.LinkedList;
import java.util.List;
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
import com.ibm.wala.cast.ir.translator.AstTranslator;
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
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstDynamicPropertyClass;
import com.ibm.wala.cast.loader.AstFunctionClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.Retranslatable;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.LanguageImpl;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IComparisonInstruction.Operator;
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
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
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
        Class c = o.getClass();
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
    public JSInstructionFactory instructionFactory() {
      return new JSInstructionFactory() {

        @Override
        public JavaScriptCheckReference CheckReference(int ref) {
          return new JavaScriptCheckReference(ref);
        }

        @Override
        public SSAGetInstruction GetInstruction(int result, int ref, String field) {
          return GetInstruction(result, ref,
              FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(field), JavaScriptTypes.Root));
        }

        @Override
        public JavaScriptInstanceOf InstanceOf(int result, int objVal, int typeVal) {
          return new JavaScriptInstanceOf(result, objVal, typeVal);
        }

        @Override
        public JavaScriptInvoke Invoke(int function, int[] results, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(function, results, params, exception, site);
        }

        @Override
        public JavaScriptInvoke Invoke(int function, int result, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(function, result, params, exception, site);
        }

        @Override
        public JavaScriptInvoke Invoke(int function, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(function, params, exception, site);
        }

        @Override
        public JavaScriptPropertyRead PropertyRead(int result, int objectRef, int memberRef) {
          return new JavaScriptPropertyRead(result, objectRef, memberRef);
        }

        @Override
        public JavaScriptPropertyWrite PropertyWrite(int objectRef, int memberRef, int value) {
          return new JavaScriptPropertyWrite(objectRef, memberRef, value);
        }

        @Override
        public SSAPutInstruction PutInstruction(int ref, int value, String field) {
          try {
            byte[] utf8 = field.getBytes("UTF-8");
            return PutInstruction(ref, value,
                FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreate(utf8, 0, utf8.length), JavaScriptTypes.Root));
          } catch (UnsupportedEncodingException e) {
            Assertions.UNREACHABLE();
            return null;
          }
        }

        @Override
        public JavaScriptTypeOfInstruction TypeOfInstruction(int lval, int object) {
          return new JavaScriptTypeOfInstruction(lval, object);
        }

        @Override
        public JavaScriptWithRegion WithRegion(int expr, boolean isEnter) {
          return new JavaScriptWithRegion(expr, isEnter);
        }

        @Override
        public AstAssertInstruction AssertInstruction(int value, boolean fromSpecification) {
          return new AstAssertInstruction(value, fromSpecification);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.AssignInstruction AssignInstruction(int result, int val) {
          return new AssignInstruction(result, val);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.EachElementGetInstruction EachElementGetInstruction(int value, int objectRef) {
          return new EachElementGetInstruction(value, objectRef);
        }

        @Override
        public com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction EachElementHasNextInstruction(int value, int objectRef) {
          return new EachElementHasNextInstruction(value, objectRef);
        }

        @Override
        public AstEchoInstruction EchoInstruction(int[] rvals) {
          return new AstEchoInstruction(rvals);
        }

        @Override
        public AstGlobalRead GlobalRead(int lhs, FieldReference global) {
          return new AstGlobalRead(lhs, global);
        }

        @Override
        public AstGlobalWrite GlobalWrite(FieldReference global, int rhs) {
          return new AstGlobalWrite(global, rhs);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(lval, rval, fieldVal, fieldRef);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(lval, rval, fieldRef);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal) {
          return new AstIsDefinedInstruction(lval, rval, fieldVal);
        }

        @Override
        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval) {
          return new AstIsDefinedInstruction(lval, rval);
        }

        @Override
        public AstLexicalRead LexicalRead(Access[] accesses) {
          return new AstLexicalRead(accesses);
        }

        @Override
        public AstLexicalRead LexicalRead(Access access) {
          return new AstLexicalRead(access);
        }

        @Override
        public AstLexicalRead LexicalRead(int lhs, String definer, String globalName) {
          return new AstLexicalRead(lhs, definer, globalName);
        }

        @Override
        public AstLexicalWrite LexicalWrite(Access[] accesses) {
          return new AstLexicalWrite(accesses);
        }

        @Override
        public AstLexicalWrite LexicalWrite(Access access) {
          return new AstLexicalWrite(access);
        }

        @Override
        public AstLexicalWrite LexicalWrite(String definer, String globalName, int rhs) {
          return new AstLexicalWrite(definer, globalName, rhs);
        }

        @Override
        public SSAArrayLengthInstruction ArrayLengthInstruction(int result, int arrayref) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAArrayLoadInstruction ArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAArrayStoreInstruction ArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSABinaryOpInstruction BinaryOpInstruction(IOperator operator, boolean overflow, boolean unsigned, int result,
            int val1, int val2, boolean mayBeInteger) {
          return new SSABinaryOpInstruction(operator, result, val1, val2, mayBeInteger) {
            @Override
            public boolean isPEI() {
              return false;
            }

            @Override
            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
              return insts.BinaryOpInstruction(getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0],
                  uses == null ? getUse(0) : uses[0], uses == null ? getUse(1) : uses[1], mayBeIntegerOp());
            }
          };
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference[] types, boolean isPEI) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int result, int val, int[] typeValues, boolean isPEI) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int result, int val, int typeValue, boolean isPEI) {
          assert isPEI;
          return CheckCastInstruction(result, val, new int[]{ typeValue }, true);
        }

        @Override
        public SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference type, boolean isPEI) {
          assert isPEI;
          return CheckCastInstruction(result, val, new TypeReference[]{ type }, true);
        }

        @Override
        public SSAComparisonInstruction ComparisonInstruction(Operator operator, int result, int val1, int val2) {
          return new SSAComparisonInstruction(operator, result, val1, val2);
        }

        @Override
        public SSAConditionalBranchInstruction ConditionalBranchInstruction(
            com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2) {
          return new SSAConditionalBranchInstruction(operator, type, val1, val2);
        }

        @Override
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

        @Override
        public SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
          return new SSAGetCaughtExceptionInstruction(bbNumber, exceptionValueNumber);
        }

        @Override
        public SSAGetInstruction GetInstruction(int result, FieldReference field) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAGetInstruction GetInstruction(int result, int ref, FieldReference field) {
          return new SSAGetInstruction(result, ref, field) {
            @Override
            public boolean isPEI() {
              return false;
            }
          };
        }

        @Override
        public SSAGotoInstruction GotoInstruction() {
          return new SSAGotoInstruction();
        }

        @Override
        public SSAInstanceofInstruction InstanceofInstruction(int result, int ref, TypeReference checkedType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAInvokeInstruction InvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAInvokeInstruction InvokeInstruction(int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSALoadMetadataInstruction LoadMetadataInstruction(int lval, TypeReference entityType, Object token) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAMonitorInstruction MonitorInstruction(int ref, boolean isEnter) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSANewInstruction NewInstruction(int result, NewSiteReference site) {
          return new SSANewInstruction(result, site) {
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
        public SSANewInstruction NewInstruction(int result, NewSiteReference site, int[] params) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAPhiInstruction PhiInstruction(int result, int[] params) {
          return new SSAPhiInstruction(result, params);
        }

        @Override
        public SSAPiInstruction PiInstruction(int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
          return new SSAPiInstruction(result, val, piBlock, successorBlock, cause);
        }

        @Override
        public SSAPutInstruction PutInstruction(int ref, int value, FieldReference field) {
          return new SSAPutInstruction(ref, value, field) {
            @Override
            public boolean isPEI() {
              return false;
            }
          };
        }

        @Override
        public SSAPutInstruction PutInstruction(int value, FieldReference field) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAReturnInstruction ReturnInstruction() {
          return new SSAReturnInstruction();
        }

        @Override
        public SSAReturnInstruction ReturnInstruction(int result, boolean isPrimitive) {
          return new SSAReturnInstruction(result, isPrimitive);
        }

        @Override
        public SSASwitchInstruction SwitchInstruction(int val, int defaultLabel, int[] casesAndLabels) {
          return new SSASwitchInstruction(val, defaultLabel, casesAndLabels);
        }

        @Override
        public SSAThrowInstruction ThrowInstruction(int exception) {
          return new SSAThrowInstruction(exception) {
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
        public SSAUnaryOpInstruction UnaryOpInstruction(com.ibm.wala.shrikeBT.IUnaryOpInstruction.IOperator operator, int result,
            int val) {
          return new SSAUnaryOpInstruction(operator, result, val);
        }

        @Override
        public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, int indexVal, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAAddressOfInstruction AddressOfInstruction(int lval, int local, FieldReference field, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSALoadIndirectInstruction LoadIndirectInstruction(int lval, TypeReference t, int addressVal) {
          throw new UnsupportedOperationException();
        }

        @Override
        public SSAStoreIndirectInstruction StoreIndirectInstruction(int addressVal, int rval, TypeReference t) {
          throw new UnsupportedOperationException();
        }

        @Override
        public PrototypeLookup PrototypeLookup(int lval, int object) {
          return new PrototypeLookup(lval, object);
        }

        @Override
        public SetPrototype SetPrototype(int object, int prototype) {
          return new SetPrototype(object, prototype);
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
    public TypeReference getMetadataType() {
      return null;
    }

    @Override
    public TypeReference getStringType() {
      return JavaScriptTypes.String;
    }

    @Override
    public PrimitiveType getPrimitive(TypeReference reference) {
      return JSPrimitiveType.getPrimitive(reference);
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

  private static final Map<Selector, IMethod> emptyMap1 = Collections.emptyMap();

  private static final Map<Atom, IField> emptyMap2 = Collections.emptyMap();

  private final JavaScriptTranslatorFactory translatorFactory;
  
  private final CAstRewriterFactory preprocessor;
  
  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory) {
    this(cha, translatorFactory, null);
  }

  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory, CAstRewriterFactory preprocessor) {
    super(cha);
    this.translatorFactory = translatorFactory;
    this.preprocessor = preprocessor;
  }

  class JavaScriptClass extends AstClass {
    private IClass superClass;

    private JavaScriptClass(IClassLoader loader, TypeReference classRef, TypeReference superRef,
        CAstSourcePositionMap.Position sourcePosition) {
      super(sourcePosition, classRef.getName(), loader, (short) 0, emptyMap2, emptyMap1);
      types.put(classRef.getName(), this);
      superClass = superRef == null ? null : loader.lookupClass(superRef.getName());
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public String toString() {
      return "JS:" + getReference().toString();
    }

    @Override
    public Collection<IClass> getDirectInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public IClass getSuperclass() {
      return superClass;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
      return Collections.emptySet();
    }
  }

  class JavaScriptRootClass extends AstDynamicPropertyClass {

    private JavaScriptRootClass(IClassLoader loader, CAstSourcePositionMap.Position sourcePosition) {
      super(sourcePosition, JavaScriptTypes.Root.getName(), loader, (short) 0, emptyMap1, JavaScriptTypes.Root);

      types.put(JavaScriptTypes.Root.getName(), this);
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public String toString() {
      return "JS Root:" + getReference().toString();
    }

    @Override
    public Collection<IClass> getDirectInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public IClass getSuperclass() {
      return null;
    }
    
    @Override
    public Collection<Annotation> getAnnotations() {
      return Collections.emptySet();
    }
  }

  class JavaScriptCodeBody extends AstFunctionClass {
    private final WalkContext translationContext;
    private final CAstEntity entity;
    
    public JavaScriptCodeBody(TypeReference codeName, TypeReference parent, IClassLoader loader,
        CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
      super(codeName, parent, loader, sourcePosition);
      types.put(codeName.getName(), this);
      this.translationContext = context;
      this.entity = entity;
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }
    
    private IMethod setCodeBody(JavaScriptMethodObject codeBody) {
      this.functionBody = codeBody;
      codeBody.entity = entity;
      codeBody.translationContext = translationContext;
      return codeBody;
    }
    
    @Override
    public Collection<Annotation> getAnnotations() {
      return Collections.emptySet();
    }
  }

  private static final Set<CAstQualifier> functionQualifiers;

  static {
    functionQualifiers = HashSetFactory.make();
    functionQualifiers.add(CAstQualifier.PUBLIC);
    functionQualifiers.add(CAstQualifier.FINAL);
  }

  public class JavaScriptMethodObject extends AstMethod implements Retranslatable {
    private WalkContext translationContext;
    private CAstEntity entity;

    JavaScriptMethodObject(IClass cls, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(cls, functionQualifiers, cfg, symtab, AstMethodReference.fnReference(cls.getReference()), hasCatchBlock, caughtTypes,
          hasMonitorOp, lexicalInfo, debugInfo, null);

      // force creation of these constants by calling the getter methods
      symtab.getNullConstant();
    }

    
    @Override
    public CAstEntity getEntity() {
      return entity;
    }


    @Override
    public void retranslate(AstTranslator xlator) {
      xlator.translate(entity, translationContext);
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public String toString() {
      return "<Code body of " + cls + ">";
    }

    @Override
    public TypeReference[] getDeclaredExceptions() {
      return null;
    }

    @Override
    public LexicalParent[] getParents() {
      if (lexicalInfo() == null)
        return new LexicalParent[0];

      final String[] parents = lexicalInfo().getScopingParents();

      if (parents == null)
        return new LexicalParent[0];

      LexicalParent result[] = new LexicalParent[parents.length];

      for (int i = 0; i < parents.length; i++) {
        final int hack = i;
        final AstMethod method = (AstMethod) lookupClass(parents[i], cha).getMethod(AstMethodReference.fnSelector);
        result[i] = new LexicalParent() {
          @Override
          public String getName() {
            return parents[hack];
          }

          @Override
          public AstMethod getMethod() {
            return method;
          }
        };

        if (AstTranslator.DEBUG_LEXICAL) {
          System.err.println(("parent " + result[i].getName() + " is " + result[i].getMethod()));
        }
      }

      return result;
    }

    @Override
    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

    @Override
    public boolean hasLocalVariableTable() {
      return false;
    }

    public int getMaxLocals() {
      Assertions.UNREACHABLE();
      return -1;
    }

    public int getMaxStackHeight() {
      Assertions.UNREACHABLE();
      return -1;
    }

    @Override
    public TypeReference getParameterType(int i) {
      if (i == 0) {
        return getDeclaringClass().getReference();
      } else {
        return JavaScriptTypes.Root;
      }
    }
  }

  public IClass makeCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
    return new JavaScriptCodeBody(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName(name)), P, this,
        sourcePosition, entity, context);
  }

  public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
    return makeCodeBodyType(name, JavaScriptTypes.Function, pos, entity, context);
  }

  public IClass defineScriptType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
    return makeCodeBodyType(name, JavaScriptTypes.Script, pos, entity, context);
  }

  public IMethod defineCodeBodyCode(String clsName, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    JavaScriptCodeBody C = (JavaScriptCodeBody) lookupClass(clsName, cha);
    assert C != null : clsName;
    return C.setCodeBody(makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, C));
  }

  public JavaScriptMethodObject makeCodeBodyCode(AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
      IClass C) {
    return new JavaScriptMethodObject(C, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo,
        debugInfo);
  }

  final JavaScriptRootClass ROOT = new JavaScriptRootClass(this, null);

  final JavaScriptClass UNDEFINED = new JavaScriptClass(this, JavaScriptTypes.Undefined, JavaScriptTypes.Root, null);

  final JavaScriptClass PRIMITIVES = new JavaScriptClass(this, JavaScriptTypes.Primitives, JavaScriptTypes.Root, null);

  final JavaScriptClass FAKEROOT = new JavaScriptClass(this, JavaScriptTypes.FakeRoot, JavaScriptTypes.Root, null);

  final JavaScriptClass STRING = new JavaScriptClass(this, JavaScriptTypes.String, JavaScriptTypes.Root, null);

  final JavaScriptClass NULL = new JavaScriptClass(this, JavaScriptTypes.Null, JavaScriptTypes.Root, null);

  final JavaScriptClass ARRAY = new JavaScriptClass(this, JavaScriptTypes.Array, JavaScriptTypes.Root, null);

  final JavaScriptClass OBJECT = new JavaScriptClass(this, JavaScriptTypes.Object, JavaScriptTypes.Root, null);

  final JavaScriptClass TYPE_ERROR = new JavaScriptClass(this, JavaScriptTypes.TypeError, JavaScriptTypes.Root, null);

  final JavaScriptClass CODE_BODY = new JavaScriptClass(this, JavaScriptTypes.CodeBody, JavaScriptTypes.Root, null);

  final JavaScriptClass FUNCTION = new JavaScriptClass(this, JavaScriptTypes.Function, JavaScriptTypes.CodeBody, null);

  final JavaScriptClass SCRIPT = new JavaScriptClass(this, JavaScriptTypes.Script, JavaScriptTypes.CodeBody, null);

  final JavaScriptClass BOOLEAN = new JavaScriptClass(this, JavaScriptTypes.Boolean, JavaScriptTypes.Root, null);

  final JavaScriptClass NUMBER = new JavaScriptClass(this, JavaScriptTypes.Number, JavaScriptTypes.Root, null);

  final JavaScriptClass DATE = new JavaScriptClass(this, JavaScriptTypes.Date, JavaScriptTypes.Root, null);

  final JavaScriptClass REGEXP = new JavaScriptClass(this, JavaScriptTypes.RegExp, JavaScriptTypes.Root, null);

  final JavaScriptClass BOOLEAN_OBJECT = new JavaScriptClass(this, JavaScriptTypes.BooleanObject, JavaScriptTypes.Object, null);

  final JavaScriptClass NUMBER_OBJECT = new JavaScriptClass(this, JavaScriptTypes.NumberObject, JavaScriptTypes.Object, null);

  final JavaScriptClass DATE_OBJECT = new JavaScriptClass(this, JavaScriptTypes.DateObject, JavaScriptTypes.Object, null);

  final JavaScriptClass REGEXP_OBJECT = new JavaScriptClass(this, JavaScriptTypes.RegExpObject, JavaScriptTypes.Object, null);

  final JavaScriptClass STRING_OBJECT = new JavaScriptClass(this, JavaScriptTypes.StringObject, JavaScriptTypes.Object, null);

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

  /**
   * adds the {@link #bootstrapFileNames bootstrap files} to the list of modules
   * and then invokes the superclass method
   */
  @Override
  public void init(List<Module> modules) {

    List<Module> all = new LinkedList<Module>();

    for (final String fn : bootstrapFileNames) {
      all.add(new SourceURLModule(getClass().getClassLoader().getResource(fn)) {
        @Override
        public String getName() {
          return fn;
        }
      });
    }

    all.addAll(modules);

    super.init(all);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected TranslatorToCAst getTranslatorToCAst(final CAst ast, SourceModule module) {
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
