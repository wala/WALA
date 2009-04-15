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
    this.modifiers = new HashSet<CAstQualifier>();
    if (modifiers != null) {
      this.modifiers.addAll(modifiers);
    }
   }

  public String toString() {
    if (type == null) {
      return "global " + name;
    } else {
      return "global " + name + ":" + type;
    }
  }

  public int getKind() {
    return GLOBAL_ENTITY;
  }

  public String getName() {
    return name;
  }

  public CAstType getType() {
    return type;
  }

  public Collection<CAstQualifier> getQualifiers() {
    return modifiers;
  }
}
