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
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Default implementation of MethodContextInterpreter for context-insensitive analysis
 */
public abstract class ContextInsensitiveRTAInterpreter implements RTAContextInterpreter, SSAContextInterpreter {

  private final AnalysisCache analysisCache;

  public ContextInsensitiveRTAInterpreter(AnalysisCache cache) {
    this.analysisCache = cache;
  }

  public AnalysisCache getAnalysisCache() {
    return analysisCache;
  }

  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.getNewSites(node.getMethod()).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.getCallSites(node.getMethod()).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.getFieldsRead(node.getMethod()).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.getFieldsWritten(node.getMethod()).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public boolean understands(CGNode node) {
    return true;
  }

  public boolean recordFactoryType(CGNode node, IClass klass) {
    // not a factory type
    return false;
  }
}
