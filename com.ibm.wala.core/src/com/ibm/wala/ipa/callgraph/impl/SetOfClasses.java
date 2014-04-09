/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Logically, a set of {@link IClass}.
 * 
 * TODO: why does this not extend {@link Set}? Is there a good reason anymore?
 */
public abstract class SetOfClasses implements Serializable {

  public abstract boolean contains(String klassName);

  public abstract boolean contains(TypeReference klass);

  /**
   * Iterate all classes in the given hierarchy that this set contains.
   * 
   * @throws IllegalArgumentException if hierarchy is null
   */
  public Iterator<IClass> iterator(IClassHierarchy hierarchy) {
    if (hierarchy == null) {
      throw new IllegalArgumentException("hierarchy is null");
    }
    HashSet<IClass> result = HashSetFactory.make();
    for (IClass klass : hierarchy) {
      if (contains(klass.getReference())) {
        result.add(klass);
      }
    }
    return result.iterator();
  }

  public abstract void add(IClass klass);

}
