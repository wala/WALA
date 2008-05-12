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

import java.util.Random;

/**
 * a simple pseudo-random number generator.
 * 
 * This is too expensive to use in inner loops.  Deprecating.
 * 
 * @author sfink
 */
@Deprecated
public class DeterministicHashCode {

  final private static Random r = new Random(1000);

  public static synchronized int get() {
    return r.nextInt();
  }
}
