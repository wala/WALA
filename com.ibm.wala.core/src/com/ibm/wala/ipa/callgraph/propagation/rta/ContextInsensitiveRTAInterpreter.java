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
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * Default implementation of MethodContextInterpreter for context-insensitive
 * analysis
 * 
 * @author sfink
 */
public abstract class ContextInsensitiveRTAInterpreter implements RTAContextInterpreter, SSAContextInterpreter {

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.MethodContextInterpreter#getAllocationStatements(com.ibm.wala.classLoader.IMethod, com.ibm.detox.ipa.callgraph.Context)
   */
  public Iterator<NewSiteReference> iterateNewSites(CGNode node, WarningSet warnings) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.iterateNewSites(node.getMethod(), warnings);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getCallSites(com.ibm.wala.classLoader.IMethod, com.ibm.detox.ipa.callgraph.Context, com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<CallSiteReference> iterateCallSites(CGNode node, WarningSet warnings) {
    try {
      return CodeScanner.iterateCallSites(node.getMethod(), warnings);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }
  

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsRead(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator iterateFieldsRead(CGNode node, WarningSet warnings) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.iterateFieldsRead(node.getMethod(), warnings);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsWritten(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator iterateFieldsWritten(CGNode node, WarningSet warnings) {
    try {
      return CodeScanner.iterateFieldsWritten(node.getMethod(), warnings);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }


  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#understands(com.ibm.wala.classLoader.IMethod, com.ibm.detox.ipa.callgraph.Context)
   */
  public boolean understands(CGNode node) {
    return true;
  }
  public boolean recordFactoryType(CGNode node, IClass klass) {
    // not a factory type
    return false;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // this object is not bound to a WarningSet
  }
}
