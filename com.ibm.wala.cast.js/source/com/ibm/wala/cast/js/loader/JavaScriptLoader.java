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
import java.util.*;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.cast.js.translator.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cast.loader.*;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;

public class JavaScriptLoader implements IClassLoader {
  private final Map types = new HashMap();
  private final JavaScriptTranslatorFactory translatorFactory;
  private final ClassHierarchy cha;

  JavaScriptLoader(ClassHierarchy cha,
		   JavaScriptTranslatorFactory translatorFactory) 
  {
    this.cha = cha;
    this.translatorFactory = translatorFactory;
  }

  class JavaScriptClass extends AstClass {
    private IClass superClass;
    
    private JavaScriptClass(IClassLoader loader,
			    TypeReference classRef, 			   
			    TypeReference superRef, 
			    CAstSourcePositionMap.Position sourcePosition) {
      super(sourcePosition,
	    classRef.getName(),
	    loader,
	    (short)0,
	    Collections.EMPTY_MAP,
	    Collections.EMPTY_MAP);
      types.put(classRef.getName(), this);
      superClass = superRef==null? null: loader.lookupClass(superRef.getName(), cha);
    }

    public ClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "JS:" + getReference().toString();
    }

    @Override
    public Collection getDirectInterfaces() throws ClassHierarchyException {
      return Collections.EMPTY_SET;
    }

    @Override
     public IClass getSuperclass() throws ClassHierarchyException {
      return superClass;
    }
  }

  class JavaScriptRootClass extends AstDynamicPropertyClass {

    private JavaScriptRootClass(IClassLoader loader, 
				CAstSourcePositionMap.Position sourcePosition) 
    {
      super(sourcePosition,
	    JavaScriptTypes.Root.getName(),
	    loader,
	    (short)0,
	    Collections.EMPTY_MAP,
	    JavaScriptTypes.Root);

      types.put(JavaScriptTypes.Root.getName(), this);
    }

    public ClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "JS Root:" + getReference().toString();
    }

    public Collection getDirectInterfaces() throws ClassHierarchyException {
      return Collections.EMPTY_SET;
    }

    public IClass getSuperclass() throws ClassHierarchyException {
      return null;
    }
  }

  class JavaScriptCodeBody extends AstFunctionClass {

    public JavaScriptCodeBody(TypeReference codeName,
			      TypeReference parent,
			      IClassLoader loader,
			      CAstSourcePositionMap.Position sourcePosition) 
    {
      super(codeName, parent, loader, sourcePosition);
      types.put(codeName.getName(), this);
    }      

    public ClassHierarchy getClassHierarchy() {
      return cha;
    }

    private IMethod setCodeBody(IMethod codeBody) {
      this.functionBody = codeBody;
      return codeBody;
    }
  }
      
  private final Set functionQualifiers;
  
  {  
    functionQualifiers = new HashSet();
    functionQualifiers.add(CAstQualifier.PUBLIC);
    functionQualifiers.add(CAstQualifier.FINAL);
  }

  public class JavaScriptMethodObject extends AstMethod {

    JavaScriptMethodObject(JavaScriptCodeBody cls,
			   AbstractCFG cfg,
			   SymbolTable symtab,
			   boolean hasCatchBlock,
			   TypeReference[][] caughtTypes,
			   LexicalInformation lexicalInfo,
			   DebuggingInformation debugInfo)
    {
      super(cls, 
	    functionQualifiers,
	    cfg, 
	    symtab, 
	    AstMethodReference.fnReference(cls.getReference()),
	    hasCatchBlock, 
	    caughtTypes,
	    lexicalInfo,
	    debugInfo);
    }

    public ClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "<Code body of " + cls + ">";
    }

    public TypeReference[] getDeclaredExceptions() {
      return null;
    }
    
    public LexicalParent[] getParents() {
      if (lexicalInfo == null) return new LexicalParent[ 0 ];
	  
      final String[] parents = lexicalInfo.getScopingParents();

      if (parents == null) return new LexicalParent[ 0 ];

      LexicalParent result[] =
	new LexicalParent[ parents.length ];
      
      for(int i = 0; i < parents.length; i++) {
	final int hack = i;
	final AstMethod method = (AstMethod) lookupClass(parents[i], cha).getMethod(AstMethodReference.fnSelector);
	result[i] = new LexicalParent() {
	  public String getName() { return parents[hack]; }
	  public AstMethod getMethod() { return method; }
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

  public IClass 
    defineCodeBodyType(String name, 
		       TypeReference P, 
		       CAstSourcePositionMap.Position sourcePosition) 
  {
    return
      new JavaScriptCodeBody(
        TypeReference.findOrCreate(
	  JavaScriptTypes.jsLoader, 
	  TypeName.string2TypeName( name )),
	P,
	this,
	sourcePosition);
  }

  public IClass 
    defineFunctionType(String name, CAstSourcePositionMap.Position pos) 
  {
    return defineCodeBodyType(name, JavaScriptTypes.Function, pos);
  }

  public IClass
    defineScriptType(String name, CAstSourcePositionMap.Position pos) 
  {
    return defineCodeBodyType(name, JavaScriptTypes.Script, pos);
  }

  public IMethod defineCodeBodyCode(String clsName,
				    AbstractCFG cfg,
				    SymbolTable symtab,
				    boolean hasCatchBlock,
				    TypeReference[][] caughtTypes,
				    LexicalInformation lexicalInfo,
				    DebuggingInformation debugInfo)
  {
    JavaScriptCodeBody C = (JavaScriptCodeBody) lookupClass(clsName, cha);
    Assertions._assert(C != null, clsName);
    return C.setCodeBody(new JavaScriptMethodObject(C, cfg, symtab, hasCatchBlock, caughtTypes, lexicalInfo, debugInfo));
  }

  final JavaScriptRootClass ROOT =
    new JavaScriptRootClass(this, null);
  final JavaScriptClass UNDEFINED =
    new JavaScriptClass(this, JavaScriptTypes.Undefined, JavaScriptTypes.Root, null);
  final JavaScriptClass PRIMITIVES =
    new JavaScriptClass(this, JavaScriptTypes.Primitives, JavaScriptTypes.Root, null);
  final JavaScriptClass STRING =
    new JavaScriptClass(this, JavaScriptTypes.String, JavaScriptTypes.Root, null);
  final JavaScriptClass NULL =
    new JavaScriptClass(this, JavaScriptTypes.Null, JavaScriptTypes.Root, null);
  final JavaScriptClass BOOLEAN =
    new JavaScriptClass(this, JavaScriptTypes.Boolean, JavaScriptTypes.Root, null);
  final JavaScriptClass NUMBER =
    new JavaScriptClass(this, JavaScriptTypes.Number, JavaScriptTypes.Root, null);
  final JavaScriptClass DATE =
    new JavaScriptClass(this, JavaScriptTypes.Date, JavaScriptTypes.Root, null);
  final JavaScriptClass REGEXP =
    new JavaScriptClass(this, JavaScriptTypes.RegExp, JavaScriptTypes.Root, null);
  final JavaScriptClass ARRAY =
    new JavaScriptClass(this, JavaScriptTypes.Array, JavaScriptTypes.Root, null);
  final JavaScriptClass OBJECT =
    new JavaScriptClass(this, JavaScriptTypes.Object, JavaScriptTypes.Root, null);
  final JavaScriptClass TYPE_ERROR =
    new JavaScriptClass(this, JavaScriptTypes.TypeError, JavaScriptTypes.Root, null);
  final JavaScriptClass CODE_BODY =
    new JavaScriptClass(this, JavaScriptTypes.CodeBody, JavaScriptTypes.Root, null);
  final JavaScriptClass FUNCTION =
    new JavaScriptClass(this, JavaScriptTypes.Function, JavaScriptTypes.CodeBody, null);
  final JavaScriptClass SCRIPT =
    new JavaScriptClass(this, JavaScriptTypes.Script, JavaScriptTypes.CodeBody, null);

  public IClass lookupClass(String className, ClassHierarchy cha) {
    Assertions._assert(this.cha == cha);
    return (IClass) types.get( TypeName.string2TypeName(className) );
  }

  public IClass lookupClass(TypeName className, ClassHierarchy cha) {
    Assertions._assert(this.cha == cha);
    return (IClass) types.get( className );
  }

  public ClassLoaderReference getReference() {
    return JavaScriptTypes.jsLoader;
  }

  public Iterator iterateAllClasses() {
    return types.values().iterator();
  }

  public int getNumberOfClasses() {
    return 0;
  }

  public Atom getName() {
    return getReference().getName();
  }

  public int getNumberOfMethods() {
      return types.size();
  }

  public String getSourceFileName(IClass klass) {
    return klass.getSourceFileName();
  }

  public IClassLoader getParent() {
    // currently, JavaScript land does not interact with any other loaders
    Assertions.UNREACHABLE("JavaScriptLoader.getParent() called?!?");
    return null;
  }

  public void init(Set modules) throws IOException {
    translatorFactory.make(this).translate(modules);
  }

  public void removeAll(Collection toRemove) {
    Set keys = new HashSet();

    for(Iterator EE = types.entrySet().iterator(); EE.hasNext(); ) {
      Map.Entry E = (Map.Entry)EE.next();
      if (toRemove.contains( E.getValue() )) {
	keys.add( E.getKey() );
      }
    }

    for(Iterator KK = keys.iterator(); KK.hasNext(); ) {
      types.remove( KK.next() );
    }
  }

}
