/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PrimitiveMask;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Implementation of {@link IClass} for array classes. Such classes would be best called 'broken
 * covariant array types', since that is the semantics that they implement.
 */
public class ArrayClass implements IClass, Constants {

  private final IClassHierarchy cha;

  /**
   * Package-visible constructor; only for use by ArrayClassLoader class. 'loader' must be the
   * Primordial IClassLoader.
   *
   * <p>[WHY? -- array classes are loaded by the element classloader??]
   */
  ArrayClass(TypeReference type, IClassLoader loader, IClassHierarchy cha) {
    this.type = type;
    this.loader = loader;
    this.cha = cha;
    TypeReference elementType = type.getInnermostElementType();
    if (!elementType.isPrimitiveType()) {
      IClass klass = loader.lookupClass(elementType.getName());
      if (klass == null) {
        Assertions.UNREACHABLE("caller should not attempt to create an array with type " + type);
      }
    } else {
      // assert loader.getReference().equals(ClassLoaderReference.Primordial);
    }
  }

  private final TypeReference type;

  private final IClassLoader loader;

  @Override
  public IClassLoader getClassLoader() {
    return loader;
  }

  @Override
  public TypeName getName() {
    return getReference().getName();
  }

  /** Does this class represent an array of primitives? */
  public boolean isOfPrimitives() {
    return this.type.getInnermostElementType().isPrimitiveType();
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public int getModifiers() {
    return ACC_PUBLIC | ACC_FINAL;
  }

  public String getQualifiedNameForReflection() {
    return type.getName().toString(); // XXX is this the right string?
  }

  @Override
  public IClass getSuperclass() {
    IClass elt = getElementClass();
    assert getReference().getArrayElementType().isPrimitiveType() || elt != null;

    // super is Ljava/lang/Object in two cases:
    // 1) [Ljava/lang/Object
    // 2) [? for primitive arrays (null from getElementClass)
    if (elt == null || elt.getReference() == getClassLoader().getLanguage().getRootType()) {
      return loader.lookupClass(getClassLoader().getLanguage().getRootType().getName());
    }

    // else it is array of super of element type (yuck)
    else {
      TypeReference eltSuperRef = elt.getSuperclass().getReference();
      TypeReference superRef = TypeReference.findOrCreateArrayOf(eltSuperRef);
      return elt.getSuperclass().getClassLoader().lookupClass(superRef.getName());
    }
  }

  @Override
  public IMethod getMethod(Selector sig) {
    return cha.lookupClass(getClassLoader().getLanguage().getRootType()).getMethod(sig);
  }

  @Override
  public IField getField(Atom name) {
    return getSuperclass().getField(name);
  }

  @Override
  public IField getField(Atom name, TypeName typeName) {
    return getSuperclass().getField(name, typeName);
  }

  @Override
  public Collection<IMethod> getDeclaredMethods() {
    return Collections.emptySet();
  }

  public int getNumberOfDeclaredMethods() {
    return 0;
  }

  @Override
  public TypeReference getReference() {
    return type;
  }

  @Override
  public String getSourceFileName() {
    return null;
  }

  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  @Override
  public boolean isArrayClass() {
    return true;
  }

  @Override
  public String toString() {
    return getReference().toString();
  }

  /**
   * @return the IClass that represents the array element type, or null if the element type is a
   *     primitive
   */
  public IClass getElementClass() {
    TypeReference elementType = getReference().getArrayElementType();
    if (elementType.isPrimitiveType()) {
      return null;
    }
    return loader.lookupClass(elementType.getName());
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public Collection<IField> getDeclaredInstanceFields() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getDeclaredStaticFields() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    HashSet<IClass> result = HashSetFactory.make(2);
    for (TypeReference ref : getClassLoader().getLanguage().getArrayInterfaces()) {
      IClass klass = loader.lookupClass(ref.getName());
      if (klass != null) {
        result.add(klass);
      }
    }

    return result;
  }

  /** @see IClass#getAllImplementedInterfaces() */
  public Collection<IClass> getAllAncestorInterfaces() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isReferenceType() {
    return true;
  }

  public int getDimensionality() {
    return getArrayTypeDimensionality(getReference());
  }

  /**
   * @param reference a type reference for an array type
   * @return the dimensionality of the array
   */
  public static int getArrayTypeDimensionality(TypeReference reference) {
    int mask = reference.getDerivedMask();
    if ((mask & PrimitiveMask) == PrimitiveMask) {
      mask >>= ElementBits;
    }
    int dims = 0;
    while ((mask & ArrayMask) == ArrayMask) {
      mask >>= ElementBits;
      dims++;
    }
    assert dims > 0;
    return dims;
  }

  /**
   * @return the IClass that represents the innermost array element type, or null if the element
   *     type is a primitive
   */
  public IClass getInnermostElementClass() {
    TypeReference elementType = getReference().getInnermostElementType();
    if (elementType.isPrimitiveType()) {
      return null;
    }
    return loader.lookupClass(elementType.getName());
  }

  @Override
  public Collection<IClass> getDirectInterfaces() throws UnimplementedError {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ArrayClass) {
      ArrayClass other = (ArrayClass) obj;
      return loader.equals(other.loader) && type.equals(other.type);
    } else {
      return false;
    }
  }

  @Override
  public Collection<IField> getAllInstanceFields() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public Collection<IField> getAllStaticFields() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public Collection<? extends IMethod> getAllMethods() {
    return loader
        .lookupClass(getClassLoader().getLanguage().getRootType().getName())
        .getAllMethods();
  }

  @Override
  public Collection<IField> getAllFields() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public Reader getSource() {
    return null;
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return Collections.emptySet();
  }
}
