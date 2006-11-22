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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * @author sfink
 *
 */
public interface TabulationDomain extends OrdinalSetMapping<Object> {

  /**
   * if this domain supports a partial order on facts, return true
   * if d1 is weaker than d2 (intutitively d1 meet d2 = d1)
   * 
   * return false otherwise
   */
  boolean isWeakerThan(int d1, int d2);



}
