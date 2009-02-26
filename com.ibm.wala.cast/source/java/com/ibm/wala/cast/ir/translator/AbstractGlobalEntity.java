/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.util.debug.Assertions;

public class AbstractGlobalEntity extends AbstractDataEntity {
  private final String name;

  private final Set<CAstQualifier> modifiers;

  public AbstractGlobalEntity(String name, Set<CAstQualifier> modifiers) {
    this.name = name;
     this.modifiers = new HashSet<CAstQualifier>();
    if (modifiers != null) {
      this.modifiers.addAll(modifiers);
    }
   }

  public String toString() {
    return "global " + name;
  }

  public int getKind() {
    return GLOBAL_ENTITY;
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