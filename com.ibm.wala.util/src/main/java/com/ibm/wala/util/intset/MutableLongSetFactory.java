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
package com.ibm.wala.util.intset;

/** An object that creates some flavor of mutable int set. */
public interface MutableLongSetFactory {

  public MutableLongSet make(long[] set);

  public MutableLongSet parse(String string);

  public MutableLongSet makeCopy(LongSet x);

  public MutableLongSet make();
}
