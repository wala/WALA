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
import java.util.Iterator;
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
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.LanguageImpl;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
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

    public Atom getName() {
      return Atom.findOrCreateUnicodeAtom("JavaScript");
    }

    public TypeReference getRootType() {
      return JavaScriptTypes.Root;
    }

    public TypeReference getThrowableType() {
      return JavaScriptTypes.Root;
    }

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

    public boolean isNullType(TypeReference type) {
      return type.equals(JavaScriptTypes.Undefined) || type.equals(JavaScriptTypes.Null);
    }

    public TypeReference[] getArrayInterfaces() {
      return new TypeReference[0];
    }

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

    public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
        throws InvalidClassFileException {
      return Collections.singleton(JavaScriptTypes.Root);
    }

    public Object getMetadataToken(Object value) {
      assert false;
      return null;
    }

    public TypeReference getPointerType(TypeReference pointee) throws UnsupportedOperationException {
      throw new UnsupportedOperationException("JavaScript does not permit explicit pointers");
    }

    public JSInstructionFactory instructionFactory() {
      return new JSInstructionFactory() {

        public JavaScriptCheckReference CheckReference(int iindex, int ref) {
          return new JavaScriptCheckReference(iindex, ref);
        }

        public SSAGetInstruction GetInstruction(int iindex, int result, int ref, String field) {
          return GetInstruction(iindex, result, ref, 
              FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(field), JavaScriptTypes.Root));
        }

        public JavaScriptInstanceOf InstanceOf(int iindex, int result, int objVal, int typeVal) {
          return new JavaScriptInstanceOf(iindex, result, objVal, typeVal);
        }

        public JavaScriptInvoke Invoke(int iindex, int function, int[] results, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, results, params, exception, site);
        }

        public JavaScriptInvoke Invoke(int iindex, int function, int[] results, int[] params, int exception, CallSiteReference site,
            Access[] lexicalReads, Access[] lexicalWrites) {
          return new JavaScriptInvoke(iindex, function, results, params, exception, site, lexicalReads, lexicalWrites);
        }

        public JavaScriptInvoke Invoke(int iindex, int function, int result, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, result, params, exception, site);
        }

        public JavaScriptInvoke Invoke(int iindex, int function, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(iindex, function, params, exception, site);
        }

        public JavaScriptPropertyRead PropertyRead(int iindex, int result, int objectRef, int memberRef) {
          return new JavaScriptPropertyRead(iindex, result, objectRef, memberRef);
        }

        public JavaScriptPropertyWrite PropertyWrite(int iindex, int objectRef, int memberRef, int value) {
          return new JavaScriptPropertyWrite(iindex, objectRef, memberRef, value);
        }

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

        public JavaScriptTypeOfInstruction TypeOfInstruction(int iindex, int lval, int object) {
          return new JavaScriptTypeOfInstruction(iindex, lval, object);
        }

        public JavaScriptWithRegion WithRegion(int iindex, int expr, boolean isEnter) {
          return new JavaScriptWithRegion(iindex, expr, isEnter);
        }

        public AstAssertInstruction AssertInstruction(int iindex, int value, boolean fromSpecification) {
          return new AstAssertInstruction(iindex, value, fromSpecification);
        }

        public com.ibm.wala.cast.ir.ssa.AssignInstruction AssignInstruction(int iindex, int result, int val) {
          return new AssignInstruction(iindex, result, val);
        }

        public com.ibm.wala.cast.ir.ssa.EachElementGetInstruction EachElementGetInstruction(int iindex, int value, int objectRef) {
          return new EachElementGetInstruction(iindex, value, objectRef);
        }

        public com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction EachElementHasNextInstruction(int iindex, int value, int objectRef) {
          return new EachElementHasNextInstruction(iindex, value, objectRef);
        }

        public AstEchoInstruction EchoInstruction(int iindex, int[] rvals) {
          return new AstEchoInstruction(iindex, rvals);
        }

        public AstGlobalRead GlobalRead(int iindex, int lhs, FieldReference global) {
          return new AstGlobalRead(iindex, lhs, global);
        }

        public AstGlobalWrite GlobalWrite(int iindex, FieldReference global, int rhs) {
          return new AstGlobalWrite(iindex, global, rhs);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, int fieldVal, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldVal, fieldRef);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldRef);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval, int fieldVal) {
          return new AstIsDefinedInstruction(iindex, lval, rval, fieldVal);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int iindex, int lval, int rval) {
          return new AstIsDefinedInstruction(iindex, lval, rval);
        }

        public AstLexicalRead LexicalRead(int iindex, Access[] accesses) {
          return new AstLexicalRead(iindex, accesses);
        }

        public AstLexicalRead LexicalRead(int iindex, Access access) {
          return new AstLexicalRead(iindex, access);
        }

        public AstLexicalRead LexicalRead(int iindex, int lhs, String definer, String globalName) {
          return new AstLexicalRead(iindex, lhs, definer, globalName);
        }

        public AstLexicalWrite LexicalWrite(int iindex, Access[] accesses) {
          return new AstLexicalWrite(iindex, accesses);
        }

        public AstLexicalWrite LexicalWrite(int iindex, Access access) {
          return new AstLexicalWrite(iindex, access);
        }

        public AstLexicalWrite LexicalWrite(int iindex, String definer, String globalName, int rhs) {
          return new AstLexicalWrite(iindex, definer, globalName, rhs);
        }

        public SSAArrayLengthInstruction ArrayLengthInstruction(int iindex, int result, int arrayref) {
          throw new UnsupportedOperationException();
        }

        public SSAArrayLoadInstruction ArrayLoadInstruction(int iindex, int result, int arrayref, int index, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        public SSAArrayStoreInstruction ArrayStoreInstruction(int iindex, int arrayref, int index, int value, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        public SSABinaryOpInstruction BinaryOpInstruction(int iindex, IOperator operator, boolean overflow, boolean unsigned, int result,
            int val1, int val2, boolean mayBeInteger) {
          return new SSABinaryOpInstruction(iindex, operator, result, val1, val2, mayBeInteger) {
            public boolean isPEI() {
              return false;
            }

            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
              return insts.BinaryOpInstruction(iindex, getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0],
                  uses == null ? getUse(0) : uses[0], uses == null ? getUse(1) : uses[1], mayBeIntegerOp());
            }
          };
        }

        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, TypeReference[] types, boolean isPEI) {
          throw new UnsupportedOperationException();
        }

        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, int[] typeValues, boolean isPEI) {
          throw new UnsupportedOperationException();
        }

        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, int typeValue, boolean isPEI) {
          assert isPEI;
          return CheckCastInstruction(iindex, result, val, new int[]{ typeValue }, true);
        }

        public SSACheckCastInstruction CheckCastInstruction(int iindex, int result, int val, TypeReference type, boolean isPEI) {
          assert isPEI;
          return CheckCastInstruction(iindex, result, val, new TypeReference[]{ type }, true);
        }

        public SSAComparisonInstruction ComparisonInstruction(int iindex, Operator operator, int result, int val1, int val2) {
          return new SSAComparisonInstruction(iindex, operator, result, val1, val2);
        }

        public SSAConditionalBranchInstruction ConditionalBranchInstruction(int iindex, 
            com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2) {
          return new SSAConditionalBranchInstruction(iindex, operator, type, val1, val2);
        }

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

        public SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int iindex, int bbNumber, int exceptionValueNumber) {
          return new SSAGetCaughtExceptionInstruction(iindex, bbNumber, exceptionValueNumber);
        }

        public SSAGetInstruction GetInstruction(int iindex, int result, FieldReference field) {
          throw new UnsupportedOperationException();
        }

        public SSAGetInstruction GetInstruction(int iindex, int result, int ref, FieldReference field) {
          return new SSAGetInstruction(iindex, result, ref, field) {
            public boolean isPEI() {
              return false;
            }
          };
        }

        public SSAGotoInstruction GotoInstruction(int iindex) {
          return new SSAGotoInstruction(iindex);
        }

        public SSAInstanceofInstruction InstanceofInstruction(int iindex, int result, int ref, TypeReference checkedType) {
          throw new UnsupportedOperationException();
        }

        public SSAInvokeInstruction InvokeInstruction(int iindex, int result, int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
        }

        public SSAInvokeInstruction InvokeInstruction(int iindex, int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
        }

        public SSALoadMetadataInstruction LoadMetadataInstruction(int iindex, int lval, TypeReference entityType, Object token) {
          throw new UnsupportedOperationException();
        }

        public SSAMonitorInstruction MonitorInstruction(int iindex, int ref, boolean isEnter) {
          throw new UnsupportedOperationException();
        }

        public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site) {
          return new SSANewInstruction(iindex, result, site) {
            public boolean isPEI() {
              return true;
            }

            public Collection<TypeReference> getExceptionTypes() {
              return Collections.singleton(JavaScriptTypes.TypeError);
            }
          };
        }

        public SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site, int[] params) {
          throw new UnsupportedOperationException();
        }

        public SSAPhiInstruction PhiInstruction(int iindex, int result, int[] params) {
          return new SSAPhiInstruction(iindex, result, params);
        }

        public SSAPiInstruction PiInstruction(int iindex, int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
          return new SSAPiInstruction(iindex, result, val, piBlock, successorBlock, cause);
        }

        public SSAPutInstruction PutInstruction(int iindex, int ref, int value, FieldReference field) {
          return new SSAPutInstruction(iindex, ref, value, field) {
            public boolean isPEI() {
              return false;
            }
          };
        }

        public SSAPutInstruction PutInstruction(int iindex, int value, FieldReference field) {
          throw new UnsupportedOperationException();
        }

        public SSAReturnInstruction ReturnInstruction(int iindex) {
          return new SSAReturnInstruction(iindex);
        }

        public SSAReturnInstruction ReturnInstruction(int iindex, int result, boolean isPrimitive) {
          return new SSAReturnInstruction(iindex, result, isPrimitive);
        }

        public SSASwitchInstruction SwitchInstruction(int iindex, int val, int defaultLabel, int[] casesAndLabels) {
          return new SSASwitchInstruction(iindex, val, defaultLabel, casesAndLabels);
        }

        public SSAThrowInstruction ThrowInstruction(int iindex, int exception) {
          return new SSAThrowInstruction(iindex, exception) {
            public boolean isPEI() {
              return true;
            }

            public Collection<TypeReference> getExceptionTypes() {
              return Collections.emptySet();
            }
          };
        }

        public SSAUnaryOpInstruction UnaryOpInstruction(int iindex, com.ibm.wala.shrikeBT.IUnaryOpInstruction.IOperator operator, int result,
            int val) {
          return new SSAUnaryOpInstruction(iindex, operator, result, val);
        }

        public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, int indexVal, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        public SSAAddressOfInstruction AddressOfInstruction(int iindex, int lval, int local, FieldReference field, TypeReference pointeeType) {
          throw new UnsupportedOperationException();
        }

        public SSALoadIndirectInstruction LoadIndirectInstruction(int iindex, int lval, TypeReference t, int addressVal) {
          throw new UnsupportedOperationException();
        }

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

    public boolean isDoubleType(TypeReference type) {
      return type == JavaScriptTypes.Number || type == JavaScriptTypes.NumberObject;
    }

    public boolean isFloatType(TypeReference type) {
      return false;
    }

    public boolean isIntType(TypeReference type) {
      return false;
    }

    public boolean isLongType(TypeReference type) {
      return false;
    }

    public boolean isMetadataType(TypeReference type) {
      return false;
    }

    public boolean isStringType(TypeReference type) {
      return type == JavaScriptTypes.String || type == JavaScriptTypes.StringObject;
    }

    public boolean isVoidType(TypeReference type) {
      return false;
    }

    public TypeReference getMetadataType() {
      return null;
    }

    public TypeReference getStringType() {
      return JavaScriptTypes.String;
    }

    public PrimitiveType getPrimitive(TypeReference reference) {
      return JSPrimitiveType.getPrimitive(reference);
    }

    public boolean isBooleanType(TypeReference type) {
      return JavaScriptTypes.Boolean.equals(type);
    }

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

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

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

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "JS Root:" + getReference().toString();
    }

    public Collection<IClass> getDirectInterfaces() {
      return Collections.emptySet();
    }

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
        TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(cls, functionQualifiers, cfg, symtab, AstMethodReference.fnReference(cls.getReference()), hasCatchBlock, caughtTypes,
          hasMonitorOp, lexicalInfo, debugInfo, null);

      // force creation of these constants by calling the getter methods
      symtab.getNullConstant();
    }

    
    public CAstEntity getEntity() {
      return entity;
    }


    public void retranslate(AstTranslator xlator) {
      xlator.translate(entity, translationContext);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "<Code body of " + cls + ">";
    }

    public TypeReference[] getDeclaredExceptions() {
      return null;
    }

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
          public String getName() {
            return parents[hack];
          }

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

    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

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
      TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    JavaScriptCodeBody C = (JavaScriptCodeBody) lookupClass(clsName, cha);
    assert C != null : clsName;
    return C.setCodeBody(makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, C));
  }

  public JavaScriptMethodObject makeCodeBodyCode(AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
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

  public Language getLanguage() {
    return JS;
  }

  public ClassLoaderReference getReference() {
    return JavaScriptTypes.jsLoader;
  }

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
  public void init(List<Module> modules) {

    List<Module> all = new LinkedList<Module>();

    for (final String fn : bootstrapFileNames) {
      all.add(new SourceURLModule(getClass().getClassLoader().getResource(fn)) {
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

  @Override
  protected void finishTranslation() {
    Iterator<ModuleEntry> ms = getModulesWithParseErrors();
    while (ms.hasNext()) {
      ModuleEntry m = ms.next();
      System.err.println(m);
      System.err.println(getMessages(m));
    }
  }
}
