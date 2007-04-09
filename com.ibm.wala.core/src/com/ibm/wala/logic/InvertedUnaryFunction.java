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
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class InvertedUnaryFunction extends BinaryRelation {

  private final UnaryFunction f;
  
  protected InvertedUnaryFunction(String symbol, UnaryFunction f) {
    super(symbol);
    this.f = f;
  }
  
  public static InvertedUnaryFunction make(String symbol, UnaryFunction f) {
    return new InvertedUnaryFunction(symbol,f);
  }
  
  public ITheory asTheory(IVocabulary<?> v) {
    return new InvertedUnaryFunctionTheory(v);
    
  }
  
  private class InvertedUnaryFunctionTheory extends AbstractTheory {

    private final IVocabulary<?> originalVocabulary;
    
    private InvertedUnaryFunctionTheory(IVocabulary<?> originalVocabulary) {
      this.originalVocabulary = originalVocabulary;
    }
    
    public Collection<? extends IFormula> getSentences() {
      // f(i) == j  equivalent to  R(j,i)
      Variable i = Variable.make(0, originalVocabulary.getDomain());
      Variable j = Variable.make(1, originalVocabulary.getDomain());
      RelationFormula fiequalsj = RelationFormula.makeEquals(FunctionTerm.make(f, i), j);
      RelationFormula Rji = RelationFormula.make(InvertedUnaryFunction.this,j,i);
      IFormula result = QuantifiedFormula.forall(i, j, BinaryFormula.biconditional(fiequalsj,Rji));
      return Collections.singleton(result);
    }

    public IVocabulary getVocabulary() {
      return new IVocabulary<Object>() {
        public OrdinalSetMapping<Object> getConstants() {
          return new MutableMapping<Object>();
        }
        public IntPair getDomain() {
          return AbstractVocabulary.emptyDomain();
        }
        public Collection<? extends IFunction> getFunctions() {
          return Collections.emptySet();
        }
        public Collection<? extends IRelation> getRelations() {
          return Collections.singleton(InvertedUnaryFunction.this);
        }
        
      };
    }
    
  }

}
