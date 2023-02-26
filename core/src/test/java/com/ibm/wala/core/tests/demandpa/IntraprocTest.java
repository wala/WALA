/*
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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo.PointsToResult;
import com.ibm.wala.demandpa.alg.IntraProcFilter;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;

public class IntraprocTest extends AbstractPtrTest {

  public IntraprocTest() {
    super(TestInfo.SCOPE_FILE);
  }

  @Test
  public void testId()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doPointsToSizeTest(TestInfo.TEST_ID, 0);
  }

  @Test
  public void testMissingClassMetadataRef()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    DemandRefinementPointsTo drpt = makeDemandPointerAnalysis("Lmissingmetadata/MissingClassRef");
    // find the call to toString() in the application code and make sure we can get the points-to
    // set for its receiver
    for (CGNode node : drpt.getBaseCallGraph()) {
      if (!node.getMethod()
          .getDeclaringClass()
          .getClassLoader()
          .getReference()
          .equals(ClassLoaderReference.Application)) {
        continue;
      }
      IR ir = node.getIR();
      if (ir == null) continue;
      Iterator<CallSiteReference> callSites = ir.iterateCallSites();
      while (callSites.hasNext()) {
        CallSiteReference site = callSites.next();
        if (site.getDeclaredTarget().getName().toString().equals("toString")) {
          System.out.println(site + " in " + node);
          SSAAbstractInvokeInstruction[] calls = ir.getCalls(site);
          PointerKey pk = drpt.getHeapModel().getPointerKeyForLocal(node, calls[0].getUse(0));
          Pair<PointsToResult, Collection<InstanceKey>> pointsTo = drpt.getPointsTo(pk, k -> true);
          System.out.println("POINTS TO RESULT: " + pointsTo);
        }
      }
    }
  }

  @Override
  protected StateMachineFactory<IFlowLabel> getStateMachineFactory() {
    return new IntraProcFilter.Factory();
  }
}
