/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import java.io.IOException;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.intset.BimodalMutableIntSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSetFactory;
import com.ibm.wala.util.intset.DebuggingMutableIntSetFactory;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSetFactory;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSetFactory;
import com.ibm.wala.util.intset.MutableSparseIntSetFactory;
import com.ibm.wala.util.intset.SemiSparseMutableIntSetFactory;

/**
 * 
 * Run the call graph only test with paranoid debugging bit vectors
 * 
 * @author Julian Dolby
 */
public class DebuggingBitsetCallGraphTest extends WalaTestCase {

  public static void main(String[] args) {
    justThisTest(DebuggingBitsetCallGraphTest.class);
  }

  private final CallGraphTest graphTest;

  public DebuggingBitsetCallGraphTest(String arg0) {
    super(arg0);
    graphTest = new CallGraphTest(arg0);
  }

  private void runBitsetTest(MutableIntSetFactory p, MutableIntSetFactory s) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    MutableIntSetFactory save = IntSetUtil.getDefaultIntSetFactory();
    try {
      IntSetUtil.setDefaultIntSetFactory(new DebuggingMutableIntSetFactory(p, s));
      graphTest.testJLex();
    } finally {
      IntSetUtil.setDefaultIntSetFactory(save);
    }
  }

  public void testBimodalSparse() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    runBitsetTest(new BimodalMutableIntSetFactory(), new MutableSparseIntSetFactory());
  }

  public void testSharedBimodal() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    runBitsetTest(new MutableSharedBitVectorIntSetFactory(), new BimodalMutableIntSetFactory());
  }

  public void testSharedSparse() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    runBitsetTest(new MutableSharedBitVectorIntSetFactory(), new MutableSparseIntSetFactory());
  }

  public void testSharedBitVector() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    runBitsetTest(new MutableSharedBitVectorIntSetFactory(), new BitVectorIntSetFactory());
  }

  public void testSemiSparseShared() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    runBitsetTest(new SemiSparseMutableIntSetFactory(), new MutableSharedBitVectorIntSetFactory());
  }

}
