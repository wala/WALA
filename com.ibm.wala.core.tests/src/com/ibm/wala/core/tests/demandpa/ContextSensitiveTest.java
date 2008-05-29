/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.core.tests.demandpa;

import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.demandpa.alg.ContextSensitiveStateMachine;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.IDemandPointerAnalysis;
import com.ibm.wala.demandpa.alg.refinepolicy.AlwaysRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.AlwaysRefineFieldsPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.driver.WalaUtil;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.strings.Atom;

public class ContextSensitiveTest extends AbstractPtrTest {

  @Override
  protected void setUp() {
    WalaUtil.initializeTraceFile();
  }

  public void testArraySet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAY_SET, 1);
  }

  public void testClone() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_CLONE, 1);
  }

  public void testFooId() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ID, 1);
  }

  public void testHashtableEnum() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // 3 because
    // can't tell between key, value, and entry enumerators in Hashtable
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_HASHTABLE_ENUM, 3);
  }

  // we know this one fails...
  // public void testOnTheFlyCS() throws ClassHierarchyException {
  // String mainClass = TestInfo.TEST_ONTHEFLY_CS;
  // final IDemandPointerAnalysis dmp =
  // makeDemandPointerAnalysis(TestInfo.SCOPE_FILE, mainClass);
  // CGNode testMethod =
  // AbstractPtrTest.findInstanceMethod(dmp.getBaseCallGraph(),
  // dmp.getClassHierarchy().lookupClass(
  // TypeReference.findOrCreate(ClassLoaderReference.Application,
  // "Ltestdata/TestOnTheFlyCS$C2")), Atom
  // .findOrCreateUnicodeAtom("doSomething"),
  // Descriptor.findOrCreateUTF8("(Ljava/lang/Object;)V"));
  // PointerKey keyToQuery = AbstractPtrTest.getParam(testMethod, "testThisVar",
  // dmp.getHeapModel());
  // Collection<InstanceKey> pointsTo = dmp.getPointsTo(keyToQuery);
  // if (debug) {
  // System.err.println("points-to for " + mainClass + ": " + pointsTo);
  // }
  // assertEquals(1, pointsTo.size());
  // }

  public void testWithinMethodCall() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    String mainClass = TestInfo.TEST_WITHIN_METHOD_CALL;
    final IDemandPointerAnalysis dmp = makeDemandPointerAnalysis(TestInfo.SCOPE_FILE, mainClass);

    CGNode testMethod = AbstractPtrTest.findStaticMethod(dmp.getBaseCallGraph(), Atom.findOrCreateUnicodeAtom("testMethod"),
        Descriptor.findOrCreateUTF8("(Ljava/lang/Object;)V"));
    PointerKey keyToQuery = AbstractPtrTest.getParam(testMethod, "testThisVar", dmp.getHeapModel());
    Collection<InstanceKey> pointsTo = dmp.getPointsTo(keyToQuery);
    if (debug) {
      System.err.println("points-to for " + mainClass + ": " + pointsTo);
    }
    assertEquals(1, pointsTo.size());
  }

  public void testLinkedListIter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_LINKEDLIST_ITER, 1);
  }

  public void testGlobal() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_GLOBAL, 1);
  }

  public void testHashSet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_HASH_SET, 2, 2, 1);
  }

  public void testHashMapGet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_HASHMAP_GET, 2, 1, 1);
  }

  public void testMethodRecursion() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_METHOD_RECURSION, 2);
  }

  public void testArraySetIter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAY_SET_ITER, 1);
  }

  public void testArrayList() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAY_LIST, 1);
  }

  public void testLinkedList() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_LINKED_LIST, 1);
  }

  @Override
  protected StateMachineFactory<IFlowLabel> getStateMachineFactory() {
    return new ContextSensitiveStateMachine.Factory();
  }

  @Override
  protected DemandRefinementPointsTo makeDemandPointerAnalysis(String scopeFile, String mainClass) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    DemandRefinementPointsTo dmp = super.makeDemandPointerAnalysis(scopeFile, mainClass);
    dmp.setRefinementPolicyFactory(new SinglePassRefinementPolicy.Factory(new AlwaysRefineFieldsPolicy(),
        new AlwaysRefineCGPolicy()));
    return dmp;
  }

}
