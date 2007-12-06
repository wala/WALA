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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstDynamicPropertyClass;
import com.ibm.wala.cast.loader.AstFunctionClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.LanguageImpl;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

public class JavaScriptLoader extends CAstAbstractLoader {

  public final static Language JS = new LanguageImpl() {

    public Atom getName() {
      return Atom.findOrCreateUnicodeAtom("JavaScript");
    }

    public TypeReference getRootType() {
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
      return 
	type.equals(JavaScriptTypes.Undefined)
	                 || 
	type.equals(JavaScriptTypes.Null);
    }
  };

  private static final Map<Selector,IMethod> emptyMap1 = Collections.emptyMap();
  private static final Map<Atom,IField> emptyMap2 = Collections.emptyMap();

  private final JavaScriptTranslatorFactory translatorFactory;

  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory) {
    super(cha);
    this.translatorFactory = translatorFactory;
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
    public Collection<IClass> getDirectInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    @Override
    public IClass getSuperclass() throws ClassHierarchyException {
      return superClass;
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

    public Collection<IClass> getDirectInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public IClass getSuperclass() throws ClassHierarchyException {
      return null;
    }
  }

  class JavaScriptCodeBody extends AstFunctionClass {

    public JavaScriptCodeBody(TypeReference codeName, TypeReference parent, IClassLoader loader,
        CAstSourcePositionMap.Position sourcePosition) {
      super(codeName, parent, loader, sourcePosition);
      types.put(codeName.getName(), this);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    private IMethod setCodeBody(IMethod codeBody) {
      this.functionBody = codeBody;
      return codeBody;
    }
  }

  private final Set<CAstQualifier> functionQualifiers;

  {
    functionQualifiers = HashSetFactory.make();
    functionQualifiers.add(CAstQualifier.PUBLIC);
    functionQualifiers.add(CAstQualifier.FINAL);
  }

  public class JavaScriptMethodObject extends AstMethod {

    JavaScriptMethodObject(JavaScriptCodeBody cls, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        TypeReference[][] caughtTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(cls, functionQualifiers, cfg, symtab, AstMethodReference.fnReference(cls.getReference()), hasCatchBlock, caughtTypes,
          lexicalInfo, debugInfo);
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
      if (lexicalInfo == null)
        return new LexicalParent[0];

      final String[] parents = lexicalInfo.getScopingParents();

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

        Trace.println("parent " + result[i].getName() + " is " + result[i].getMethod());
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
      return JavaScriptTypes.Root;
    }
  }

  public IClass defineCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition) {
    return new JavaScriptCodeBody(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName(name)), P, this,
        sourcePosition);
  }

  public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos) {
    return defineCodeBodyType(name, JavaScriptTypes.Function, pos);
  }

  public IClass defineScriptType(String name, CAstSourcePositionMap.Position pos) {
    return defineCodeBodyType(name, JavaScriptTypes.Script, pos);
  }

  public IMethod defineCodeBodyCode(String clsName, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      TypeReference[][] caughtTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    JavaScriptCodeBody C = (JavaScriptCodeBody) lookupClass(clsName, cha);
    Assertions._assert(C != null, clsName);
    return C.setCodeBody(new JavaScriptMethodObject(C, cfg, symtab, hasCatchBlock, caughtTypes, lexicalInfo, debugInfo));
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

  public void init(Set modules) throws IOException {
    translatorFactory.make(this).translate(modules);
  }

}
