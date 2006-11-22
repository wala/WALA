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
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * A context interpreter that first checks with A, then defaults to B.
 * 
 * @author sfink
 */
public class DelegatingRTAContextInterpreter implements RTAContextInterpreter {

  private final RTAContextInterpreter A;

  private final RTAContextInterpreter B;

  public DelegatingRTAContextInterpreter(RTAContextInterpreter A, RTAContextInterpreter B) {
    this.A = A;
    this.B = B;
    if (Assertions.verifyAssertions) {
      Assertions._assert(B != null, "B is null");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#understands(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context)
   */
  public boolean understands(CGNode node) {
    if (A != null) {
      return A.understands(node) || B.understands(node);
    } else {
      return B.understands(node);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getAllocatedTypes(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<NewSiteReference> iterateNewSites(CGNode node, WarningSet warnings) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateNewSites(node, warnings);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.iterateNewSites(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getCallSites(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<CallSiteReference> iterateCallSites(CGNode node, WarningSet warnings) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateCallSites(node, warnings);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.iterateCallSites(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsRead(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator iterateFieldsRead(CGNode node, WarningSet warnings) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateFieldsRead(node, warnings);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.iterateFieldsRead(node, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsWritten(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator iterateFieldsWritten(CGNode node, WarningSet warnings) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateFieldsWritten(node, warnings);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.iterateFieldsWritten(node, warnings);
  }

  public boolean recordFactoryType(CGNode node, IClass klass) {
    boolean result = false;
    if (A != null) {
      result |= A.recordFactoryType(node, klass);
    }
    result |= B.recordFactoryType(node, klass);
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    if (A != null) {
      A.setWarnings(newWarnings);
    }
    B.setWarnings(newWarnings);
  }

  public String toString() {
    return getClass().getName() + ": " + A + ", " + B;
  }
}
