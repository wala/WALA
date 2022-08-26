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

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeIRFactory;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.summaries.SyntheticIRFactory;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.util.debug.Assertions;

/**
 * Default implementation of {@link IRFactory}.
 *
 * <p>This creates {@link IR} objects from Shrike methods, and directly from synthetic methods.
 */
public class DefaultIRFactory implements IRFactory<IMethod> {

  private final ShrikeIRFactory shrikeFactory = new ShrikeIRFactory();

  private final SyntheticIRFactory syntheticFactory = new SyntheticIRFactory();

  public ControlFlowGraph<?, ?> makeCFG(IMethod method, @SuppressWarnings("unused") Context c)
      throws IllegalArgumentException {
    if (method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if (method.isWalaSynthetic()) {
      return syntheticFactory.makeCFG((SyntheticMethod) method);
    } else if (method instanceof IBytecodeMethod) {
      @SuppressWarnings("unchecked")
      final IBytecodeMethod<IInstruction> castMethod = (IBytecodeMethod<IInstruction>) method;
      return shrikeFactory.makeCFG(castMethod);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public IR makeIR(IMethod method, Context c, SSAOptions options) throws IllegalArgumentException {
    if (method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if (method.isWalaSynthetic()) {
      return syntheticFactory.makeIR((SyntheticMethod) method, c, options);
    } else if (method instanceof IBytecodeMethod) {
      @SuppressWarnings("unchecked")
      final IBytecodeMethod<IInstruction> castMethod = (IBytecodeMethod<IInstruction>) method;
      return shrikeFactory.makeIR(castMethod, c, options);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * Is the {@link Context} irrelevant as to structure of the {@link IR} for a particular {@link
   * IMethod}?
   */
  @Override
  public boolean contextIsIrrelevant(IMethod method) {
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    if (method.isWalaSynthetic()) {
      return syntheticFactory.contextIsIrrelevant((SyntheticMethod) method);
    } else if (method instanceof ShrikeCTMethod) {
      // we know ShrikeFactory contextIsIrrelevant
      return true;
    } else {
      // be conservative .. don't know what's going on.
      return false;
    }
  }
}
