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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntRange {
  private int beg, end;

  public IntRange(int beg, int end) {
    this.beg = beg;
    this.end = end;
  }

  public IntRange(int beg) {
    this(beg, beg + 1);
  }

  public Iterator iteratorFor(Object ary[]) {
    List l = new ArrayList();
    int max = end;
    if (max < 0) {
      max = ary.length;
    }
    for (int i = beg; i < max; i++) {
      l.add(ary[i]);
    }
    return l.iterator();
  }

  public Iterator iteratorFor(List list) {
    List l = new ArrayList();
    int max = end;
    if (max < 0) {
      max = list.size();
    }
    for (int i = beg; i < max; i++) {
      l.add(list.get(i));
    }
    return l.iterator();
  }
}