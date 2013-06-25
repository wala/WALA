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

  @Override
  public String toString() {
    return "class " + type.getName();
  }

  @Override
  public int getKind() {
    return TYPE_ENTITY;
  }

  @Override
  public String getName() {
    return type.getName();
  }

  @Override
  public CAstType getType() {
    return type;
  }

  @Override
  public Collection<CAstQualifier> getQualifiers() {
    return type.getQualifiers();
  }
}