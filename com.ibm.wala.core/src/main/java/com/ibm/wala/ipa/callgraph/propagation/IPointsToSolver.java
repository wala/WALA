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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/** Basic interface for a pointer analysis solver. */
public interface IPointsToSolver {

  public void solve(IProgressMonitor monitor) throws IllegalArgumentException, CancelException;
}
