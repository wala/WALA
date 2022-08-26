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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/** A synthetic method that models the fake root node. */
public class FakeRootMethod extends AbstractRootMethod {

  public static final Atom name = Atom.findOrCreateAsciiAtom("fakeRootMethod");

  public static final Descriptor descr =
      Descriptor.findOrCreate(new TypeName[0], TypeReference.VoidName);

  public FakeRootMethod(
      final IClass fakeRootClass, AnalysisOptions options, IAnalysisCacheView cache) {
    super(
        MethodReference.findOrCreate(fakeRootClass.getReference(), name, descr),
        fakeRootClass.getClassHierarchy(),
        options,
        cache);
  }

  /**
   * @return true iff m is the fake root method.
   * @throws IllegalArgumentException if m is null
   */
  public boolean isFakeRootMethod(MemberReference m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    return m.equals(getReference());
  }

  /**
   * @return true iff block is a basic block in the fake root method
   * @throws IllegalArgumentException if block is null
   */
  public static boolean isFromFakeRoot(IBasicBlock<?> block) {
    if (block == null) {
      throw new IllegalArgumentException("block is null");
    }
    IMethod m = block.getMethod();
    return m instanceof FakeRootMethod && ((FakeRootMethod) m).isFakeRootMethod(m.getReference());
  }
}
