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
package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;

/**
 * This is intended as an internal interface; clients probably shouldn't be using this directly.
 *
 * <p>If you have a call graph in hand, to get the {@link IR} for a {@link CGNode}, use
 * node.getIR();
 *
 * <p>Otherwise, look at {@link SSACache}.
 */
public interface IRFactory<T extends IMethod> {

  /** Build an SSA {@link IR} for a method in a particular context */
  IR makeIR(T method, Context c, SSAOptions options);

  /** Does this factory always return the same IR for a method, regardless of context? */
  boolean contextIsIrrelevant(T method);
}
