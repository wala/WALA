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
import java.util.Collections;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * The union of two theories.
 * 
 * @author sjfink
 */
public class CombinedTheory extends AbstractTheory {

  private final ITheory a;

  private final ITheory b;

  private CombinedTheory(ITheory a, ITheory b) {
    this.a = a;
    this.b = b;
  }

  public static CombinedTheory make(ITheory a, ITheory b) throws IllegalArgumentException {
    if (a == null) {
      throw new IllegalArgumentException("a cannot be null");
    }
    if (b == null) {
      throw new IllegalArgumentException("b cannot be null");
    }
    return new CombinedTheory(a, b);
  }

  public Collection<IFormula> getSentences() {
    Set<IFormula> union = HashSetFactory.make();
    union.addAll(a.getSentences());
    union.addAll(b.getSentences());
    return union;
  }

  public IVocabulary getVocabulary() {
    return CombinedVocabulary.make(a.getVocabulary(), b.getVocabulary());
  }

  public static ITheory make(ITheory t, IVocabulary<?> v) {
    return make(t, new JustVocabulary(v));
  }

  private final static class JustVocabulary extends AbstractTheory {

    private final IVocabulary v;

    public JustVocabulary(IVocabulary v) {
      this.v = v;
    }

    public Collection<IFormula> getSentences() {
      return Collections.emptySet();
    }

    public IVocabulary getVocabulary() {
      return v;
    }

  }

}
