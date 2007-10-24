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

import com.ibm.wala.util.intset.IntPair;

/**
 * A simple class to define a simple vocabulary of functions
 * and relations
 * 
 * @author sjfink
 *
 */
public class BasicVocabulary<T extends IConstant> extends AbstractVocabulary<T> {

  private final Collection<? extends IFunction> functions;
  private final Collection<? extends IRelation> relations;
  
  protected BasicVocabulary(final Collection<? extends IFunction> functions, final Collection<? extends IRelation> relations) {
    super();
    this.functions = functions;
    this.relations = relations;
  }

  public IntPair getDomain() {
    return AbstractVocabulary.emptyDomain();
  }

  public Collection<? extends IFunction> getFunctions() {
    return Collections.unmodifiableCollection(functions);
  }

  public Collection<? extends IRelation> getRelations() {
    return Collections.unmodifiableCollection(relations);
  }

  public static <T extends IConstant> BasicVocabulary<T> make(IFunction f) {
    Collection<IRelation> empty = Collections.emptySet();
    return new BasicVocabulary<T>(Collections.singleton(f), empty);
  }
  
  public static <T extends IConstant> BasicVocabulary<T> make(Collection<IFunction> f) {
    Collection<IRelation> empty = Collections.emptySet();
    return new BasicVocabulary<T>(f, empty);
  }
  
  public static <T extends IConstant> BasicVocabulary<T> make(Collection<? extends IFunction> f, Collection<IRelation> r) {
    return new BasicVocabulary<T>(f, r);
  }

  public Collection<T> getConstants() {
    return Collections.emptySet();
  }

}
