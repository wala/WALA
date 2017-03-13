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

import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;

public class AbstractGlobalEntity extends AbstractDataEntity {
  private final String name;

  private final Set<CAstQualifier> modifiers;

  private final CAstType type;
  
  public AbstractGlobalEntity(String name, CAstType type, Set<CAstQualifier> modifiers) {
    this.name = name;
    this.type = type;
    this.modifiers = new HashSet<>();
    if (modifiers != null) {
      this.modifiers.addAll(modifiers);
    }
   }

  @Override
  public String toString() {
    if (type == null) {
      return "global " + name;
    } else {
      return "global " + name + ":" + type;
    }
  }

  @Override
  public int getKind() {
    return GLOBAL_ENTITY;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CAstType getType() {
    return type;
  }

  @Override
  public Collection<CAstQualifier> getQualifiers() {
    return modifiers;
  }
}
