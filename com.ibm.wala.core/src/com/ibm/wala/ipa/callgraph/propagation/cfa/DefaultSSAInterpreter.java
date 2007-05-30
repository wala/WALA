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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.DefaultRTAInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Basic analysis; context-insensitive except for newInstance and clone
 * 
 * @author sfink
 */
public class DefaultSSAInterpreter extends DefaultRTAInterpreter implements SSAContextInterpreter {

  private final CloneInterpreter cloneInterpreter;
  private final ContextInsensitiveSSAInterpreter defaultInterpreter;

  /**
   * @param options
   * @param cha
   * @param warnings
   */
  public DefaultSSAInterpreter(AnalysisOptions options, ClassHierarchy cha,WarningSet warnings) {
    super(options, cha, warnings);
    cloneInterpreter = new CloneInterpreter();
    defaultInterpreter = new ContextInsensitiveSSAInterpreter(options, cha);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.callgraph.MethodContextInterpreterProvider#getNodeInterpreter(com.ibm.detox.ipa.callgraph.CGNode)
   */
  private SSAContextInterpreter getCFAInterpreter(CGNode node) {
    if (cloneInterpreter.understands(node)) {
      return cloneInterpreter;
    } else {
      return defaultInterpreter;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#getIR(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public IR getIR(CGNode node, WarningSet warnings) {
    return getCFAInterpreter(node).getIR(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#getNumberOfStatements(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public int getNumberOfStatements(CGNode node, WarningSet warnings) {
    return getCFAInterpreter(node).getNumberOfStatements(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#understands(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context)
   */
  public boolean understands(IMethod method, Context context) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getAllocatedTypes(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node, WarningSet warnings) {
    return getCFAInterpreter(node).iterateNewSites(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getCallSites(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node, WarningSet warnings) {
    return getCFAInterpreter(node).iterateCallSites(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.IClass)
   */
  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    // do nothing; we don't understand factory methods.
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.cfg.CFGProvider#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public ControlFlowGraph getCFG(CGNode N, WarningSet warnings) {
    return getCFAInterpreter(N).getCFG(N, warnings);
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getDU(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.util.warnings.WarningSet)
   */
  public DefUse getDU(CGNode node, WarningSet warnings) {
    return getCFAInterpreter(node).getDU(node, warnings);
  }
  
}
