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

import com.ibm.wala.util.intset.IntPair;

public abstract class AbstractVocabulary<T extends IConstant> implements IVocabulary<T> {

  private static final IntPair EMPTY = IntPair.make(-1, -1);

  public static IntPair emptyDomain() {
    return EMPTY;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("Domain: " + getDomain() + "\n");
    result.append(getConstants());
    result.append("Functions:\n");
    if (getFunctions().isEmpty()) {
      result.append(" <none> ");
    } else {
      for (IFunction f : getFunctions()) {
        result.append(f).append("\n");
      }
    }
    result.append("Relations:\n");
    if (getRelations().isEmpty()) {
      result.append(" <none> ");
    } else {
      for (IRelation r : getRelations()) {
        result.append(r).append("\n");
      }
    }
    return result.toString();
  }

}
