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
package com.ibm.wala.util.collections;

/**
 *
 * intersection of two filters
 * 
 * @author sfink
 */
public class Filtersection implements Filter {
  
  final private Filter a;
  final private Filter b;
  
  public Filtersection(Filter a, Filter b) {
    this.a = a;
    this.b = b;
  }

  public boolean accepts(Object o) {
    return a.accepts(o) && b.accepts(o);
  }

}
