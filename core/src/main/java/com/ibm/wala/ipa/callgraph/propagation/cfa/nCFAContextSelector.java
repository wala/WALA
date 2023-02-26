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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;

public class nCFAContextSelector extends CallStringContextSelector {
  private final int n;

  public nCFAContextSelector(int n, ContextSelector base) {
    super(base);
    this.n = n;
  }

  @Override
  protected int getLength(CGNode caller, CallSiteReference site, IMethod target) {
    return n;
  }
}
