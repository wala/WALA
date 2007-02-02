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

import com.ibm.wala.cast.tree.*;

import java.util.*;

public class CAstTypeDictionaryImpl implements CAstTypeDictionary {
  private final Map/*<ASTType,CAstType>*/ fMap= new HashMap();

  public CAstType getCAstTypeFor(Object/*ASTType*/ astType) {
    return (CAstType) fMap.get(astType);
  }

  public void map(Object/*ASTType*/ astType, CAstType castType) {
    fMap.put(astType, castType);
  }

  public CAstReference resolveReference(CAstReference ref) {
    return ref;
  }
}
