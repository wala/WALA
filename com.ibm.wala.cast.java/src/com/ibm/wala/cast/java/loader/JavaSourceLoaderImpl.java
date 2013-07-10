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

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionFactory;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.AstJavaNewEnclosingInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstField;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstType.Function;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * A {@link ClassLoaderImpl} that processes source file entities in the
 * compile-time classpath.
 */
public abstract class JavaSourceLoaderImpl extends ClassLoaderImpl {
  public Map<CAstEntity, IClass> fTypeMap = HashMapFactory.make();

  /**
   * WALA representation of a Java class residing in a source file
   * 
   * @author rfuhrer
   */
  public class JavaClass extends AstClass {
    protected final IClass enclosingClass;

    protected final Collection superTypeNames;

    private final Collection<Annotation> annotations;
    
    public JavaClass(String typeName, Collection superTypeNames, CAstSourcePositionMap.Position position, Collection qualifiers,
        JavaSourceLoaderImpl loader, IClass enclosingClass, Collection<Annotation> annotations) {
      super(position, TypeName.string2TypeName(typeName), loader, (short) mapToInt(qualifiers), new HashMap<Atom, IField>(), new HashMap<Selector, IMethod>());
      this.superTypeNames = superTypeNames;
      this.enclosingClass = enclosingClass;
      this.annotations = annotations;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
      return annotations;
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public IClass getSuperclass() {
      boolean excludedSupertype=false;
      for (Iterator iter = superTypeNames.iterator(); iter.hasNext();) {
        TypeName name = (TypeName) iter.next();
        IClass domoType = lookupClass(name);
        if (domoType != null && !domoType.isInterface()) {
          return domoType;
        }
        if (domoType == null && getClassHierarchy().getScope().getExclusions().contains(name.toString().substring(1))){
          excludedSupertype = true;
        }
      }

      // The following test allows the root class to reside in source; without
      // it, the assertion requires all classes represented by a JavaClass to
      // have a superclass.
      if (!getName().equals(JavaSourceLoaderImpl.this.getLanguage().getRootType().getName()) && !excludedSupertype) {
        Assertions.UNREACHABLE("Cannot find super class for " + this + " in " + superTypeNames);
      }
      
      if (excludedSupertype){
        System.err.println("Not tracking calls through excluded superclass of " + getName() + " extends " + superTypeNames);
      }
      
      return null;
    }

    @Override
    public Collection<IClass> getDirectInterfaces() {
      List<IClass> result = new ArrayList<IClass>();
      for (Iterator iter = superTypeNames.iterator(); iter.hasNext();) {
        TypeName name = (TypeName) iter.next();
        IClass domoType = lookupClass(name);
        if (domoType != null && domoType.isInterface()) {
            result.add(domoType);
        }
        if (domoType == null && !getClassHierarchy().getScope().getExclusions().contains(name.toString().substring(1))){
          assert false : "Failed to find non-excluded interface: " + name;
        }
      }

      return result;
    }

    private void addMethod(CAstEntity methodEntity, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      declaredMethods.put(Util.methodEntityToSelector(methodEntity), new ConcreteJavaMethod(methodEntity, owner, cfg, symtab,
          hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo));
    }

    private void addMethod(CAstEntity methodEntity, IClass owner) {
      declaredMethods.put(Util.methodEntityToSelector(methodEntity), new AbstractJavaMethod(methodEntity, owner));
    }

    private void addField(CAstEntity fieldEntity) {
      declaredFields.put(Util.fieldEntityToAtom(fieldEntity), new JavaField(fieldEntity, JavaSourceLoaderImpl.this, this, JavaSourceLoaderImpl.this.getAnnotations(fieldEntity)));
    }

    public IClass getEnclosingClass() {
      return enclosingClass;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer("<src-class: " );
      sb.append(getName().toString());
      if (enclosingClass != null) {
        sb.append(" (within " + enclosingClass.getName() + ")");
      }
      if (annotations != null && !annotations.isEmpty()) {
        for(Annotation a : annotations) {
          sb.append("[" + a.getType().getName().getClassName() + "]");
        }
      }
      return sb.toString();
    }
  }

  private Collection<Annotation> getAnnotations(CAstEntity e) {
    Collection<CAstAnnotation> annotations = e.getAnnotations();
    if (annotations == null || annotations.isEmpty()) {
      return null;
    } else {
      Collection<Annotation> result = HashSetFactory.make();
      for(CAstAnnotation ca : annotations) {
        TypeName walaTypeName = toWALATypeName(ca.getType());
        TypeReference ref = TypeReference.findOrCreate(getReference(), walaTypeName);
        if (ca.getArguments() == null || ca.getArguments().isEmpty()) {
          result.add(Annotation.make(ref));
        } else {
          Map<String,ElementValue> args = HashMapFactory.make();
          for(Map.Entry<String, Object> a : ca.getArguments().entrySet()) {
            args.put(a.getKey(), new ConstantElementValue(a.getValue()));
          }
          result.add(Annotation.makeWithNamed(ref, args));
        }
      }
      return result;
    }
  }
  
  /**
   * WALA representation of a field on a Java type that resides in a source file
   * 
   * @author rfuhrer
   */
  private class JavaField extends AstField {
    private JavaField(CAstEntity fieldEntity, IClassLoader loader, IClass declaringClass, Collection<Annotation> annotations) {
      super(FieldReference.findOrCreate(declaringClass.getReference(), Atom.findOrCreateUnicodeAtom(fieldEntity.getName()),
          TypeReference.findOrCreate(loader.getReference(), TypeName.string2TypeName(fieldEntity.getType().getName()))),
          fieldEntity.getQualifiers(), declaringClass, declaringClass.getClassHierarchy(), annotations);
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
        Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(owner, methodEntity.getQualifiers(), cfg, symtab, MethodReference.findOrCreate(owner.getReference(), Util
          .methodEntityToSelector(methodEntity)), hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, JavaSourceLoaderImpl.this.getAnnotations(methodEntity));
      this.parameterTypes = computeParameterTypes(methodEntity);
      this.exceptionTypes = computeExceptionTypes(methodEntity);
    }

    public JavaEntityMethod(CAstEntity methodEntity, IClass owner) {
      super(owner, methodEntity.getQualifiers(), MethodReference.findOrCreate(owner.getReference(), Util
          .methodEntityToSelector(methodEntity)), JavaSourceLoaderImpl.this.getAnnotations(methodEntity));
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public String getLocalVariableName(int bcIndex, int localNumber) {
      Assertions.UNREACHABLE("AbstractJavaMethod.getLocalVariableName() called");
      return null;
    }

    @Override
    public boolean hasLocalVariableTable() {
      Assertions.UNREACHABLE("AbstractJavaMethod.hasLocalVariableTable() called");
      return false;
    }

    @Override
    public LexicalParent[] getParents() {
      return new LexicalParent[0];
    }

    @Override
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
        Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(methodEntity, owner, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo);
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

    @Override
    public boolean hasLocalVariableTable() {
      return false;
    }

    @Override
    public LexicalParent[] getParents() {
      if (AstTranslator.DEBUG_LEXICAL) {
        System.err.println(("resolving parents of " + this));
      }

      if (lexicalInfo() == null) {
        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println("no info");
        return new LexicalParent[0];
      }

      final String[] parents = lexicalInfo().getScopingParents();

      if (parents == null) {
        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println("no parents");
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
        Descriptor desc = Descriptor.findOrCreateUTF8(Language.JAVA, descStr);

        final Selector sel = new Selector(name, desc);

        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println(("get " + typeName + ", " + nameStr + ", " + descStr));

        final int hack = i;
        result[i] = new LexicalParent() {
          @Override
          public String getName() {
            return parents[hack];
          }

          @Override
          public AstMethod getMethod() {
            return (AstMethod) cls.getMethod(sel);
          }
        };

        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println(("parent " + result[i].getName() + " is " + result[i].getMethod()));
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

  @Override
  protected void loadAllSources(Set<ModuleEntry> modules) {
    getTranslator().loadAllSources(modules);
  }

  protected abstract SourceModuleTranslator getTranslator();

  @Override
  public void init(List<Module> modules) throws IOException {
    super.init(modules);
    fTypeMap = null;
  }

  public void defineFunction(CAstEntity n, IClass owner, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      Map<IBasicBlock, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    ((JavaClass) owner).addMethod(n, owner, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo);
  }

  public void defineAbstractFunction(CAstEntity n, IClass owner) {
    ((JavaClass) owner).addMethod(n, owner);
  }

  public void defineField(CAstEntity n, IClass owner) {
    ((JavaClass) owner).addField(n);
  }

  private TypeName toWALATypeName(CAstType type) {
    return TypeName.string2TypeName(type.getName());
  }
  
  public IClass defineType(CAstEntity type, String typeName, CAstEntity owner) {
    Collection<TypeName> superTypeNames = new ArrayList<TypeName>();
    for (Iterator superTypes = type.getType().getSupertypes().iterator(); superTypes.hasNext();) {
      superTypeNames.add(toWALATypeName(((CAstType) superTypes.next())));
    }

    JavaClass javaClass = new JavaClass(typeName, superTypeNames, type.getPosition(), type.getQualifiers(), this,
        (owner != null) ? (JavaClass) fTypeMap.get(owner) : (JavaClass) null, getAnnotations(type));

    if (getParent().lookupClass(javaClass.getName()) != null) {
      return null;
    }
    
    fTypeMap.put(type, javaClass);
    loadedClasses.put(javaClass.getName(), javaClass);
    return javaClass;
  }

  @Override
  public String toString() {
    return "Java Source Loader (classes " + loadedClasses.values() + ")";
  }
  
  public static class InstructionFactory extends JavaInstructionFactory implements AstJavaInstructionFactory {

    @Override
    public com.ibm.wala.cast.java.ssa.EnclosingObjectReference EnclosingObjectReference(int lval, TypeReference type) {
      return new EnclosingObjectReference(lval, type);
    }

    @Override
    public AstJavaNewEnclosingInstruction JavaNewEnclosingInstruction(int result, NewSiteReference site, int enclosing) {
      return new AstJavaNewEnclosingInstruction(result, site, enclosing);
    }

    @Override
    public AstJavaInvokeInstruction JavaInvokeInstruction(int result[], int[] params, int exception, CallSiteReference site) {
      return result == null ? new AstJavaInvokeInstruction(params, exception, site) : new AstJavaInvokeInstruction(result[0],
          params, exception, site);
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
      throw new UnsupportedOperationException();
    }

    @Override
    public com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction EachElementHasNextInstruction(int value, int objectRef) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstEchoInstruction EchoInstruction(int[] rvals) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstGlobalRead GlobalRead(int lhs, FieldReference global) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstGlobalWrite GlobalWrite(FieldReference global, int rhs) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal, FieldReference fieldRef) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, FieldReference fieldRef) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval) {
      throw new UnsupportedOperationException();
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

    public SSAThrowInstruction NonExceptingThrowInstruction(int exception) {
      throw new UnsupportedOperationException();
   }    
  }
  
  private static final InstructionFactory insts = new InstructionFactory();
  
  @Override
  public InstructionFactory getInstructionFactory() {
    return insts;
  }

}
