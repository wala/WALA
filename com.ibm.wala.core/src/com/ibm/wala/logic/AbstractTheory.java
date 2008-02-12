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
import java.util.TreeSet;

public abstract class AbstractTheory extends DefaultDecorator implements ITheory {

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(getClass().toString());
    result.append(":\n");
    result.append(getVocabulary());
    result.append("Sentences:\n");
    TreeSet<String> sorted = new TreeSet<String>();
    for (IFormula f : getSentences()) {
      sorted.add(f.toString());
    }
    for (String s : sorted) {
      result.append(s);
      result.append("\n");
    }
    return result.toString();
  }

  /**
   * by default, just return all sentences
   */
  public Collection<? extends IFormula> getSentencesRelevantToConstraints(Collection<? extends IFormula> constraints) {
    return getSentences();
  }

  
}
