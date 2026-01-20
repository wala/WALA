/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.jspecify.annotations.NonNull;

public abstract class AbstractEntity implements CAstEntity {
  private Position sourcePosition;

  private final Map<CAstNode, @NonNull Collection<CAstEntity>> scopedEntities =
      HashMapFactory.make();

  @Override
  public Map<CAstNode, @NonNull Collection<CAstEntity>> getAllScopedEntities() {
    return scopedEntities;
  }

  @Override
  public String getSignature() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public Collection<CAstAnnotation> getAnnotations() {
    return null;
  }

  public void setPosition(Position pos) {
    sourcePosition = pos;
  }

  @Override
  public Position getPosition() {
    return sourcePosition;
  }

  @Override
  public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
    Collection<CAstEntity> cAstEntities = scopedEntities.get(construct);
    return cAstEntities == null ? EmptyIterator.instance() : cAstEntities.iterator();
  }

  public void addScopedEntity(CAstNode construct, CAstEntity child) {
    scopedEntities.computeIfAbsent(construct, absent -> HashSetFactory.make(1)).add(child);
  }
}
