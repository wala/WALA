/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;

/**
 * An exception to throw when call graph construction is canceled. This exception allows clients to
 * retrieve the partially-built call graph and pointer analysis
 */
public class CallGraphBuilderCancelException extends CancelException {

  private static final long serialVersionUID = -3071193971009314659L;

  private final CallGraph cg;

  private final PointerAnalysis<InstanceKey> pointerAnalysis;

  public static CallGraphBuilderCancelException createCallGraphBuilderCancelException(
      Exception cause, CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis) {
    return new CallGraphBuilderCancelException(cause, cg, pointerAnalysis);
  }

  public static CallGraphBuilderCancelException createCallGraphBuilderCancelException(
      String msg, CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis) {
    return new CallGraphBuilderCancelException(msg, cg, pointerAnalysis);
  }

  /** @return the {@link CallGraph} in whatever state it was left when computation was canceled */
  public CallGraph getPartialCallGraph() {
    return cg;
  }

  /**
   * @return the {@link PointerAnalysis} in whatever state it was left when computation was canceled
   */
  public PointerAnalysis<InstanceKey> getPartialPointerAnalysis() {
    return pointerAnalysis;
  }

  private CallGraphBuilderCancelException(
      String msg, CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis) {
    super(msg);
    this.cg = cg;
    this.pointerAnalysis = pointerAnalysis;
  }

  private CallGraphBuilderCancelException(
      Exception cause, CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis) {
    super(cause);
    this.cg = cg;
    this.pointerAnalysis = pointerAnalysis;
  }
}
