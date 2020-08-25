/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;

/**
 * A call vertex represents the possible callees of a function call or {@code new} expression.
 *
 * @author mschaefer
 */
public class CallVertex extends Vertex {
  // method containing the call
  private final FuncVertex func;

  // PC of the call site
  private final CallSiteReference site;

  // the call instruction itself
  private final JavaScriptInvoke invk;

  CallVertex(FuncVertex func, CallSiteReference site, JavaScriptInvoke invk) {
    this.func = func;
    this.site = site;
    this.invk = invk;
  }

  public FuncVertex getCaller() {
    return func;
  }

  public CallSiteReference getSite() {
    return site;
  }

  public JavaScriptInvoke getInstruction() {
    return invk;
  }

  /** Does this call vertex correspond to a {@code new} instruction? */
  public boolean isNew() {
    return site.getDeclaredTarget() == JavaScriptMethods.ctorReference;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitCalleeVertex(this);
  }

  @Override
  public String toString() {
    return "Callee(" + func + ", " + site + ')';
  }

  @Override
  public String toSourceLevelString(IAnalysisCacheView cache) {
    IClass concreteType = func.getConcreteType();
    AstMethod method = (AstMethod) concreteType.getMethod(AstMethodReference.fnSelector);
    return "Callee(" + method.getSourcePosition(site.getProgramCounter()).prettyPrint() + ")";
  }
}
