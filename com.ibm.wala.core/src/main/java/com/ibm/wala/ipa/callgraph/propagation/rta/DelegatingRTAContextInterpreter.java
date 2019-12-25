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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.FieldReference;
import java.util.Iterator;

/** A context interpreter that first checks with A, then defaults to B. */
public class DelegatingRTAContextInterpreter implements RTAContextInterpreter {

  private final RTAContextInterpreter A;

  private final RTAContextInterpreter B;

  public DelegatingRTAContextInterpreter(RTAContextInterpreter A, RTAContextInterpreter B) {
    this.A = A;
    this.B = B;
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
  }

  @Override
  public boolean understands(CGNode node) {
    if (A != null) {
      return A.understands(node) || B.understands(node);
    } else {
      return B.understands(node);
    }
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateNewSites(node);
      }
    }
    assert B.understands(node);
    return B.iterateNewSites(node);
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateCallSites(node);
      }
    }
    assert B.understands(node);
    return B.iterateCallSites(node);
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateFieldsRead(node);
      }
    }
    assert B.understands(node);
    return B.iterateFieldsRead(node);
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.iterateFieldsWritten(node);
      }
    }
    assert B.understands(node);
    return B.iterateFieldsWritten(node);
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    boolean result = false;
    if (A != null) {
      result |= A.recordFactoryType(node, klass);
    }
    result |= B.recordFactoryType(node, klass);
    return result;
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + A + ", " + B;
  }
}
