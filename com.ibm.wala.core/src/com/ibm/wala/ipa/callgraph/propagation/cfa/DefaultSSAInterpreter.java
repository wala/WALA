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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import java.util.Iterator;

import com.ibm.wala.analysis.reflection.CloneInterpreter;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.DefaultRTAInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * Basic analysis; context-insensitive except for newInstance and clone
 */
public class DefaultSSAInterpreter extends DefaultRTAInterpreter implements SSAContextInterpreter {

  private final CloneInterpreter cloneInterpreter;

  private final ContextInsensitiveSSAInterpreter defaultInterpreter;

  public DefaultSSAInterpreter(AnalysisOptions options, IAnalysisCacheView cache) {
    super(options, cache);
    cloneInterpreter = new CloneInterpreter();
    defaultInterpreter = new ContextInsensitiveSSAInterpreter(options, cache);
  }

  private SSAContextInterpreter getCFAInterpreter(CGNode node) {
    if (cloneInterpreter.understands(node)) {
      return cloneInterpreter;
    } else {
      return defaultInterpreter;
    }

  }

  @Override
  public IR getIR(CGNode node) {
    return getCFAInterpreter(node).getIR(node);
  }

  
  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  @Override
  public int getNumberOfStatements(CGNode node) {
    return getCFAInterpreter(node).getNumberOfStatements(node);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    return getCFAInterpreter(node).iterateNewSites(node);
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    return getCFAInterpreter(node).iterateCallSites(node);
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    // do nothing; we don't understand factory methods.
    return false;
  }

  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getCFAInterpreter(N).getCFG(N);
  }

  @Override
  public DefUse getDU(CGNode node) {
    return getCFAInterpreter(node).getDU(node);
  }

}
