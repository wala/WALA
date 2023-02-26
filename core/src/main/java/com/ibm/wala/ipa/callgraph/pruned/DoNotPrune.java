/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ipa.callgraph.pruned;

import com.ibm.wala.ipa.callgraph.CGNode;

public class DoNotPrune implements PruningPolicy {

  public static DoNotPrune INSTANCE = new DoNotPrune();

  @Override
  public boolean check(CGNode n) {
    return true;
  }
}
