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

import java.util.*;

public class EmptyTheory extends AbstractTheory {

  private final static EmptyTheory INSTANCE = new EmptyTheory();
  
  public static EmptyTheory singleton() {
    return INSTANCE;
  }

  private EmptyTheory() {}
  
  public Collection<? extends IFormula> getSentences() {
    return Collections.emptySet();
  }

  public IVocabulary getVocabulary() {
    return BasicVocabulary.make(Collections.<IFunction>emptySet());
  }

}
