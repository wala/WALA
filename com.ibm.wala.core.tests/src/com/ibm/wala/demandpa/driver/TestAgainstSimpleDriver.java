/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/**
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
package com.ibm.wala.demandpa.driver;

import java.io.IOException;
import java.util.Collection;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.demandpa.TestInfo;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.IDemandPointerAnalysis;
import com.ibm.wala.demandpa.alg.SimpleDemandPointsTo;
import com.ibm.wala.demandpa.alg.refinepolicy.AlwaysRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.AlwaysRefineFieldsPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.DummyStateMachine;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.SimpleMemoryAccessMap;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

/**
 * Driver that tests a pointer analysis results against the results of
 * {@link SimpleDemandPointsTo}.
 * 
 * @author Manu Sridharan
 * 
 */
public class TestAgainstSimpleDriver {

  private static final boolean VERBOSE = false;

  public static void main(String[] args) throws IllegalArgumentException, CancelException, IOException {

    // for (String String : ALL_TEST_CASES) {
    // runAnalysisForString(String.mainClass, String.scopeFileName);
    // }
    runAnalysisForTestCase(TestInfo.TEST_ARRAY_SET_ITER);
    System.err.println("computed all points-to sets successfully");
  }

  private static void runAnalysisForTestCase(String mainClass) throws IllegalArgumentException, CancelException, IOException {
    System.err.println("=======---------------=============");
    System.err.println(("ANALYZING " + mainClass + "\n\n"));
    // describe the "scope", what is the program we're analyzing
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestInfo.SCOPE_FILE, CallGraphTestUtil.REGRESSION_EXCLUSIONS);

    // build a type hierarchy
    ClassHierarchy cha = null;
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // set up call graph construction options; mainly what should be considered
    // entrypoints?
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    // build an RTA call graph
    CallGraphBuilder<InstanceKey> rtaBuilder = Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha, scope);
    final CallGraph cg = rtaBuilder.makeCallGraph(options, null);
    // System.err.println(cg.toString());

    MemoryAccessMap fam = new SimpleMemoryAccessMap(cg, rtaBuilder.getPointerAnalysis().getHeapModel(), false);
    // System.err.println(fam.toString());

    IDemandPointerAnalysis dmp = makeDemandPointerAnalysis(options, cha, scope, cg, fam);

    IDemandPointerAnalysis simpleDmp = new SimpleDemandPointsTo(cg, dmp.getHeapModel(), fam, cha, options);
    CGNode main = cg.getEntrypointNodes().iterator().next();

    IR ir = main.getIR();
    TypeInference ti = TypeInference.make(ir, false);
    for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
      TypeAbstraction t = ti.getType(i);
      if (t != null) {
        LocalPointerKey v = (LocalPointerKey) dmp.getHeapModel().getPointerKeyForLocal(main, i);
        Collection<InstanceKey> p = dmp.getPointsTo(v);
        Collection<InstanceKey> oldP = simpleDmp.getPointsTo(v);
        if (!sameContents(p, oldP)) {
          System.err.println(("different result for " + v));
          System.err.println(("old " + oldP + "\n\nnew " + p));
        }
        printResult(v, p);
      }
    }
  }

  private static <T> boolean sameContents(Collection<T> c1, Collection<T> c2) {
    return c1.containsAll(c2) && c2.containsAll(c1);
  }

  private static void printResult(PointerKey pk, Collection<InstanceKey> result) {
    if (VERBOSE) {
      System.err.println("points-to for " + pk);
      if (result.isEmpty()) {
        System.err.println("  EMPTY!");
      }
      for (InstanceKey instanceKey : result) {
        System.err.println("  " + instanceKey);
      }
    }
  }

  private static IDemandPointerAnalysis makeDemandPointerAnalysis(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope,
      CallGraph cg, MemoryAccessMap fam) {
    SSAPropagationCallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
    // return new TestNewGraphPointsTo(cg, builder, fam, cha, warnings);
    DemandRefinementPointsTo fullDemandPointsTo = DemandRefinementPointsTo.makeWithDefaultFlowGraph(cg, builder, fam, cha, options, new DummyStateMachine.Factory<IFlowLabel>());
    // fullDemandPointsTo.setCGRefinePolicy(new AlwaysRefineCGPolicy());
    // fullDemandPointsTo.setFieldRefinePolicy(new AlwaysRefineFieldsPolicy());
    fullDemandPointsTo.setRefinementPolicyFactory(new SinglePassRefinementPolicy.Factory(new AlwaysRefineFieldsPolicy(),
        new AlwaysRefineCGPolicy()));
    return fullDemandPointsTo;
  }

}
