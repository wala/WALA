/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Sep 21, 2005
 */
package com.ibm.wala.cast.tree.impl;

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.tree.CAstReference;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstTypeDictionary;
import com.ibm.wala.util.collections.HashMapFactory;

public class CAstTypeDictionaryImpl<A> implements CAstTypeDictionary {
  protected final Map<A, CAstType> fMap = HashMapFactory.make();

  @Override
  public CAstType getCAstTypeFor(Object astType) {
      return fMap.get(astType);
  }

  public void map(A astType, CAstType castType) {
    fMap.put(astType, castType);
  }

  @Override
  public Iterator<CAstType> iterator() {
    return fMap.values().iterator();
  }

  @Override
  public CAstReference resolveReference(CAstReference ref) {
    return ref;
  }
}
