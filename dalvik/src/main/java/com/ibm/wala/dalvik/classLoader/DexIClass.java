/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import static org.jf.dexlib2.AccessFlags.ABSTRACT;
import static org.jf.dexlib2.AccessFlags.INTERFACE;
import static org.jf.dexlib2.AccessFlags.PRIVATE;
import static org.jf.dexlib2.AccessFlags.PUBLIC;
import static org.jf.dexlib2.AccessFlags.SYNTHETIC;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;

public class DexIClass extends BytecodeClass<IClassLoader> {

  /** Item which contains the class definitions. (compute by DexFile, from the dexLib) */
  private final ClassDef classDef;

  /**
   * Bitfields of these flags are used to indicate the accessibility and overall properties of
   * classes and class members. i.e. public/private/abstract/interface.
   */
  private final int modifiers;

  private IMethod[] methods = null;

  // private int construcorId = -1;

  private int clinitId = -1;

  private final DexModuleEntry dexModuleEntry;
  //    public IClassLoader loader;

  public DexIClass(IClassLoader loader, IClassHierarchy cha, final DexModuleEntry dexEntry) {
    super(loader, cha);
    this.dexModuleEntry = dexEntry;
    classDef = dexEntry.getClassDefItem();
    //        this.loader = loader;

    // Set modifiers
    modifiers = classDef.getAccessFlags();

    // computerTypeReference()
    // Set typeReference
    typeReference = TypeReference.findOrCreate(loader.getReference(), dexEntry.getClassName());

    // set hashcode
    hashCode = 2161 * getReference().hashCode();

    // computeSuperName()
    // Set Super Name;
    String descriptor = classDef.getSuperclass() != null ? classDef.getSuperclass() : null;
    if (descriptor != null && descriptor.endsWith(";"))
      descriptor = descriptor.substring(0, descriptor.length() - 1); // remove last ';'
    superName = descriptor != null ? ImmutableByteArray.make(descriptor) : null;

    // computeInterfaceNames()
    // Set interfaceNames
    final List<String> intfList = classDef.getInterfaces();
    int size = intfList == null ? 0 : intfList.size();
    // if (size != 0)
    //  System.out.println(intfList.getTypes().get(0).getTypeDescriptor());

    interfaceNames = new ImmutableByteArray[size];
    for (int i = 0; i < size; i++) {
      descriptor = intfList.get(i);
      if (descriptor.endsWith(";")) descriptor = descriptor.substring(0, descriptor.length() - 1);
      interfaceNames[i] = ImmutableByteArray.make(descriptor);
    }

    // Set direct instance fields
    //      if (classData == null) {
    //            throw new RuntimeException("DexIClass::DexIClass(): classData is null");
    //      }
    //      final EncodedField[] encInstFields = classData.getInstanceFields();
    //      size = encInstFields==null?0:encInstFields.length;
    //      instanceFields = new IField[size];
    //      for (int i = 0; i < size; i++) {
    //          //name of instance field.
    //          //System.out.println(encInstFields[i].field.getFieldName().getStringValue());
    //          //name of field type.
    //          //System.out.println(encInstFields[i].field.getFieldType().getTypeDescriptor());
    //          instanceFields[i] = new DexIField(encInstFields[i],this);
    //      }
    //
    //      // Set direct static fields
    //      final EncodedField[] encStatFields = classData.getStaticFields();
    //      size = encStatFields==null?0:encStatFields.length;
    //      staticFields = new IField[size];
    //      for (int i = 0; i < size; i++) {
    //          //name of static field
    //          //System.out.println(encInstFields[i].field.getFieldName().getStringValue());
    //          staticFields[i] = new DexIField(encStatFields[i],this);
    //      }

    // computeFields()
    // final EncodedField[] encInstFields = classData.getInstanceFields();

    final Iterable<? extends Field> encInstFields = classDef.getInstanceFields();
    List<IField> ifs = new ArrayList<>();
    for (Field dexf : encInstFields) {
      ifs.add(new DexIField(dexf, this));
    }
    instanceFields = ifs.toArray(new IField[0]);

    // Set direct static fields
    final Iterable<? extends Field> encStatFields = classDef.getStaticFields();
    List<IField> sfs = new ArrayList<>();
    for (Field dexf : encStatFields) {
      sfs.add(new DexIField(dexf, this));
    }
    staticFields = sfs.toArray(new IField[0]);
  }

  /** @return The classDef Item associated with this class. */
  public ClassDef getClassDefItem() {
    return classDef;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isPublic()
   */
  @Override
  public boolean isPublic() {
    return (modifiers & PUBLIC.getValue()) != 0;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isPrivate()
   */
  @Override
  public boolean isPrivate() {
    return (modifiers & PRIVATE.getValue()) != 0;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isInterface()
   */
  @Override
  public boolean isInterface() {
    return (modifiers & INTERFACE.getValue()) != 0;
  }

  /** @see com.ibm.wala.classLoader.IClass#isAbstract() */
  @Override
  public boolean isAbstract() {
    return (modifiers & ABSTRACT.getValue()) != 0;
  }

  /** @see com.ibm.wala.classLoader.IClass#isAbstract() */
  @Override
  public boolean isSynthetic() {
    return (modifiers & SYNTHETIC.getValue()) != 0;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  @Override
  public int getModifiers() throws UnsupportedOperationException {
    return modifiers;
  }

  /** @see java.lang.Object#equals(Object) */
  @Override
  public boolean equals(Object obj) {
    // it's ok to use instanceof since this class is final
    // if (this.getClass().equals(obj.getClass())) {
    if (obj instanceof DexIClass) {
      return getReference().equals(((DexIClass) obj).getReference());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  Collection<Annotation> getAnnotations(Set<String> types) {
    Set<Annotation> result = HashSetFactory.make();
    for (org.jf.dexlib2.iface.Annotation a : classDef.getAnnotations()) {
      if (types == null || types.contains(AnnotationVisibility.getVisibility(a.getVisibility()))) {
        result.add(DexUtil.getAnnotation(a, getClassLoader().getReference()));
      }
    }
    return result;
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return getAnnotations((Set<String>) null);
  }

  @Override
  public Collection<Annotation> getAnnotations(boolean runtimeInvisible) {
    return getAnnotations(getTypes(runtimeInvisible));
  }

  static Set<String> getTypes(boolean runtimeInvisible) {
    Set<String> types = HashSetFactory.make();
    types.add(AnnotationVisibility.getVisibility(AnnotationVisibility.SYSTEM));
    if (runtimeInvisible) {
      types.add(AnnotationVisibility.getVisibility(AnnotationVisibility.BUILD));
    } else {
      types.add(AnnotationVisibility.getVisibility(AnnotationVisibility.RUNTIME));
    }
    return types;
  }

  List<Annotation> getAnnotations(Method m, Set<String> set) {
    List<Annotation> result = new ArrayList<>();
    for (org.jf.dexlib2.iface.Annotation a : m.getAnnotations()) {
      if (set == null || set.contains(AnnotationVisibility.getVisibility(a.getVisibility()))) {
        result.add(DexUtil.getAnnotation(a, getClassLoader().getReference()));
      }
    }
    return result;
  }

  Collection<Annotation> getAnnotations(Field m) {
    List<Annotation> result = new ArrayList<>();
    for (org.jf.dexlib2.iface.Annotation a : m.getAnnotations()) {
      result.add(DexUtil.getAnnotation(a, getClassLoader().getReference()));
    }
    return result;
  }

  Map<Integer, List<Annotation>> getParameterAnnotations(Method m) {
    Map<Integer, List<Annotation>> result = HashMapFactory.make();
    int i = 0;
    for (MethodParameter as : m.getParameters()) {
      for (org.jf.dexlib2.iface.Annotation a : as.getAnnotations()) {
        if (!result.containsKey(i)) {
          result.put(i, new ArrayList<>());
        }
        result.get(i).add(DexUtil.getAnnotation(a, getClassLoader().getReference()));
      }
      i++;
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.BytecodeClass#computeDeclaredMethods()
   */
  @Override
  protected IMethod[] computeDeclaredMethods() {
    ArrayList<IMethod> methodsAL = new ArrayList<>();

    if (methods == null) {
      //            final EncodedMethod[] directMethods =
      // classDef.getClassData().getDirectMethods();
      //            final EncodedMethod[] virtualMethods =
      // classDef.getClassData().getVirtualMethods();
      final Iterable<? extends Method> directMethods = classDef.getDirectMethods();
      final Iterable<? extends Method> virtualMethods = classDef.getVirtualMethods();

      // methods = new IMethod[dSize+vSize];

      // Create Direct methods (static, private, constructor)
      int i = 0;
      for (Method dMethod : directMethods) {
        // methods[i] = new DexIMethod(dMethod,this);
        DexIMethod method = new DexIMethod(dMethod, this);
        methodsAL.add(method);

        // Set construcorId
        // if ( (dMethod.accessFlags & CONSTRUCTOR.getValue()) != 0){
        //    construcorId = i;
        // }
        // Set clinitId
        // if (methods[i].isClinit())
        if (method.isClinit()) {
          clinitId = i;
        }
        i++;
      }

      // Create virtual methods (other methods)
      for (Method dexm : virtualMethods) {
        // methods[dSize+i] = new DexIMethod(virtualMethods[i],this);
        methodsAL.add(new DexIMethod(dexm, this));
        // is this enough to determine if the class is an activity?
        // maybe check superclass?  -- but that may also not be enough
        // may need to keep checking superclass of superclass, etc.

      }
    }

    if (methods == null) methods = methodsAL.toArray(new IMethod[0]);

    return methods;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  @Override
  public IMethod getClassInitializer() {
    if (methods == null) {
      computeDeclaredMethods();
    }
    //      return construcorId!=-1?methods[construcorId]:null;
    return clinitId != -1 ? methods[clinitId] : null;
  }

  @Override
  public DexFileModule getContainer() {
    return dexModuleEntry.getContainer();
  }
}
