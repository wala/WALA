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

package com.ibm.wala.ipa.callgraph.propagation.rta;

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContextInsensitiveSSAInterpreter;
import com.ibm.wala.types.FieldReference;

/**
 * Basic analysis; context-insensitive
 */
public class DefaultRTAInterpreter implements RTAContextInterpreter {

  private static final boolean DEBUG = false;

  private final ContextInsensitiveRTAInterpreter defaultInterpreter;

  /**
   * @param options governing analysis options
   */
  public DefaultRTAInterpreter(AnalysisOptions options, IAnalysisCacheView cache) {
    defaultInterpreter = new ContextInsensitiveSSAInterpreter(options, cache);
  }

  private RTAContextInterpreter getNodeInterpreter(CGNode node) {

    if (node.getMethod() instanceof FakeRootMethod) {
      FakeRootMethod f = (FakeRootMethod) node.getMethod();
      return f.getInterpreter();
    } else {
      if (DEBUG) {
        System.err.println(("providing context insensitive interpreter for node " + node));
      }
      return defaultInterpreter;
    }
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return getNodeInterpreter(node).iterateNewSites(node);
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return getNodeInterpreter(node).iterateCallSites(node);
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return getNodeInterpreter(node).iterateFieldsRead(node);
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return getNodeInterpreter(node).iterateFieldsWritten(node);
  }

  @Override
  public boolean understands(CGNode node) {
    return true;
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    // not a factory type
    return false;
  }
}
