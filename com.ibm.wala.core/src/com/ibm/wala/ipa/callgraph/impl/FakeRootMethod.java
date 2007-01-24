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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.Atom;

/**
 * 
 * A synthetic method that models the fake root node.
 * 
 * @author sfink
 */
public class FakeRootMethod extends AbstractRootMethod {

  private static final Atom name = Atom.findOrCreateAsciiAtom("fakeRootMethod");

  private static final MethodReference rootMethod = MethodReference.findOrCreate(FakeRootClass.FAKE_ROOT_CLASS, name, Descriptor
      .findOrCreateUTF8("()V"));

  public FakeRootMethod(final ClassHierarchy cha, AnalysisOptions options) {
    super(rootMethod, cha, options);
  }

  /**
   * @param m
   *          a method reference
   * @return true iff m is the fake root method.
   */
  public static boolean isFakeRootMethod(MethodReference m) {
    return m.equals(rootMethod);
  }
  
  /**
   * @param block
   * @return true iff block is a basic block in the fake root method
   */
  public static boolean isFromFakeRoot(IBasicBlock block) {
    IMethod m = block.getMethod();
    return FakeRootMethod.isFakeRootMethod(m.getReference());
  }

  public static MethodReference getRootMethod() {
    return rootMethod;
  }

}
