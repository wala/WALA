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
package com.ibm.wala.classLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyWarning;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.RuntimeInvisibleAnnotationsReader;
import com.ibm.wala.shrikeCT.SignatureReader;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.RuntimeInvisibleAnnotationsReader.UnimplementedException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.generics.ClassSignature;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.ShrikeClassReaderHandle;
import com.ibm.wala.util.collections.BimodalMap;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.SmallMap;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A class read from Shrike
 * 
 * @author sfink
 */
public final class ShrikeClass implements IClass {

  static final boolean DEBUG = false;

  /**
   * The Shrike object that knows how to read the class file
   */
  private final ShrikeClassReaderHandle reader;

  /**
   * The object that loaded this class.
   */
  private final IClassLoader loader;

  /**
   * Governing class hierarchy for this class
   */
  private final IClassHierarchy cha;

  /**
   * A mapping from Selector to IMethod
   * 
   * TODO: get rid of this for classes (though keep it for interfaces) instead
   * ... use a VMT.
   */
  private Map<Selector, IMethod> methodMap;

  /**
   * A mapping from Selector to IMethod used to cache method lookups from
   * superclasses
   */
  private Map<Selector, IMethod> inheritCache;

  /**
   * Canonical type representation
   */
  private TypeReference typeReference;

  /**
   * An object to track warnings
   */
  private final WarningSet warnings;

  /**
   * superclass
   */
  private IClass superClass;

  /**
   * Compute the superclass lazily.
   */
  private boolean superclassComputed = false;

  /**
   * An Atom which holds the name of the super class. We cache this for
   * efficiency reasons.
   */
  private ImmutableByteArray superName;

  /**
   * The names of interfaces for this class. We cache this for efficiency
   * reasons.
   */
  private ImmutableByteArray[] interfaceNames;

  /**
   * The IClasses that represent all interfaces this class implements (if it's a
   * class) or extends (it it's an interface)
   */
  private Collection<IClass> allInterfaces = null;

  /**
   * The instance fields declared in this class.
   */
  private IField[] instanceFields;

  /**
   * The static fields declared in this class.
   */
  private IField[] staticFields;

  /**
   * JVM-level modifiers; cached here for efficiency
   */
  private int modifiers;

  /**
   * hash code; cached here for efficiency
   */
  private final int hashCode;

  /**
   * @throws IllegalArgumentException
   *           if reader is null
   */
  public ShrikeClass(ShrikeClassReaderHandle reader, IClassLoader loader, IClassHierarchy cha, WarningSet warnings)
      throws InvalidClassFileException {
    if (reader == null) {
      throw new IllegalArgumentException("reader is null");
    }
    this.reader = reader;
    this.loader = loader;
    this.cha = cha;
    this.warnings = warnings;
    computeTypeReference();
    this.hashCode = 2161 * getReference().hashCode();
    // as long as the reader is around, pull more data out
    // of it before the soft reference to it disappears
    computeSuperName();
    computeModifiers();
    computeInterfaceNames();
    computeFields();
  }

  /**
   * Compute the fields declared by this class
   * 
   * @throws InvalidClassFileException
   *           iff Shrike fails to read the class file correctly
   */
  private void computeFields() throws InvalidClassFileException {
    ClassReader cr = reader.get();
    int fieldCount = cr.getFieldCount();
    List<FieldImpl> instanceList = new ArrayList<FieldImpl>(fieldCount);
    List<FieldImpl> staticList = new ArrayList<FieldImpl>(fieldCount);
    try {
      for (int i = 0; i < fieldCount; i++) {
        int accessFlags = cr.getFieldAccessFlags(i);
        Atom name = Atom.findOrCreateUnicodeAtom(cr.getFieldName(i));
        ImmutableByteArray b = ImmutableByteArray.make(cr.getFieldType(i));
        Collection<Annotation> annotations = null;
        try {
          annotations = getRuntimeInvisibleAnnotations(i);
          annotations = annotations.isEmpty() ? null : annotations;
        } catch (UnimplementedException e) {
          e.printStackTrace();
          // keep going
        }
      

        if ((accessFlags & ClassConstants.ACC_STATIC) == 0) {
          addFieldToList(instanceList, name, b, accessFlags, annotations);
        } else {
          addFieldToList(staticList, name, b, accessFlags, annotations);
        }
      }
      instanceFields = new IField[instanceList.size()];
      populateFieldArrayFromList(instanceList, instanceFields);
      staticFields = new IField[staticList.size()];
      populateFieldArrayFromList(staticList, staticFields);

    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  private void populateFieldArrayFromList(List<FieldImpl> L, IField[] A) {
    Iterator<FieldImpl> it = L.iterator();
    for (int i = 0; i < A.length; i++) {
      A[i] = it.next();
    }
  }

  private void addFieldToList(List<FieldImpl> L, Atom name, ImmutableByteArray fieldType, int accessFlags, Collection<Annotation> annotations) {
    TypeName T = null;
    if (fieldType.get(fieldType.length() - 1) == ';') {
      T = TypeName.findOrCreate(fieldType, 0, fieldType.length() - 1);
    } else {
      T = TypeName.findOrCreate(fieldType);
    }
    TypeReference type = TypeReference.findOrCreate(getClassLoader().getReference(), T);
    FieldReference fr = FieldReference.findOrCreate(getReference(), name, type);
    FieldImpl f = new FieldImpl(this, fr, accessFlags, annotations);
    L.add(f);
  }

  public IClassLoader getClassLoader() {
    return loader;
  }

  public boolean isInterface() {
    boolean result = ((modifiers & Constants.ACC_INTERFACE) != 0);
    return result;

  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isAbstract()
   */
  public boolean isAbstract() {
    boolean result = ((modifiers & Constants.ACC_ABSTRACT) != 0);
    return result;
  }

  /**
   * @throws InvalidClassFileException
   */
  private void computeModifiers() throws InvalidClassFileException {
    modifiers = reader.get().getAccessFlags();
  }

  public int getModifiers() {
    return modifiers;
  }

  /**
   * Note that this is called from the constructor, at which point this class is
   * not yet ready to actually load the superclass. Instead, we pull out the
   * name of the superclass and cache it here, to avoid hitting the reader
   * later.
   */
  private void computeSuperName() {
    try {
      String s = reader.get().getSuperName();
      if (s != null) {
        superName = ImmutableByteArray.make("L" + s);
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
  }

  private void computeSuperclass() {
    superclassComputed = true;

    if (superName == null) {
      if (!getReference().equals(TypeReference.JavaLangObject)) {
        superClass = loader.lookupClass(TypeReference.JavaLangObject.getName(), getClassHierarchy());
      }
      return;
    }

    superClass = loader.lookupClass(TypeName.findOrCreate(superName), getClassHierarchy());
    if (DEBUG) {
      Trace.println("got superclass " + superClass + " for " + this);
    }
  }

  public IClass getSuperclass() throws ClassHierarchyException {
    if (!superclassComputed) {
      computeSuperclass();
    }
    if (superClass == null && !getReference().equals(TypeReference.JavaLangObject)) {
      try {
        throw new ClassHierarchyException("No superclass " + reader.get().getSuperName() + " found for " + this);
      } catch (InvalidClassFileException e) {
        Assertions.UNREACHABLE();
      }
    }
    return superClass;
  }

  /**
   * Note that this is called from the constructor, at which point this class is
   * not yet ready to actually load the interfaces. Instead, we pull out the
   * name of the interfaces and cache it here, to avoid hitting the reader
   * later.
   */
  private void computeInterfaceNames() {
    try {
      String[] s = reader.get().getInterfaceNames();
      interfaceNames = new ImmutableByteArray[s.length];
      for (int i = 0; i < interfaceNames.length; i++) {
        interfaceNames[i] = ImmutableByteArray.make("L" + s[i]);
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Method getAllInterfacesAsCollection.
   * 
   * @return Collection of IClasses, representing the interfaces this class
   *         implements.
   */
  private Collection<IClass> computeAllInterfacesAsCollection() throws ClassHierarchyException {
    Collection<IClass> c = getDirectInterfaces();
    Set<IClass> result = HashSetFactory.make();
    for (Iterator<IClass> it = c.iterator(); it.hasNext();) {
      IClass klass = it.next();
      if (klass.isInterface()) {
        result.add(klass);
      } else {
        warnings.add(ClassHierarchyWarning.create("expected an interface " + klass));
      }
    }
    for (Iterator<IClass> it = c.iterator(); it.hasNext();) {
      ShrikeClass I = (ShrikeClass) it.next();
      if (I.isInterface()) {
        result.addAll(I.computeAllInterfacesAsCollection());
      } else {
        warnings.add(ClassHierarchyWarning.create("expected an interface " + I));
      }
    }

    // now add any interfaces from the super class
    ShrikeClass sup = null;
    try {
      sup = (ShrikeClass) getSuperclass();
    } catch (ClassHierarchyException e1) {
      Assertions.UNREACHABLE();
    }
    if (sup != null) {
      result.addAll(sup.computeAllInterfacesAsCollection());
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
   */
  public Collection<IClass> getDirectInterfaces() {
    return array2IClassSet(interfaceNames);
  }

  /**
   * Method array2Set.
   * 
   * @param interfaces
   *          a set of class names
   * @return Set of all IClasses that can be loaded corresponding to the class
   *         names in the interfaces array; raise warnings if classes can not be
   *         loaded
   */
  private Collection<IClass> array2IClassSet(ImmutableByteArray[] interfaces) {
    ArrayList<IClass> result = new ArrayList<IClass>(interfaces.length);
    for (int i = 0; i < interfaces.length; i++) {
      ImmutableByteArray name = interfaces[i];
      IClass klass = null;
      klass = loader.lookupClass(TypeName.findOrCreate(name), getClassHierarchy());
      if (klass == null) {
        warnings.add(ClassNotFoundWarning.create(name));
      } else {
        result.add(klass);
      }
    }
    return result;
  }

  /**
   * @author sfink
   * 
   * A warning for when we get a class not found exception
   */
  private static class ClassNotFoundWarning extends Warning {

    final ImmutableByteArray className;

    ClassNotFoundWarning(ImmutableByteArray className) {
      super(Warning.SEVERE);
      this.className = className;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + className;
    }

    public static ClassNotFoundWarning create(ImmutableByteArray className) {
      return new ClassNotFoundWarning(className);
    }
  }

  /**
   * set up the methodMap mapping
   */
  private void computeMethodMap() throws InvalidClassFileException {

    if (methodMap == null) {
      ShrikeCTMethod[] methods = computeDeclaredMethods();
      if (methods.length > 5) {
        methodMap = HashMapFactory.make(methods.length);
      } else {
        methodMap = new SmallMap<Selector, IMethod>();
      }
      for (int i = 0; i < methods.length; i++) {
        ShrikeCTMethod m = methods[i];
        methodMap.put(m.getReference().getSelector(), m);
      }
    }
  }

  /**
   * initialize the declared methods array
   * 
   * @throws InvalidClassFileException
   */
  private ShrikeCTMethod[] computeDeclaredMethods() throws InvalidClassFileException {
    int methodCount = reader.get().getMethodCount();
    ShrikeCTMethod[] result = new ShrikeCTMethod[methodCount];
    for (int i = 0; i < methodCount; i++) {
      ShrikeCTMethod m = new ShrikeCTMethod(this, i);
      if (DEBUG) {
        Trace.println("Register method " + m + " for class " + this);
      }
      result[i] = m;
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.types.Selector)
   */
  public IMethod getMethod(Selector selector) {
    if (DEBUG) {
      Trace.println("getMethod " + selector + " in " + this);
    }

    if (methodMap == null) {
      try {
        computeMethodMap();
      } catch (InvalidClassFileException e1) {
        e1.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }

    // my methods + cached parent stuff
    IMethod result = methodMap.get(selector);
    if (result != null) {
      return result;
    }
    if (inheritCache != null) {
      result = inheritCache.get(selector);
      if (result != null) {
        return result;
      }
    }

    // check parent, caching if found
    try {
      if (!selector.equals(MethodReference.clinitSelector) && !selector.equals(MethodReference.initSelector)) {
        ShrikeClass superclass = (ShrikeClass) getSuperclass();
        if (superclass != null) {
          IMethod inherit = superclass.getMethod(selector);
          if (inherit != null) {
            if (inheritCache == null) {
              inheritCache = new BimodalMap<Selector, IMethod>(5);
            }
            inheritCache.put(selector, inherit);
            return inherit;
          }
        }
      }
    } catch (ClassHierarchyException e) {
      Assertions.UNREACHABLE();
    }

    // didn't find it yet. special logic for interfaces
    try {
      if (isInterface() || isAbstract()) {
        final Iterator<IClass> it = (isInterface()) ? getAllAncestorInterfaces().iterator() : getAllImplementedInterfaces()
            .iterator();
        // try each superinterface
        while (it.hasNext()) {
          IClass k = it.next();
          result = k.getMethod(selector);
          if (result != null) {
            return result;
          }
        }
      }
    } catch (ClassHierarchyException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("Bad method lookup in " + this);
    }
    return null;

  }

  private final HashMap<Atom, IField> fieldMap = new HashMap<Atom, IField>(5);

  /*
   * @see com.ibm.wala.classLoader.IClass#getField(com.ibm.wala.util.Atom)
   */
  public IField getField(Atom name) {
    if (fieldMap.containsKey(name)) {
      return fieldMap.get(name);
    } else {
      IField f = findDeclaredField(name);
      if (f != null) {
        fieldMap.put(name, f);
        return f;
      } else if (superClass != null) {
        f = superClass.getField(name);
        if (f != null) {
          fieldMap.put(name, f);
          return f;
        }
      }
      // try superinterfaces
      try {
        Collection<IClass> ifaces = isInterface() ? getAllAncestorInterfaces() : getAllImplementedInterfaces();
        for (IClass i : ifaces) {
          f = i.getField(name);
          if (f != null) {
            fieldMap.put(name, f);
            return f;
          }
        }
      } catch (ClassHierarchyException e) {
        // skip
      }
    }

    return null;
  }

  private IField findDeclaredField(Atom name) {
    for (int i = 0; i < instanceFields.length; i++) {
      if (instanceFields[i].getName() == name) {
        return instanceFields[i];
      }
    }

    for (int i = 0; i < staticFields.length; i++) {
      if (staticFields[i].getName() == name) {
        return staticFields[i];
      }
    }

    return null;
  }

  /**
   * initialize the TypeReference field for this instance
   * 
   * @throws InvalidClassFileException
   *           iff Shrike can't read this class
   */
  private void computeTypeReference() throws InvalidClassFileException {
    String className = "L" + reader.get().getName();
    ImmutableByteArray name = ImmutableByteArray.make(className);

    typeReference = TypeReference.findOrCreate(getClassLoader().getReference(), TypeName.findOrCreate(name));
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getReference()
   */
  public TypeReference getReference() {
    return typeReference;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getSourceFileName()
   */
  public String getSourceFileName() {
    return loader.getSourceFileName(this);
  }

  public Collection<IClass> getAllImplementedInterfaces() throws ClassHierarchyException {
    if (Assertions.verifyAssertions) {
      if (isInterface()) {
        Assertions.UNREACHABLE("shouldn't ask for implemented interfaces of an interface " + this);
      }
    }
    if (allInterfaces != null) {
      return allInterfaces;
    } else {
      Collection<IClass> C = computeAllInterfacesAsCollection();
      allInterfaces = Collections.unmodifiableCollection(C);
      return allInterfaces;
    }
  }

  public Collection<IClass> getAllAncestorInterfaces() throws ClassHierarchyException {
    if (Assertions.verifyAssertions) {
      if (!isInterface()) {
        Assertions.UNREACHABLE();
      }
    }
    if (allInterfaces != null) {
      return allInterfaces;
    } else {
      Collection<IClass> C = computeAllInterfacesAsCollection();
      allInterfaces = Collections.unmodifiableCollection(C);
      return allInterfaces;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getReference().toString();
  }

  /**
   * @see java.lang.Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    // it's ok to use instanceof since this class is final
    // if (this.getClass().equals(obj.getClass())) {
    if (obj instanceof ShrikeClass) {
      return getReference().equals(((ShrikeClass) obj).getReference());
    } else {
      return false;
    }
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Method getReader.
   */
  public ClassReader getReader() {
    try {
      return reader.get();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  public IMethod getClassInitializer() {
    if (methodMap == null) {
      try {
        computeMethodMap();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
    return methodMap.get(MethodReference.clinitSelector);
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
   */
  public Collection<IMethod> getDeclaredMethods() {
    if (methodMap == null) {
      try {
        computeMethodMap();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
    return Collections.unmodifiableCollection(methodMap.values());
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isArrayClass()
   */
  public boolean isArrayClass() {
    return false;
  }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  public WarningSet getWarnings() {
    return warnings;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
   */
  public Collection<IField> getDeclaredInstanceFields() {
    return Collections.unmodifiableList(Arrays.asList(instanceFields));
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
   */
  public Collection<IField> getDeclaredStaticFields() {
    return Collections.unmodifiableList(Arrays.asList(staticFields));
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getName()
   */
  public TypeName getName() {
    return getReference().getName();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /**
   * Clear all optional cached data associated with this class
   */
  public void clearSoftCaches() {
    // toss optional information from each method.
    if (methodMap != null) {
      for (Iterator it = getDeclaredMethods().iterator(); it.hasNext();) {
        ShrikeCTMethod m = (ShrikeCTMethod) it.next();
        m.clearCaches();
      }
    }
    // clear the methodMap cache
    // SJF: don't do this!!! makes it hard to clear caches on methods.
    // methodMap = null;
    inheritCache = null;
    // clear the cached interfaces
    allInterfaces = null;
    // toss away the Shrike reader
    reader.clear();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
   */
  public Collection<IField> getAllInstanceFields() throws ClassHierarchyException {
    Collection<IField> result = new LinkedList<IField>(getDeclaredInstanceFields());
    IClass s = getSuperclass();
    while (s != null) {
      result.addAll(s.getDeclaredInstanceFields());
      s = s.getSuperclass();
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
   */
  public Collection<IField> getAllStaticFields() throws ClassHierarchyException {
    Collection<IField> result = new LinkedList<IField>(getDeclaredStaticFields());
    IClass s = getSuperclass();
    while (s != null) {
      result.addAll(s.getDeclaredStaticFields());
      s = s.getSuperclass();
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllMethods()
   */
  public Collection<IMethod> getAllMethods() throws ClassHierarchyException {
    Collection<IMethod> result = new LinkedList<IMethod>();
    Iterator<IMethod> declaredMethods = getDeclaredMethods().iterator();
    while (declaredMethods.hasNext()) {
      result.add(declaredMethods.next());
    }
    IClass s = getSuperclass();
    while (s != null) {
      Iterator<IMethod> superDeclaredMethods = s.getDeclaredMethods().iterator();
      while (superDeclaredMethods.hasNext()) {
        result.add(superDeclaredMethods.next());
      }
      s = s.getSuperclass();
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllFields()
   */
  public Collection<IField> getAllFields() throws ClassHierarchyException {
    Collection<IField> result = new LinkedList<IField>();
    result.addAll(getAllInstanceFields());
    result.addAll(getAllStaticFields());
    return result;
  }

  public boolean isPublic() {
    boolean result = ((modifiers & Constants.ACC_PUBLIC) != 0);
    return result;
  }

  public Collection<Annotation> getRuntimeInvisibleAnnotations() throws InvalidClassFileException, UnimplementedException {
    RuntimeInvisibleAnnotationsReader r = getRuntimeInvisibleAnnotationsReader();
    if (r != null) {
      int[] offsets = r.getAnnotationOffsets();
      Collection<Annotation> result = HashSetFactory.make();
      for (int i : offsets) {
        String type = r.getAnnotationType(i);
        type = type.replaceAll(";", "");
        TypeReference t = TypeReference.findOrCreate(getClassLoader().getReference(), type);
        result.add(Annotation.make(t));
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }

  private RuntimeInvisibleAnnotationsReader getRuntimeInvisibleAnnotationsReader() throws InvalidClassFileException {
    ClassReader r = reader.get();
    ClassReader.AttrIterator attrs = new ClassReader.AttrIterator();
    r.initClassAttributeIterator(attrs);

    // search for the desired attribute
    RuntimeInvisibleAnnotationsReader result = null;
    try {
      for (; attrs.isValid(); attrs.advance()) {
        if (attrs.getName().toString().equals("RuntimeInvisibleAnnotations")) {
          result = new RuntimeInvisibleAnnotationsReader(attrs);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  private RuntimeInvisibleAnnotationsReader getRuntimeInvisibleAnnotationsReader(int fieldIndex) throws InvalidClassFileException {
    ClassReader.AttrIterator iter = new AttrIterator();
    reader.get().initFieldAttributeIterator(fieldIndex, iter);

    // search for the desired attribute
    RuntimeInvisibleAnnotationsReader result = null;
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().toString().equals("RuntimeInvisibleAnnotations")) {
          result = new RuntimeInvisibleAnnotationsReader(iter);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }
  
  /**
   * read the runtime-invisible annotations from the class file
   */
  public Collection<Annotation> getRuntimeInvisibleAnnotations(int fieldIndex) throws InvalidClassFileException, UnimplementedException {
    RuntimeInvisibleAnnotationsReader r = getRuntimeInvisibleAnnotationsReader(fieldIndex);
    if (r != null) {
      int[] offsets = r.getAnnotationOffsets();
      Collection<Annotation> result = HashSetFactory.make();
      for (int i : offsets) {
        String type = r.getAnnotationType(i);
        type = type.replaceAll(";","");
        TypeReference t = TypeReference.findOrCreate(getClassLoader().getReference(), type);
        result.add(Annotation.make(t));
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }
  
  private SignatureReader getSignatureReader() throws InvalidClassFileException {
    ClassReader r = reader.get();
    ClassReader.AttrIterator attrs = new ClassReader.AttrIterator();
    r.initClassAttributeIterator(attrs);

    // search for the desired attribute
    SignatureReader result = null;
    try {
      for (; attrs.isValid(); attrs.advance()) {
        if (attrs.getName().toString().equals("Signature")) {
          result = new SignatureReader(attrs);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  public ClassSignature getClassSignature() throws InvalidClassFileException {
    // TODO: cache this later?
    SignatureReader r = getSignatureReader();
    if (r == null) {
      return null;
    } else {
      return ClassSignature.make(r.getSignature());
    }
  }
}