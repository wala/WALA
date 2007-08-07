/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class CombinedVocabulary extends AbstractVocabulary<Object> {

  private final IVocabulary a;
  private final IVocabulary b;
  
  private CombinedVocabulary(IVocabulary a, IVocabulary b) {
    this.a = a;
    this.b = b;
    if (b.getRelations() == null) {
      throw new IllegalArgumentException("b relations are null " + b.getClass());
    }
  }
  
  public static CombinedVocabulary make(IVocabulary a, IVocabulary b) {
    return new CombinedVocabulary(a,b);
  }
  

  @SuppressWarnings("unchecked")
  public Collection<? extends IFunction> getFunctions() {
    Collection<? extends IFunction> s = HashSetFactory.make();
    s.addAll(a.getFunctions());
    s.addAll(b.getFunctions());
    return s;
  }

  @SuppressWarnings("unchecked")
  public Collection<? extends IRelation> getRelations() {
    Collection<? extends IRelation> s = HashSetFactory.make();
    s.addAll(a.getRelations());
    s.addAll(b.getRelations());
    return s;
  }

  public IntPair getDomain() {
    IntPair ad = a.getDomain();
    IntPair bd = b.getDomain();
    if (ad.equals(AbstractVocabulary.emptyDomain())) {
      return bd;
    }
    if (bd.equals(AbstractVocabulary.emptyDomain())) {
      return ad;
    }
    
    if ((ad.getY() + 1) != bd.getX()) {
      // TODO: fix this.
      Assertions.UNREACHABLE(ad + " " + bd);
    }
    return IntPair.make(ad.getX(), bd.getY());
  }

  @SuppressWarnings("unchecked")
  public OrdinalSetMapping<Object> getConstants() {
    OrdinalSetMapping<Object> ma = a.getConstants();
    OrdinalSetMapping<Object> mb = b.getConstants();
    assert ma != null;
    assert mb != null;
    MutableMapping<Object> result = new MutableMapping<Object>();
    for (Object o : ma) {
      int i = ma.getMappedIndex(o);
      result.put(i,o);
    }
    for (Object o : mb) {
      int i = mb.getMappedIndex(o);
      result.put(i, o);
    }
    return result;
  }
}
