/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
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

public class BasicTheory extends AbstractTheory {

  private final IVocabulary vocab;
  private final Collection<? extends IFormula> sentences;
  
  protected BasicTheory(IVocabulary vocab, Collection<? extends IFormula> sentences) {
    this.vocab = vocab;
    this.sentences = sentences;
  }
  
  public static BasicTheory make(IVocabulary vocab, Collection<? extends IFormula> sentences) {
    return new BasicTheory(vocab, sentences);
  }
  
  public Collection<? extends IFormula> getSentences() {
    return sentences;
  }

  public IVocabulary getVocabulary() {
    return vocab;
  }

}
