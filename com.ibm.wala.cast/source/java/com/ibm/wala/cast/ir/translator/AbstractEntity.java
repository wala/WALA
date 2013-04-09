/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

public abstract class AbstractEntity implements CAstEntity {
  private Position sourcePosition;

  private final Map<CAstNode, Collection<CAstEntity>> scopedEntities = HashMapFactory.make();

  public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
    return scopedEntities;
  }

  public String getSignature() {
    Assertions.UNREACHABLE();
    return null;
  }

  
  public Collection<CAstAnnotation> getAnnotations() {
    return null;
  }

  public void setPosition(Position pos) {
    sourcePosition = pos;
  }

  public Position getPosition() {
    return sourcePosition;
  }

  public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
    if (scopedEntities.containsKey(construct)) {
      return scopedEntities.get(construct).iterator();
    } else {
      return EmptyIterator.instance();
    }
  }

  public void addScopedEntity(CAstNode construct, CAstEntity child) {
    if (!scopedEntities.containsKey(construct)) {
      Collection<CAstEntity> set = HashSetFactory.make(1);
      scopedEntities.put(construct, set);
    }
    scopedEntities.get(construct).add(child);
  }
}