/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.cha;

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.debug.Assertions;

public class ContextInsensitiveCHAContextInterpreter implements CHAContextInterpreter {

  @Override
  public boolean understands(CGNode node) {
    return true;
  }

  @Override
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

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    try {
      return CodeScanner.getNewSites(node.getMethod()).iterator();
    } catch (InvalidClassFileException e) {
      throw new RuntimeException(e);
    }
  }

}
