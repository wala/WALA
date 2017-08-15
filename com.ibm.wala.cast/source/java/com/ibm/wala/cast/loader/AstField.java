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
package com.ibm.wala.cast.loader;


import java.util.Collection;

import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

public class AstField implements IField {
  private final Collection<CAstQualifier> qualifiers;
  private final FieldReference ref;
  private final IClass declaringClass;
  private final IClassHierarchy cha;
  private final Collection<Annotation> annotations;

  public AstField(FieldReference ref,
		  Collection<CAstQualifier> qualifiers,
		  IClass declaringClass,
		  IClassHierarchy cha,
		  Collection<Annotation> annotations)
  {
    this.declaringClass = declaringClass;
    this.qualifiers = qualifiers;
    this.ref = ref;
    this.cha = cha;
    this.annotations = annotations;
  }

  
  @Override
  public Collection<Annotation> getAnnotations() {
    return annotations;
  }

  @Override
  public IClass getDeclaringClass() {
    return declaringClass;
  }
  
  @Override
  public String toString() {
    return "field " + ref.getName();
  }

  @Override
  public Atom getName() {
    return ref.getName();
  }

  @Override
  public TypeReference getFieldTypeReference() {
    return ref.getFieldType();
  }
  
  @Override
  public FieldReference getReference() {
    return ref;
  }

  @Override
  public boolean isStatic() {
    return qualifiers.contains(CAstQualifier.STATIC);
  }

  @Override
  public boolean isFinal() {
    return qualifiers.contains(CAstQualifier.CONST) || qualifiers.contains(CAstQualifier.FINAL);
  }

  @Override
  public boolean isPrivate() {
    return qualifiers.contains(CAstQualifier.PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return qualifiers.contains(CAstQualifier.PROTECTED);
  }

  @Override
  public boolean isPublic() {
    return qualifiers.contains(CAstQualifier.PUBLIC);
  }

  @Override
  public boolean isVolatile() {
    return qualifiers.contains(CAstQualifier.VOLATILE);
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }
}
