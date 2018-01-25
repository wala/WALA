/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.util.debug.Assertions;

public class AbstractFieldEntity extends AbstractDataEntity {
  private final String name;

  private final Set<CAstQualifier> modifiers;

  private final CAstEntity declaringClass;

  public AbstractFieldEntity(String name, Set<CAstQualifier> modifiers, boolean isStatic, CAstEntity declaringClass) {
    this.name = name;
    this.declaringClass = declaringClass;

    this.modifiers = new HashSet<>();
    if (modifiers != null) {
      this.modifiers.addAll(modifiers);
    }
    if (isStatic) {
      this.modifiers.add(CAstQualifier.STATIC);
    }
  }

  @Override
  public String toString() {
    return "field " + name + " of " + declaringClass.getName();
  }

  @Override
  public int getKind() {
    return FIELD_ENTITY;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CAstType getType() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public Collection<CAstQualifier> getQualifiers() {
    return modifiers;
  }
}
