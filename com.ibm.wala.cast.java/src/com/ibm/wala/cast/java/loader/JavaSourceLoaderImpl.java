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
/*
 * Created on Aug 22, 2005
 */
package com.ibm.wala.cast.java.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstField;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstType.Function;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.strings.Atom;

/**
 * A DOMO ClassLoaderImpl that processes source file entities in the
 * compile-time classpath.
 * 
 * @author rfuhrer
 */
public abstract class JavaSourceLoaderImpl extends ClassLoaderImpl {
  public Map<CAstEntity, IClass> fTypeMap = HashMapFactory.make();

  /**
   * WALA representation of a Java class residing in a source file
   * 
   * @author rfuhrer
   */
  public class JavaClass extends AstClass {
    private final IClass enclosingClass;

    private final Collection superTypeNames;

    @SuppressWarnings("unchecked")
    public JavaClass(String typeName, Collection superTypeNames, CAstSourcePositionMap.Position position, Collection qualifiers,
        JavaSourceLoaderImpl loader, IClass enclosingClass) {
      super(position, TypeName.string2TypeName(typeName), loader, (short) mapToInt(qualifiers), new HashMap(), new HashMap());
      this.superTypeNames = superTypeNames;
      this.enclosingClass = enclosingClass;
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public IClass getSuperclass() {
      for (Iterator iter = superTypeNames.iterator(); iter.hasNext();) {
        TypeName name = (TypeName) iter.next();
        IClass domoType = lookupClass(name);
        if (domoType != null && !domoType.isInterface()) {
          return domoType;
        }
      }

      Assertions.UNREACHABLE("Cannot find super class for " + this + " in " + superTypeNames);

      return null;
    }

    public Collection<IClass> getDirectInterfaces() {
      List<IClass> result = new ArrayList<IClass>();
      for (Iterator iter = superTypeNames.iterator(); iter.hasNext();) {
        TypeName name = (TypeName) iter.next();
        IClass domoType = lookupClass(name);
        if (domoType != null && domoType.isInterface()) {
          result.add(domoType);
        }
      }

      if (result.size() != (superTypeNames.size() - 1)) {
        Assertions._assert(result.size() == superTypeNames.size() - 1, "found " + result + " interfaces for " + superTypeNames
            + " for " + this);
      }

      return result;
    }

    private void addMethod(CAstEntity methodEntity, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        TypeReference[][] catchTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      declaredMethods.put(Util.methodEntityToSelector(methodEntity), new ConcreteJavaMethod(methodEntity, owner, cfg, symtab,
          hasCatchBlock, catchTypes, lexicalInfo, debugInfo));
    }

    private void addMethod(CAstEntity methodEntity, IClass owner) {
      declaredMethods.put(Util.methodEntityToSelector(methodEntity), new AbstractJavaMethod(methodEntity, owner));
    }

    private void addField(CAstEntity fieldEntity) {
      declaredFields.put(Util.fieldEntityToAtom(fieldEntity), new JavaField(fieldEntity, JavaSourceLoaderImpl.this, this));
    }

    public IClass getEnclosingClass() {
      return enclosingClass;
    }

    public String toString() {
      if (enclosingClass == null) {
        return "<src-class: " + getName().toString() + ">";
      } else {
        return "<src-class: " + getName().toString() + "(within " + enclosingClass.getName() + ")>";
      }
    }
  }

  /**
   * DOMO representation of a field on a Java type that resides in a source file
   * 
   * @author rfuhrer
   */
  private class JavaField extends AstField {
    private JavaField(CAstEntity fieldEntity, IClassLoader loader, IClass declaringClass) {
      super(FieldReference.findOrCreate(declaringClass.getReference(), Atom.findOrCreateUnicodeAtom(fieldEntity.getName()),
          TypeReference.findOrCreate(loader.getReference(), TypeName.string2TypeName(fieldEntity.getType().getName()))),
          fieldEntity.getQualifiers(), declaringClass, declaringClass.getClassHierarchy());
    }
  }

  /**
   * Generic DOMO representation of a method on a Java type that resides in a
   * source file
   * 
   * @author rfuhrer
   */
  private abstract class JavaEntityMethod extends AstMethod {
    private final TypeReference[] parameterTypes;

    private final TypeReference[] exceptionTypes;

    public JavaEntityMethod(CAstEntity methodEntity, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        TypeReference[][] catchTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(owner, methodEntity.getQualifiers(), cfg, symtab, MethodReference.findOrCreate(owner.getReference(), Util
          .methodEntityToSelector(methodEntity)), hasCatchBlock, catchTypes, lexicalInfo, debugInfo);
      this.parameterTypes = computeParameterTypes(methodEntity);
      this.exceptionTypes = computeExceptionTypes(methodEntity);
    }

    public JavaEntityMethod(CAstEntity methodEntity, IClass owner) {
      super(owner, methodEntity.getQualifiers(), MethodReference.findOrCreate(owner.getReference(), Util
          .methodEntityToSelector(methodEntity)));
      this.parameterTypes = computeParameterTypes(methodEntity);
      this.exceptionTypes = computeExceptionTypes(methodEntity);
    }

    public int getMaxLocals() {
      Assertions.UNREACHABLE("AbstractJavaMethod.getMaxLocals() called");
      return 0;
    }

    public int getMaxStackHeight() {
      Assertions.UNREACHABLE("AbstractJavaMethod.getMaxStackHeight() called");
      return 0;
    }

    public TypeReference getParameterType(int i) {
      return parameterTypes[i];
    }

    private TypeReference[] computeParameterTypes(CAstEntity methodEntity) {
      TypeReference[] types;
      CAstType.Function type = (Function) methodEntity.getType();
      int argCount = type.getArgumentTypes().size();
      if (isStatic()) {
        types = new TypeReference[argCount];
        for (int i = 0; i < argCount; i++) {
          types[i] = TypeReference.findOrCreate(JavaSourceLoaderImpl.this.getReference(), ((CAstType) type.getArgumentTypes()
              .get(i)).getName());
        }
      } else {
        types = new TypeReference[argCount + 1];
        types[0] = cls.getReference();
        for (int i = 0; i < argCount; i++) {
          types[i + 1] = TypeReference.findOrCreate(JavaSourceLoaderImpl.this.getReference(), ((CAstType) type.getArgumentTypes()
              .get(i)).getName());
        }
      }

      return types;
    }

    public TypeReference[] getDeclaredExceptions() {
      return exceptionTypes;
    }

    private TypeReference[] computeExceptionTypes(CAstEntity methodEntity) {
      CAstType.Function fType = (Function) methodEntity.getType();
      Collection exceptionTypes = fType.getExceptionTypes();

      TypeReference[] result = new TypeReference[exceptionTypes.size()];
      int i = 0;
      for (Iterator iter = exceptionTypes.iterator(); iter.hasNext(); i++) {
        CAstType type = (CAstType) iter.next();
        result[i] = TypeReference.findOrCreate(JavaSourceLoaderImpl.this.getReference(), type.getName());
      }

      return result;
    }

    public String toString() {
      return "<src-method: " + this.getReference() + ">";
    }
  }

  /**
   * DOMO representation of an abstract (body-less) method on a Java type that
   * resides in a source file
   * 
   * @author rfuhrer
   */
  private class AbstractJavaMethod extends JavaEntityMethod {
    public AbstractJavaMethod(CAstEntity methodEntity, IClass owner) {
      super(methodEntity, owner);
    }

    public String getLocalVariableName(int bcIndex, int localNumber) {
      Assertions.UNREACHABLE("AbstractJavaMethod.getLocalVariableName() called");
      return null;
    }

    public boolean hasLocalVariableTable() {
      Assertions.UNREACHABLE("AbstractJavaMethod.hasLocalVariableTable() called");
      return false;
    }

    public LexicalParent[] getParents() {
      return new LexicalParent[0];
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }
  }

  /**
   * DOMO representation of a concrete method (which has a body) on a Java type
   * that resides in a source file
   * 
   * @author rfuhrer
   */
  public class ConcreteJavaMethod extends JavaEntityMethod {
    public ConcreteJavaMethod(CAstEntity methodEntity, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        TypeReference[][] catchTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(methodEntity, owner, cfg, symtab, hasCatchBlock, catchTypes, lexicalInfo, debugInfo);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

    public boolean hasLocalVariableTable() {
      return false;
    }

    public LexicalParent[] getParents() {
      if (AstTranslator.DEBUG_LEXICAL) {
        Trace.println("resolving parents of " + this);
      }

      if (lexicalInfo == null) {
        if (AstTranslator.DEBUG_LEXICAL)
          Trace.println("no info");
        return new LexicalParent[0];
      }

      final String[] parents = lexicalInfo.getScopingParents();

      if (parents == null) {
        if (AstTranslator.DEBUG_LEXICAL)
          Trace.println("no parents");
        return new LexicalParent[0];
      }

      LexicalParent result[] = new LexicalParent[parents.length];

      for (int i = 0; i < parents.length; i++) {
        int lastLeftParen = parents[i].lastIndexOf('(');
        int lastQ = parents[i].lastIndexOf('/', lastLeftParen);
        String typeName = parents[i].substring(0, lastQ);
        final IClass cls = lookupClass(TypeName.string2TypeName(typeName));

        String sig = parents[i].substring(lastQ);
        int nameEnd = sig.indexOf('(');
        String nameStr = sig.substring(1, nameEnd);
        Atom name = Atom.findOrCreateUnicodeAtom(nameStr);

        String descStr = sig.substring(nameEnd);
        Descriptor desc = Descriptor.findOrCreateUTF8(descStr);

        final Selector sel = new Selector(name, desc);

        if (AstTranslator.DEBUG_LEXICAL)
          Trace.println("get " + typeName + ", " + nameStr + ", " + descStr);

        final int hack = i;
        result[i] = new LexicalParent() {
          public String getName() {
            return parents[hack];
          }

          public AstMethod getMethod() {
            return (AstMethod) cls.getMethod(sel);
          }
        };

        if (AstTranslator.DEBUG_LEXICAL)
          Trace.println("parent " + result[i].getName() + " is " + result[i].getMethod());
      }

      return result;
    }
  }

  public static int mapToInt(Collection/* <CAstQualifier> */qualifiers) {
    int result = 0;
    for (Iterator iter = qualifiers.iterator(); iter.hasNext();) {
      CAstQualifier q = (CAstQualifier) iter.next();

      if (q == CAstQualifier.PUBLIC)
        result |= ClassConstants.ACC_PUBLIC;
      if (q == CAstQualifier.PROTECTED)
        result |= ClassConstants.ACC_PROTECTED;
      if (q == CAstQualifier.PRIVATE)
        result |= ClassConstants.ACC_PRIVATE;
      if (q == CAstQualifier.STATIC)
        result |= ClassConstants.ACC_STATIC;
      if (q == CAstQualifier.FINAL)
        result |= ClassConstants.ACC_FINAL;
      if (q == CAstQualifier.SYNCHRONIZED)
        result |= ClassConstants.ACC_SYNCHRONIZED;
      if (q == CAstQualifier.TRANSIENT)
        result |= ClassConstants.ACC_TRANSIENT;
      if (q == CAstQualifier.NATIVE)
        result |= ClassConstants.ACC_NATIVE;
      if (q == CAstQualifier.INTERFACE)
        result |= ClassConstants.ACC_INTERFACE;
      if (q == CAstQualifier.ABSTRACT)
        result |= ClassConstants.ACC_ABSTRACT;
      if (q == CAstQualifier.VOLATILE)
        result |= ClassConstants.ACC_VOLATILE;
      if (q == CAstQualifier.STRICTFP)
        result |= ClassConstants.ACC_STRICT;
    }
    return result;
  }

  public JavaSourceLoaderImpl(ClassLoaderReference loaderRef, IClassLoader parent, SetOfClasses exclusions, IClassHierarchy cha) throws IOException {
    super(loaderRef, cha.getScope().getArrayClassLoader(), parent, cha.getScope().getExclusions(), cha);
  }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  protected void loadAllSources(Set/* <ModuleEntry> */modules) {
    getTranslator().loadAllSources(modules);
    fTypeMap = null;
  }

  protected abstract SourceModuleTranslator getTranslator();
  
  public void defineFunction(CAstEntity n, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      TypeReference[][] catchTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    ((JavaClass) owner).addMethod(n, owner, cfg, symtab, hasCatchBlock, catchTypes, lexicalInfo, debugInfo);
  }

  public void defineAbstractFunction(CAstEntity n, IClass owner) {
    ((JavaClass) owner).addMethod(n, owner);
  }

  public void defineField(CAstEntity n, IClass owner) {
    ((JavaClass) owner).addField(n);
  }

  public IClass defineType(CAstEntity type, String typeName, CAstEntity owner) {
    Collection<TypeName> superTypeNames = new ArrayList<TypeName>();
    for (Iterator superTypes = type.getType().getSupertypes().iterator(); superTypes.hasNext();) {
      superTypeNames.add(TypeName.string2TypeName(((CAstType) superTypes.next()).getName()));
    }

    JavaClass javaClass = new JavaClass(typeName, superTypeNames, type.getPosition(), type.getQualifiers(), this,
        (owner != null) ? (JavaClass) fTypeMap.get(owner) : (JavaClass) null);

    fTypeMap.put(type, javaClass);
    loadedClasses.put(javaClass.getName(), javaClass);
    return javaClass;
  }

  public String toString() {
    return "Java Source Loader (classes " + loadedClasses.values() + ")";
  }
}
