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


import com.ibm.wala.cast.tree.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;

public class AstField implements IField {
  private final Collection qualifiers;
  private final FieldReference ref;
  private final IClass declaringClass;
  private final ClassHierarchy cha;

  public AstField(FieldReference ref,
		  Collection qualifiers,
		  IClass declaringClass,
		  ClassHierarchy cha)
  {
    this.declaringClass = declaringClass;
    this.qualifiers = qualifiers;
    this.ref = ref;
    this.cha = cha;
  }

  public IClass getDeclaringClass() {
    return declaringClass;
  }
  
  public String toString() {
    return "field " + ref.getName();
  }

  public Atom getName() {
    return ref.getName();
  }

  public TypeReference getFieldTypeReference() {
    return ref.getFieldType();
  }
  
  public FieldReference getFieldReference() {
    return ref;
  }

  public boolean isStatic() {
    return qualifiers.contains(CAstQualifier.STATIC);
  }

  public boolean isFinal() {
    return qualifiers.contains(CAstQualifier.CONST);
  }

  public boolean isPrivate() {
    return qualifiers.contains(CAstQualifier.PRIVATE);
  }

  public boolean isProtected() {
    return qualifiers.contains(CAstQualifier.PROTECTED);
  }

  public boolean isPublic() {
    return qualifiers.contains(CAstQualifier.PUBLIC);
  }

  public boolean isVolatile() {
    return qualifiers.contains(CAstQualifier.VOLATILE);
  }

  public ClassHierarchy getClassHierarchy() {
    return cha;
  }
}
