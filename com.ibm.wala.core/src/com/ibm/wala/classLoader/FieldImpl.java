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

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.annotations.TypeAnnotation;
import com.ibm.wala.types.generics.TypeSignature;
import com.ibm.wala.util.strings.Atom;

/**
 * Implementation of a canonical field reference. TODO: canonicalize these?
 * TODO: don't cache fieldType here .. move to class?
 */
public final class FieldImpl implements IField {

  private final IClass declaringClass;

  private final FieldReference fieldRef;

  private final int accessFlags;
  
  private final Collection<Annotation> annotations;
  
  private final Collection<TypeAnnotation> typeAnnotations;

  private final TypeSignature genericSignature;
  
  public FieldImpl(IClass declaringClass, FieldReference canonicalRef, int accessFlags, Collection<Annotation> annotations, TypeSignature sig) {
    this(declaringClass, canonicalRef, accessFlags, annotations, null, sig);
  }
  
  public FieldImpl(IClass declaringClass, FieldReference canonicalRef, int accessFlags, Collection<Annotation> annotations,
      Collection<TypeAnnotation> typeAnnotations, TypeSignature sig) {
    this.declaringClass = declaringClass;
    this.fieldRef = canonicalRef;
    this.accessFlags = accessFlags;
    this.annotations = annotations;
    this.typeAnnotations = typeAnnotations;
    this.genericSignature = sig;
    if (declaringClass == null) {
      throw new IllegalArgumentException("null declaringClass");
    }
    if (fieldRef == null) {
      throw new IllegalArgumentException("null canonicalRef");
    }
  }

  public FieldImpl(IClass declaringClass, FieldReference canonicalRef, int accessFlags, Collection<Annotation> annotations) {
    this(declaringClass, canonicalRef, accessFlags, annotations, null);
  }
  
  /**
   * @return the genericSignature
   */
  public TypeSignature getGenericSignature() {
    return genericSignature;
  }

  /*
   * @see com.ibm.wala.classLoader.IMember#getDeclaringClass()
   */
  @Override
  public IClass getDeclaringClass() {
    return declaringClass;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof FieldImpl) {
      FieldImpl other = (FieldImpl) obj;
      return fieldRef.equals(other.fieldRef) && declaringClass.equals(other.declaringClass);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 87049 * declaringClass.hashCode() + fieldRef.hashCode();
  }

  @Override
  public String toString() {
    FieldReference fr = getReference();
    return fr.toString();
  }

  @Override
  public FieldReference getReference() {
    return FieldReference.findOrCreate(getDeclaringClass().getReference(), getName(), getFieldTypeReference());
  }

  /*
   * @see com.ibm.wala.classLoader.IMember#getName()
   */
  @Override
  public Atom getName() {
    return fieldRef.getName();
  }

  /*
   * @see com.ibm.wala.classLoader.IField#getFieldTypeReference()
   */
  @Override
  public TypeReference getFieldTypeReference() {
    return fieldRef.getFieldType();
  }

  @Override
  public boolean isStatic() {
    return ((accessFlags & ClassConstants.ACC_STATIC) != 0);
  }

  @Override
  public boolean isFinal() {
    return ((accessFlags & ClassConstants.ACC_FINAL) != 0);
  }

  @Override
  public boolean isPrivate() {
    return ((accessFlags & ClassConstants.ACC_PRIVATE) != 0);
  }

  @Override
  public boolean isProtected() {
    return ((accessFlags & ClassConstants.ACC_PROTECTED) != 0);
  }

  @Override
  public boolean isPublic() {
    return ((accessFlags & ClassConstants.ACC_PUBLIC) != 0);
  }
  
  @Override
  public boolean isVolatile() {
    return ((accessFlags & ClassConstants.ACC_VOLATILE) != 0);
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return declaringClass.getClassHierarchy();
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return annotations == null ? null : Collections.unmodifiableCollection(annotations);
  }

  public Collection<TypeAnnotation> getTypeAnnotations() {
    return typeAnnotations == null ? null : Collections.unmodifiableCollection(typeAnnotations);
  }
}
