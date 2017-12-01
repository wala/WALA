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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.demandpa.TestInfo;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.IDemandPointerAnalysis;
import com.ibm.wala.demandpa.alg.statemachine.DummyStateMachine;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.CallGraphMapUtil;
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
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Driver that tests analysis results against ZeroOneCFA analysis.
 * 
 * @author Manu Sridharan
 * 
 */
public class CompareToZeroOneCFADriver {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // for (String testCase : TestInfo.ALL_TEST_CASES) {
    // runUnitTestCase(testCase);
    // }
    // runUnitTestCase(TestInfo.TEST_PRIMITIVES);
    // runApplication("/home/manu/research/DOMO/tests/JLex.jar");
  }

  @SuppressWarnings("unused")
  private static void runUnitTestCase(String mainClass) throws IllegalArgumentException, CancelException, IOException {
    System.err.println("=======---------------=============");
    System.err.println(("ANALYZING " + mainClass + "\n\n"));
    // describe the "scope", what is the program we're analyzing
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestInfo.SCOPE_FILE, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    Object warnings = new Object();

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

    // run existing pointer analysis
    doTests(scope, cha, options);
    System.err.println("ALL FINE");
  }

  @SuppressWarnings("unused")
  private static void runApplication(String appJar) throws IllegalArgumentException, CancelException, IOException {
    System.err.println("=======---------------=============");
    System.err.println(("ANALYZING " + appJar + "\n\n"));

    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, new File(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

    // TODO: return the warning set (need a CAPA type)
    // invoke DOMO to build a DOMO class hierarchy object
    Object warnings = new Object();
    ClassHierarchy cha = null;
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
    doTests(scope, cha, options);
    System.err.println("ALL FINE");
  }

  private static void doTests(AnalysisScope scope, final ClassHierarchy cha, AnalysisOptions options) throws IllegalArgumentException, CancelException {
    final SSAPropagationCallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    final CallGraph oldCG = builder.makeCallGraph(options,null);
    final PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

    // now, run our analysis
    // build an RTA call graph
    CallGraphBuilder<InstanceKey> rtaBuilder = Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha, scope);
    final CallGraph cg = rtaBuilder.makeCallGraph(options, null);
    // System.err.println(cg.toString());

    MemoryAccessMap fam = new SimpleMemoryAccessMap(cg, rtaBuilder.getPointerAnalysis().getHeapModel(), false);

    final IDemandPointerAnalysis dmp = makeDemandPointerAnalysis(options, cha, scope, cg, fam);

    final class Helper {
      void checkPointersInMethod(CGNode node) {
        // TODO remove this hack
        if (node.getMethod().getReference().toString().indexOf("clone()Ljava/lang/Object;") != -1) {
          System.err.println(("SKIPPING " + node));
          return;
        }
        CGNode oldNode = CallGraphMapUtil.mapCGNode(node, cg, oldCG);
        if (oldNode == null) {
          return;
        }
        System.err.println(("METHOD " + node));
        IR ir = node.getIR();
        TypeInference ti = TypeInference.make(ir, false);
        for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
          TypeAbstraction t = ti.getType(i);
          if (t != null) {
            final HeapModel heapModel = dmp.getHeapModel();
            LocalPointerKey pk = (LocalPointerKey) heapModel.getPointerKeyForLocal(node, i);
            LocalPointerKey oldPk = (LocalPointerKey) CallGraphMapUtil.mapPointerKey(pk, cg, oldCG, heapModel);
            Collection<InstanceKey> p2set = dmp.getPointsTo(pk);
            OrdinalSet<InstanceKey> otherP2Set = pa.getPointsToSet(oldPk);
            System.err.println(("OLD POINTS-TO " + otherP2Set));
            for (InstanceKey key : otherP2Set) {
              if (knownBug(key)) {
                continue;
              }
              InstanceKey newKey = CallGraphMapUtil.mapInstKey(key, oldCG, cg, heapModel);
              if (!p2set.contains(newKey)) {
                System.err.println("BADNESS");
                System.err.println(("pointer key " + pk));
                System.err.println(("missing " + newKey));
                Assertions.UNREACHABLE();
              }
            }
          }
        }
      }

    }
    Helper h = new Helper();
    for (CGNode node : cg) {
      h.checkPointersInMethod(node);
    }
  }

  private static boolean knownBug(InstanceKey key) {
    // if (key instanceof MultiNewArrayAllocationSiteKey) {
    // return true;
    // }
    if (key instanceof InstanceKeyWithNode) {
      CGNode node = ((InstanceKeyWithNode) key).getNode();
      MethodReference methodRef = node.getMethod().getReference();
      if (methodRef.toString().equals("< Primordial, Ljava/lang/Object, clone()Ljava/lang/Object; >")) {
        return true;
      }
    }
    return false;
  }

  private static IDemandPointerAnalysis makeDemandPointerAnalysis(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope,
      CallGraph cg, MemoryAccessMap fam) {
    SSAPropagationCallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    // return new TestNewGraphPointsTo(cg, builder, fam, cha, warnings);
    DemandRefinementPointsTo fullDemandPointsTo = DemandRefinementPointsTo.makeWithDefaultFlowGraph(cg, builder, fam, cha, options, new DummyStateMachine.Factory<IFlowLabel>());
    // fullDemandPointsTo.setOnTheFly(true);
    // fullDemandPointsTo.setRefineFields(true);
    return fullDemandPointsTo;
  }

}
