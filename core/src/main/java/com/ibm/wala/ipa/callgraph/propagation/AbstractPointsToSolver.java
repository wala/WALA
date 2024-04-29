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

/** Abstract base class for solver for pointer analysis. */
public abstract class AbstractPointsToSolver implements IPointsToSolver {

  protected static final boolean DEBUG = false;

  private final PropagationSystem system;

  private final PropagationCallGraphBuilder builder;

  private final ReflectionHandler reflectionHandler;

  public AbstractPointsToSolver(PropagationSystem system, PropagationCallGraphBuilder builder) {
    if (system == null) {
      throw new IllegalArgumentException("null system");
    }
    this.system = system;
    this.builder = builder;
    this.reflectionHandler = new ReflectionHandler(builder);
  }

  @Override
  public abstract void solve(IProgressMonitor monitor)
      throws IllegalArgumentException, CancelException;

  protected PropagationCallGraphBuilder getBuilder() {
    return builder;
  }

  protected ReflectionHandler getReflectionHandler() {
    return reflectionHandler;
  }

  protected PropagationSystem getSystem() {
    return system;
  }
}
