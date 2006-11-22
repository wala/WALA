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

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.collections.Filter;

/**
 *
 * This silly set includes all nodes in the universe that represent
 * non-native, non-synthetic methods.
 * 
 * @author sfink
 */
public class NormalCGNodeFilter implements Filter {

  private final static NormalCGNodeFilter singleton = new NormalCGNodeFilter();

  /**
   * Class is a singleton
   */
  private NormalCGNodeFilter() {
  }

  /**
   * @see java.util.Collection#contains(Object)
   */
  public boolean accepts(Object o) {
    if (o instanceof CGNode) {
      CGNode n = (CGNode) o;
      IMethod m = n.getMethod();
      return ((!m.isSynthetic()) && (!m.isNative()));
    } else {
      return false;
    }
  }

  public static final Filter instance() {
    return singleton;
  }
}
