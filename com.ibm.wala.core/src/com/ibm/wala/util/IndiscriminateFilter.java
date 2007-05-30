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

package com.ibm.wala.util;

import com.ibm.wala.util.collections.Filter;

/**
 * 
 * A filter that accepts everything.
 * 
 * @author sfink
 */
public class IndiscriminateFilter implements Filter {

  private final static IndiscriminateFilter INSTANCE = new IndiscriminateFilter();

  public static IndiscriminateFilter singleton() {
    return INSTANCE;
  }

  /*
   * @see com.ibm.wala.util.Filter#accepts(java.lang.Object)
   */
  public boolean accepts(Object o) {
    return true;
  }

}
