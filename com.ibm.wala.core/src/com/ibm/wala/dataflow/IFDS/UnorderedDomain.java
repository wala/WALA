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

import com.ibm.wala.util.intset.MutableMapping;

/**
 * @author sfink
 *
 */
public class UnorderedDomain<T> extends MutableMapping<T> implements TabulationDomain<T> {

  /* 
   * @see com.ibm.wala.dataflow.IFDS.TabulationDomain#isWeakerThan(int, int)
   */
  public boolean isWeakerThan(int d1, int d2) {
    return false;
  }

}
