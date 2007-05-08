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

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * @author sfink
 */
public abstract class SetOfClasses {

  public abstract boolean contains(String klassName);

  public abstract boolean contains(TypeReference klass);

  /**
   * Iterate all classes in the given hierarchy that this set
   * contains.
   * @param hierarchy
   * @return Iterator of IClass
   * @throws IllegalArgumentException  if hierarchy is null
   */
  public Iterator<IClass> iterator(ClassHierarchy hierarchy) {
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

  /**
   * @paramklass
   */
  public abstract void add(IClass klass);

}
