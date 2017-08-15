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
 * Created on Aug 31, 2005
 */
package com.ibm.wala.cast.tree;

import java.util.Iterator;

public interface CAstTypeDictionary/*<ASTType>*/ extends Iterable<CAstType> {

  CAstType getCAstTypeFor(Object/*ASTType*/ type);

  CAstReference resolveReference(CAstReference ref);

  @Override
  Iterator<CAstType> iterator();

}
