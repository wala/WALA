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
import org.junit.AfterClass;
import org.junit.Assert;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.OnlyArraysPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.DummyStateMachine;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PABasedMemoryAccessMap;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

public abstract class AbstractPtrTest {

  protected boolean debug = false;

  /**
   * file holding analysis scope specification
   */
  protected final String scopeFile;

  protected AbstractPtrTest(String scopeFile) {
    this.scopeFile = scopeFile;
  }

  private static AnalysisScope cachedScope;

  private static IClassHierarchy cachedCHA;

  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    for (CGNode n : Iterator2Iterable.make(cg.getSuccNodes(cg.getFakeRootNode()))) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method");
    return null;
  }

  public static CGNode findStaticMethod(CallGraph cg, Atom name, Descriptor args) {
    for (CGNode n : cg) {
      // System.err.println(n.getMethod().getName() + " " +
      // n.getMethod().getDescriptor());
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(args)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method");
    return null;
  }

  public static CGNode findInstanceMethod(CallGraph cg, IClass declaringClass, Atom name, Descriptor args) {
    for (CGNode n : cg) {
      // System.err.println(n.getMethod().getDeclaringClass() + " " +
      // n.getMethod().getName() + " " + n.getMethod().getDescriptor());
      if (n.getMethod().getDeclaringClass().equals(declaringClass) && n.getMethod().getName().equals(name)
          && n.getMethod().getDescriptor().equals(args)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method");
    return null;
  }

  public static PointerKey getParam(CGNode n, String methodName, HeapModel heapModel) {
    IR ir = n.getIR();
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction) s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
          Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
          SSAInstruction callInstr = ir.getInstructions()[indices.intIterator().next()];
          Assertions.productionAssertion(callInstr.getNumberOfUses() == 1, "multiple uses for call");
          return heapModel.getPointerKeyForLocal(n, callInstr.getUse(0));
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }


  protected void doFlowsToSizeTest(String mainClass, int size) throws ClassHierarchyException, IllegalArgumentException,
      CancelException, IOException {
    Collection<PointerKey> flowsTo = getFlowsToSetToTest(mainClass);
    if (debug) {
      System.err.println("flows-to for " + mainClass + ": " + flowsTo);
    }
    Assert.assertEquals(size, flowsTo.size());
  }

  private Collection<PointerKey> getFlowsToSetToTest(String mainClass) throws ClassHierarchyException, IllegalArgumentException,
      CancelException, IOException {
    final DemandRefinementPointsTo dmp = makeDemandPointerAnalysis(mainClass);

    // find the single allocation site of FlowsToType, make an InstanceKey, and
    // query it
    CGNode mainMethod = AbstractPtrTest.findMainMethod(dmp.getBaseCallGraph());
    InstanceKey keyToQuery = getFlowsToInstanceKey(mainMethod, dmp.getHeapModel());
    Collection<PointerKey> flowsTo = dmp.getFlowsTo(keyToQuery).snd;
    return flowsTo;
  }

  /**
   * returns the instance key corresponding to the single allocation site of
   * type FlowsToType
   */
  private InstanceKey getFlowsToInstanceKey(CGNode mainMethod, HeapModel heapModel) {
    // TODO Auto-generated method stub
    TypeReference flowsToTypeRef = TypeReference.findOrCreate(ClassLoaderReference.Application,
        StringStuff.deployment2CanonicalTypeString("demandpa.FlowsToType"));
    final IR mainIR = mainMethod.getIR();
    if (debug) {
      System.err.println(mainIR);
    }
    for (NewSiteReference n : Iterator2Iterable.make(mainIR.iterateNewSites())) {
      if (n.getDeclaredType().equals(flowsToTypeRef)) {
        return heapModel.getInstanceKeyForAllocation(mainMethod, n);
      }
    }
    assert false : "could not find appropriate allocation";
    return null;
  }

  protected void doPointsToSizeTest(String mainClass, int expectedSize) throws ClassHierarchyException, IllegalArgumentException,
      CancelException, IOException {
    Collection<InstanceKey> pointsTo = getPointsToSetToTest(mainClass);
    if (debug) {
      System.err.println("points-to for " + mainClass + ": " + pointsTo);
    }
    Assert.assertEquals(expectedSize, pointsTo.size());
  }

  private Collection<InstanceKey> getPointsToSetToTest(String mainClass) throws ClassHierarchyException, IllegalArgumentException,
      CancelException, IOException {
    final DemandRefinementPointsTo dmp = makeDemandPointerAnalysis(mainClass);

    // find the testThisVar call, and check the parameter's points-to set
    CGNode mainMethod = AbstractPtrTest.findMainMethod(dmp.getBaseCallGraph());
    PointerKey keyToQuery = AbstractPtrTest.getParam(mainMethod, "testThisVar", dmp.getHeapModel());
    Collection<InstanceKey> pointsTo = dmp.getPointsTo(keyToQuery);
    return pointsTo;
  }

  protected DemandRefinementPointsTo makeDemandPointerAnalysis(String mainClass) throws ClassHierarchyException,
      IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    // build a type hierarchy
    IClassHierarchy cha = findOrCreateCHA(scope);

    // set up call graph construction options; mainly what should be considered
    // entrypoints?
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    final IAnalysisCacheView analysisCache = new AnalysisCacheImpl();
    CallGraphBuilder<InstanceKey> cgBuilder = Util.makeZeroCFABuilder(Language.JAVA, options, analysisCache, cha, scope);
    final CallGraph cg = cgBuilder.makeCallGraph(options, null);
    // System.err.println(cg.toString());

    // MemoryAccessMap mam = new SimpleMemoryAccessMap(cg,
    // cgBuilder.getPointerAnalysis().getHeapModel(), false);
    MemoryAccessMap mam = new PABasedMemoryAccessMap(cg, cgBuilder.getPointerAnalysis());
    SSAPropagationCallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, analysisCache, cha, scope);
    DemandRefinementPointsTo fullDemandPointsTo = DemandRefinementPointsTo.makeWithDefaultFlowGraph(cg, builder, mam, cha, options,
        getStateMachineFactory());

    // always refine array fields; otherwise, can be very sensitive to differences
    // in library versions.  otherwise, no refinement by default
    fullDemandPointsTo.setRefinementPolicyFactory(new SinglePassRefinementPolicy.Factory(new OnlyArraysPolicy(), new NeverRefineCGPolicy()));
    return fullDemandPointsTo;
  }

  /**
   * @param scope
   * @throws ClassHierarchyException
   */
  private static IClassHierarchy findOrCreateCHA(AnalysisScope scope) throws ClassHierarchyException {
    if (cachedCHA == null) {
      cachedCHA = ClassHierarchyFactory.make(scope);
    }
    return cachedCHA;
  }

  /**
   * @param scopeFile
   * @throws IOException
   */
  private AnalysisScope findOrCreateAnalysisScope() throws IOException {
    if (cachedScope == null) {
      cachedScope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    }
    return cachedScope;
  }

  @AfterClass
  public static void cleanup() {
    cachedScope = null;
    cachedCHA = null;
  }

  protected StateMachineFactory<IFlowLabel> getStateMachineFactory() {
    return new DummyStateMachine.Factory<>();
  }

}
