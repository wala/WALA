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

import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * Implementation of a canonical field reference. TODO: canonicalize these?
 * TODO: don't cache fieldType here .. move to class?
 * 
 * @author sfink
 */
public final class FieldImpl implements IField {

  private final IClass declaringClass;
  private final FieldReference fieldRef;
  private final int accessFlags;

  /**
   * constructor when the field type is not a primitive
   */
  public FieldImpl(IClass declaringClass, FieldReference canonicalRef,int accessFlags) {
    this.declaringClass = declaringClass;
    this.fieldRef = canonicalRef;
    this.accessFlags = accessFlags;
    if (Assertions.verifyAssertions) {
      Assertions._assert(declaringClass != null);
      Assertions._assert(fieldRef != null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMember#getDeclaringClass()
   */
  public IClass getDeclaringClass() {
    return declaringClass;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof FieldImpl) {
      FieldImpl other = (FieldImpl) obj;
      return fieldRef.equals(other.fieldRef)  && declaringClass.equals(other.declaringClass);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 87049 * declaringClass.hashCode() + fieldRef.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    FieldReference fr = getFieldReference();
    return fr.toString();
  }

  public FieldReference getFieldReference() {
    return FieldReference.findOrCreate(getDeclaringClass().getReference(), getName(), getFieldTypeReference());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMember#getName()
   */
  public Atom getName() {
    return fieldRef.getName();
  }

  /*
   * (non-Javadoc)
   * 
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
}