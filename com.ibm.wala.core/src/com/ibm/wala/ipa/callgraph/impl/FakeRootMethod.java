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
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * A synthetic method that models the fake root node.
 */
public class FakeRootMethod extends AbstractRootMethod {

  public static final Atom name = Atom.findOrCreateAsciiAtom("fakeRootMethod");

  public static final Descriptor descr = Descriptor.findOrCreate(new TypeName[0], TypeReference.VoidName);

  public static final MethodReference rootMethod = MethodReference.findOrCreate(FakeRootClass.FAKE_ROOT_CLASS, name, descr);

  public FakeRootMethod(final IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
    super(rootMethod, cha, options, cache);
  }

  /**
   * @return true iff m is the fake root method.
   * @throws IllegalArgumentException if m is null
   */
  public static boolean isFakeRootMethod(MemberReference m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    return m.equals(rootMethod);
  }

  /**
   * @return true iff block is a basic block in the fake root method
   * @throws IllegalArgumentException if block is null
   */
  public static boolean isFromFakeRoot(IBasicBlock block) {
    if (block == null) {
      throw new IllegalArgumentException("block is null");
    }
    IMethod m = block.getMethod();
    return FakeRootMethod.isFakeRootMethod(m.getReference());
  }

  public static MethodReference getRootMethod() {
    return rootMethod;
  }

}
