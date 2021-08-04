/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class DelegatingEntity implements CAstEntity {
  private final CAstEntity base;

  public DelegatingEntity(CAstEntity base) {
    this.base = base;
  }

  @Override
  public CAstEntity getOriginal() {
    return base.getOriginal();
  }

  @Override
  public int getKind() {
    return base.getKind();
  }

  @Override
  public String getName() {
    return base.getName();
  }

  @Override
  public String getSignature() {
    return base.getSignature();
  }

  @Override
  public String[] getArgumentNames() {
    return base.getArgumentNames();
  }

  @Override
  public CAstNode[] getArgumentDefaults() {
    return base.getArgumentDefaults();
  }

  @Override
  public int getArgumentCount() {
    return base.getArgumentCount();
  }

  @Override
  public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
    return base.getAllScopedEntities();
  }

  @Override
  public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
    return base.getScopedEntities(construct);
  }

  @Override
  public CAstNode getAST() {
    return base.getAST();
  }

  @Override
  public CAstControlFlowMap getControlFlow() {
    return base.getControlFlow();
  }

  @Override
  public CAstSourcePositionMap getSourceMap() {
    return base.getSourceMap();
  }

  @Override
  public CAstSourcePositionMap.Position getPosition() {
    return base.getPosition();
  }

  @Override
  public CAstNodeTypeMap getNodeTypeMap() {
    return base.getNodeTypeMap();
  }

  @Override
  public Collection<CAstQualifier> getQualifiers() {
    return base.getQualifiers();
  }

  @Override
  public CAstType getType() {
    return base.getType();
  }

  @Override
  public Collection<CAstAnnotation> getAnnotations() {
    return base.getAnnotations();
  }

  @Override
  public Position getPosition(int arg) {
    return base.getPosition(arg);
  }

  @Override
  public Position getNamePosition() {
    return base.getNamePosition();
  }
}
