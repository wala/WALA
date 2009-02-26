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

class AbstractFieldEntity extends AbstractDataEntity {
  private final String name;

  private final Set<CAstQualifier> modifiers;

  private final CAstEntity declaringClass;

  public AbstractFieldEntity(String name, Set<CAstQualifier> modifiers, boolean isStatic, CAstEntity declaringClass) {
    this.name = name;
    this.declaringClass = declaringClass;

    this.modifiers = new HashSet<CAstQualifier>();
    if (modifiers != null) {
      this.modifiers.addAll(modifiers);
    }
    if (isStatic) {
      this.modifiers.add(CAstQualifier.STATIC);
    }
  }

  public String toString() {
    return "field " + name + " of " + declaringClass.getName();
  }

  public int getKind() {
    return FIELD_ENTITY;
  }

  public String getName() {
    return name;
  }

  public CAstType getType() {
    Assertions.UNREACHABLE();
    return null;
  }

  public Collection<CAstQualifier> getQualifiers() {
    return modifiers;
  }
}