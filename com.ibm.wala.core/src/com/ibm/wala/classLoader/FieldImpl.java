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

  public FieldImpl(IClass declaringClass, FieldReference canonicalRef, int accessFlags, Collection<Annotation> annotations) {
    this.declaringClass = declaringClass;
    this.fieldRef = canonicalRef;
    this.accessFlags = accessFlags;
    this.annotations = annotations;
    if (declaringClass == null) {
      throw new IllegalArgumentException("null declaringClass");
    }
    if (fieldRef == null) {
      throw new IllegalArgumentException("null canonicalRef");
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IMember#getDeclaringClass()
   */
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

  public FieldReference getReference() {
    return FieldReference.findOrCreate(getDeclaringClass().getReference(), getName(), getFieldTypeReference());
  }

  /*
   * @see com.ibm.wala.classLoader.IMember#getName()
   */
  public Atom getName() {
    return fieldRef.getName();
  }

  /*
   * @see com.ibm.wala.classLoader.IField#getFieldTypeReference()
   */
  public TypeReference getFieldTypeReference() {
    return fieldRef.getFieldType();
  }

  public boolean isStatic() {
    return ((accessFlags & ClassConstants.ACC_STATIC) != 0);
  }

  public boolean isFinal() {
    return ((accessFlags & ClassConstants.ACC_FINAL) != 0);
  }

  public boolean isPrivate() {
    return ((accessFlags & ClassConstants.ACC_PRIVATE) != 0);
  }

  public boolean isProtected() {
    return ((accessFlags & ClassConstants.ACC_PROTECTED) != 0);
  }

  public boolean isPublic() {
    return ((accessFlags & ClassConstants.ACC_PUBLIC) != 0);
  }
  
  public boolean isVolatile() {
    return ((accessFlags & ClassConstants.ACC_VOLATILE) != 0);
  }

  public IClassHierarchy getClassHierarchy() {
    return declaringClass.getClassHierarchy();
  }

  public Collection<Annotation> getAnnotations() {
    return annotations == null ? null : Collections.unmodifiableCollection(annotations);
  }

 
}