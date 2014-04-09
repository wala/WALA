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

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

/**
 * Note that in this test we still do refinement of the array contents
 * pseudo-field, to avoid excessive sensitivity to library versions.
 */
public class NoRefinePtrTest extends AbstractPtrTest {

  public NoRefinePtrTest() {
    super(TestInfo.SCOPE_FILE);
  }

  @Test
  public void testOnTheFlySimple() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ONTHEFLY_SIMPLE, 1);
  }

  @Test
  public void testArraySet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ARRAY_SET, 2);
  }

  @Test
  public void testArraySetIter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ARRAY_SET_ITER, 2);
  }

  @Test
  public void testHashSet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_HASH_SET, 2);
  }

  @Test
  public void testMethodRecursion() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_METHOD_RECURSION, 2);
  }

  @Test
  public void testFooId() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ID, 2);
  }

  @Test
  public void testLocals() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_LOCALS, 1);
  }

  @Test
  public void testArrays() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ARRAYS, 2);
  }

  @Test
  public void testFields() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_FIELDS, 2);
  }

  @Test
  public void testFieldsHarder() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_FIELDS_HARDER, 2);
  }

  @Test
  public void testGetterSetter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_GETTER_SETTER, 2);
  }

  @Test
  public void testException() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_EXCEPTION, 4);
  }

  @Test
  public void testMultiDim() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_MULTI_DIM, 2);
  }

  @Test
  public void testGlobal() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_GLOBAL, 1);
  }
  
  @Test
  public void testFlowsToLocals() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // local var, init of FlowsToType, init of Object, and param of TestUtil.makeVarUsed()
    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_LOCALS, 4);
  }

  @Test
  public void testFlowsToId() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_ID, 8);
  }

  @Test
  public void testFlowsToFields() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_FIELDS, 6);
  }

  @Test
  public void testFlowsToFieldsHarder() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_FIELDS_HARDER, 6);
  }

  @Test
  public void testFlowsToArraySetIter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_ARRAYSET_ITER, 8);
  }

  // don't test this until we have a way to handle different library versions
//  @Test
//  public void testFlowsToHashSet() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
//    doFlowsToSizeTest(TestInfo.FLOWSTO_TEST_HASHSET, 8);
//  }
}
