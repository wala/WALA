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

/**
 * A simple class to define a simple vocabulary of functions and relations
 * 
 * @author sjfink
 * 
 */
public class BasicVocabulary<T extends IConstant> extends AbstractVocabulary<T> {

  private final Collection<? extends IFunction> functions;

  private final Collection<? extends IRelation> relations;
  
  private final Collection<AbstractVariable> variables;

  protected BasicVocabulary(final Collection<? extends IFunction> functions, final Collection<? extends IRelation> relations,
      final Collection<AbstractVariable> variables) {
    super();
    this.functions = functions;
    this.relations = relations;
    this.variables = variables;
  }

  public Collection<? extends IFunction> getFunctions() {
    return Collections.unmodifiableCollection(functions);
  }

  public Collection<? extends IRelation> getRelations() {
    return Collections.unmodifiableCollection(relations);
  }
  
  public Collection<AbstractVariable> getVariables() {
    return Collections.unmodifiableCollection(variables);
  }

  public static <T extends IConstant> BasicVocabulary<T> make(IFunction f) {
    Collection<IRelation> empty = Collections.emptySet();
    Collection<AbstractVariable> emptyV = Collections.emptySet();
    return new BasicVocabulary<T>(Collections.singleton(f), empty, emptyV);
  }

  public static <T extends IConstant> BasicVocabulary<T> make(Collection<IFunction> f) {
    Collection<IRelation> empty = Collections.emptySet();
    Collection<AbstractVariable> emptyV = Collections.emptySet();
    return new BasicVocabulary<T>(f, empty, emptyV);
  }

  public static <T extends IConstant> BasicVocabulary<T> make(Collection<? extends IFunction> f, Collection<IRelation> r) {
    Collection<AbstractVariable> emptyV = Collections.emptySet();
    return new BasicVocabulary<T>(f, r, emptyV);
  }

  public Collection<T> getConstants() {
    return Collections.emptySet();
  }

}
