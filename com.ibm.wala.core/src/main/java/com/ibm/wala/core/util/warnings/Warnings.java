/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.util.warnings;

import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

/** A global, static dictionary of warnings */
public class Warnings {

  private static final Collection<Warning> warnings = HashSetFactory.make();

  public static synchronized boolean add(Warning w) {
    return warnings.add(w);
  }

  public static synchronized void clear() {
    warnings.clear();
  }

  public static synchronized String asString() {
    TreeSet<Warning> T = new TreeSet<>(warnings);
    Iterator<Warning> it = T.iterator();
    StringBuilder result = new StringBuilder();
    for (int i = 1; i <= T.size(); i++) {
      result.append(i).append(". ");
      result.append(it.next());
      result.append('\n');
    }
    return result.toString();
  }

  public static synchronized Iterator<Warning> iterator() {
    return warnings.iterator();
  }
}
