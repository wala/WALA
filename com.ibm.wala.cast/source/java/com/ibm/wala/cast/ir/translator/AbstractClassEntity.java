/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import java.util.Collection;

import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;

public class AbstractClassEntity extends AbstractDataEntity {
  private final CAstType.Class type;

  public AbstractClassEntity(CAstType.Class type) {
    this.type = type;
  }

  public String toString() {
    return "class " + type.getName();
  }

  public int getKind() {
    return TYPE_ENTITY;
  }

  public String getName() {
    return type.getName();
  }

  public CAstType getType() {
    return type;
  }

  public Collection<CAstQualifier> getQualifiers() {
    return type.getQualifiers();
  }
}