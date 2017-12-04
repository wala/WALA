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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.demandpa.AbstractPtrTest;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Tests for Call Graph construction
 */

public class CallGraphTest extends WalaTestCase {

  public static void main(String[] args) {
    justThisTest(CallGraphTest.class);
  }


  @Test public void testJava_cup() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.JAVA_CUP, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.JAVA_CUP_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope, useShortProfile());
  }

  @Test public void testBcelVerifier() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.BCEL, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.BCEL_VERIFIER_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    // this speeds up the test
    options.setReflectionOptions(ReflectionOptions.NONE);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);
  }

  @Test public void testJLex() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.JLEX, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util
        .makeMainEntrypoints(scope, cha, TestConstants.JLEX_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);
  }

  @Test public void testCornerCases() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    // this speeds up the test
    options.setReflectionOptions(ReflectionOptions.NONE);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);

    // we expect a warning or two about class Abstract1, which has no concrete
    // subclasses
    String ws = Warnings.asString();
    Assert.assertTrue("failed to report a warning about Abstract1", ws.indexOf("cornerCases/Abstract1") > -1);

    // we do not expect a warning about class Abstract2, which has a concrete
    // subclasses
    Assert.assertTrue("reported a warning about Abstract2", ws.indexOf("cornerCases/Abstract2") == -1);
  }

  @Test public void testHello() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    if (analyzingJar()) return;
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.HELLO, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.HELLO_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);
  }

  @Test public void testStaticInit() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "LstaticInit/TestStaticInit");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
    boolean foundDoNothing = false;
    for (CGNode n : cg) {
      if (n.toString().contains("doNothing")) {
        foundDoNothing = true;
        break;
      }
    }
    Assert.assertTrue(foundDoNothing);
    options.setHandleStaticInit(false);
    cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
    for (CGNode n : cg) {
      Assert.assertTrue(!n.toString().contains("doNothing"));
    }
  }

  @Test public void testJava8Smoke() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Llambda/SortingExample");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
    boolean foundSortForward = false;
    for (CGNode n : cg) {
      if (n.toString().contains("sortForward")) {
        foundSortForward = true;
      }
    }
    Assert.assertTrue("expected for sortForward", foundSortForward);
  }
  
  @Test public void testSystemProperties() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "LstaticInit/TestSystemProperties");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCacheImpl(), cha, scope);    
    CallGraph cg = builder.makeCallGraph(options);
    for (CGNode n : cg) {
      if (n.toString().equals("Node: < Application, LstaticInit/TestSystemProperties, main([Ljava/lang/String;)V > Context: Everywhere")) {
        boolean foundToCharArray = false;
        for (CGNode callee : Iterator2Iterable.make(cg.getSuccNodes(n))) {
          if (callee.getMethod().getName().toString().equals("toCharArray")) {
            foundToCharArray = true;
            break;
          }
        }
        Assert.assertTrue(foundToCharArray);
        break;
      }
    }
  }

  @Test public void testRecursion() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.RECURSE_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);
  }

  @Test public void testHelloAllEntrypoints() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    if (analyzingJar()) return;    
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.HELLO, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    doCallGraphs(options, new AnalysisCacheImpl(), cha, scope);
  }

  @Test public void testIO() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope("primordial.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = makePrimordialPublicEntrypoints(cha, "java/io");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
  }

  public static Iterable<Entrypoint> makePrimordialPublicEntrypoints(ClassHierarchy cha, String pkg) {
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IClass clazz : cha) {

      if (clazz.getName().toString().indexOf(pkg) != -1 && !clazz.isInterface() && !clazz.isAbstract()) {
        for (IMethod method : clazz.getDeclaredMethods()) {
          if (method.isPublic() && !method.isAbstract()) {
            System.out.println("Entry:" + method.getReference());
            result.add(new DefaultEntrypoint(method, cha));
          }
        }
      }
    }
    return result::iterator;
  }
  
  @Test public void testPrimordial() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    if (useShortProfile()) {
      return;
    }

    AnalysisScope scope = 
      CallGraphTestUtil.makeJ2SEAnalysisScope(
          "primordial.txt", 
          (System.getProperty("os.name").equals("Mac OS X"))?
          "Java60RegressionExclusions.txt":
          "GUIExclusions.txt");
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = makePrimordialMainEntrypoints(cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
  }

  @Test
  public void testZeroOneContainerCopyOf() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, "Ldemandpa/TestArraysCopyOf");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
    CGNode mainMethod = AbstractPtrTest.findMainMethod(cg);
    PointerKey keyToQuery = AbstractPtrTest.getParam(mainMethod, "testThisVar", pa.getHeapModel());
    OrdinalSet<InstanceKey> pointsToSet = pa.getPointsToSet(keyToQuery);
    Assert.assertEquals(1, pointsToSet.size());
    
  }
  /**
   * make main entrypoints, even in the primordial loader.
   */
  public static Iterable<Entrypoint> makePrimordialMainEntrypoints(ClassHierarchy cha) {
    final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IClass klass : cha) {
      MethodReference mainRef = MethodReference.findOrCreate(klass.getReference(), mainMethod, Descriptor
          .findOrCreateUTF8("([Ljava/lang/String;)V"));
      IMethod m = klass.getMethod(mainRef.getSelector());
      if (m != null) {
        result.add(new DefaultEntrypoint(m, cha));
      }
    }
    return result::iterator;
  }

  public static void doCallGraphs(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope)
      throws IllegalArgumentException, CancelException {
    doCallGraphs(options, cache, cha, scope, false);
  }

  /**
   * TODO: refactor this to avoid excessive code bloat.
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public static void doCallGraphs(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope,
      boolean testPAToString) throws IllegalArgumentException, CancelException {

    // ///////////////
    // // RTA /////
    // ///////////////
    CallGraph cg = CallGraphTestUtil.buildRTA(options, cache, cha, scope);
    try {
      GraphIntegrity.check(cg);
    } catch (UnsoundGraphException e1) {
      e1.printStackTrace();
      Assert.assertTrue(e1.getMessage(), false);
    }

    Set<MethodReference> rtaMethods = CallGraphStats.collectMethods(cg);
    System.err.println("RTA methods reached: " + rtaMethods.size());
    System.err.println(CallGraphStats.getStats(cg));
    System.err.println("RTA warnings:\n");

    // ///////////////
    // // 0-CFA /////
    // ///////////////
    cg = CallGraphTestUtil.buildZeroCFA(options, cache, cha, scope, testPAToString);

    // FIXME: annoying special cases caused by clone2assign mean using
    // the rta graph for proper graph subset checking does not work.
    // (note that all the other such checks do use proper graph subset)
    Graph<MethodReference> squashZero = checkCallGraph(cg, null, rtaMethods, "0-CFA");

    // test Pretransitive 0-CFA
    // not currently supported
    // warnings = new WarningSet();
    // options.setUsePreTransitiveSolver(true);
    // CallGraph cgP = CallGraphTestUtil.buildZeroCFA(options, cha, scope,
    // warnings);
    // options.setUsePreTransitiveSolver(false);
    // Graph squashPT = checkCallGraph(warnings, cgP, squashZero, null, "Pre-T
    // 1");
    // checkCallGraph(warnings, cg, squashPT, null, "Pre-T 2");

    // ///////////////
    // // 0-1-CFA ///
    // ///////////////
    cg = CallGraphTestUtil.buildZeroOneCFA(options, cache, cha, scope, testPAToString);
    Graph<MethodReference> squashZeroOne = checkCallGraph(cg, squashZero, null, "0-1-CFA");

    // ///////////////////////////////////////////////////
    // // 0-CFA augmented to disambiguate containers ///
    // ///////////////////////////////////////////////////
    cg = CallGraphTestUtil.buildZeroContainerCFA(options, cache, cha, scope);
    Graph<MethodReference> squashZeroContainer = checkCallGraph(cg, squashZero, null, "0-Container-CFA");

    // ///////////////////////////////////////////////////
    // // 0-1-CFA augmented to disambiguate containers ///
    // ///////////////////////////////////////////////////
    cg = CallGraphTestUtil.buildZeroOneContainerCFA(options, cache, cha, scope);
    checkCallGraph(cg, squashZeroContainer, null, "0-1-Container-CFA");
    checkCallGraph(cg, squashZeroOne, null, "0-1-Container-CFA");

    // test ICFG
    checkICFG(cg);
    return;
    // /////////////
    // // 1-CFA ///
    // /////////////
    // warnings = new WarningSet();
    // cg = buildOneCFA();

  }

  /**
   * Check properties of the InterproceduralCFG
   * 
   * @param cg
   */
  private static void checkICFG(CallGraph cg) {
    InterproceduralCFG icfg = new InterproceduralCFG(cg);

    try {
      GraphIntegrity.check(icfg);
    } catch (UnsoundGraphException e) {
      e.printStackTrace();
      Assert.assertTrue(false);
    }

    // perform a little icfg exercise
    @SuppressWarnings("unused")
    int count = 0;
    for (BasicBlockInContext<ISSABasicBlock> bb : icfg) {
      if (icfg.hasCall(bb)) {
        count++;
      }
    }
  }

  /**
   * Check consistency of a callgraph, and check that this call graph is a subset of a super-graph
   * 
   * @param warnings object to track warnings
   * @param cg
   * @param superCG
   * @param superMethods
   * @param thisAlgorithm
   * @return a squashed version of cg
   */
  private static Graph<MethodReference> checkCallGraph(CallGraph cg, Graph<MethodReference> superCG,
      Set<MethodReference> superMethods, String thisAlgorithm) {
    try {
      GraphIntegrity.check(cg);
    } catch (UnsoundGraphException e1) {
      Assert.assertTrue(e1.getMessage(), false);
    }
    Set<MethodReference> callGraphMethods = CallGraphStats.collectMethods(cg);
    System.err.println(thisAlgorithm + " methods reached: " + callGraphMethods.size());
    System.err.println(CallGraphStats.getStats(cg));

    Graph<MethodReference> thisCG = squashCallGraph(thisAlgorithm, cg);

    if (superCG != null) {
      com.ibm.wala.ipa.callgraph.impl.Util.checkGraphSubset(superCG, thisCG);
    } else {
      // SJF: RTA has rotted a bit since it doesn't handle LoadClass instructions.
      // Commenting this out for now.
      // if (!superMethods.containsAll(callGraphMethods)) {
      // Set<MethodReference> temp = HashSetFactory.make();
      // temp.addAll(callGraphMethods);
      // temp.removeAll(superMethods);
      // System.err.println("Violations");
      // for (MethodReference m : temp) {
      // System.err.println(m);
      // }
      // Assertions.UNREACHABLE();
      // }
    }

    return thisCG;
  }

  /**
   * @param name
   * @param cg
   * @return a graph whose nodes are MethodReferences, and whose edges represent calls between MethodReferences
   * @throws IllegalArgumentException if cg is null
   */
  public static Graph<MethodReference> squashCallGraph(final String name, final CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    final Set<MethodReference> nodes = HashSetFactory.make();
    for (CGNode cgNode : cg) {
      nodes.add((cgNode).getMethod().getReference());
    }

    return new Graph<MethodReference>() {
      @Override
      public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("squashed " + name + " call graph\n");
        result.append("Original graph:");
        result.append(cg.toString());
        return result.toString();
      }

      /*
       * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
       */
      @Override
      public Iterator<MethodReference> iterator() {
        return nodes.iterator();
      }

      /*
       * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
       */
      @Override
      public boolean containsNode(MethodReference N) {
        return nodes.contains(N);
      }

      /*
       * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
       */
      @Override
      public int getNumberOfNodes() {
        return nodes.size();
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
       */
      @Override
      public Iterator<MethodReference> getPredNodes(MethodReference N) {
        Set<MethodReference> pred = HashSetFactory.make(10);
        MethodReference methodReference = N;
        for (CGNode cgNode : cg.getNodes(methodReference))
          for (CGNode p : Iterator2Iterable.make(cg.getPredNodes(cgNode)))
            pred.add(p.getMethod().getReference());

        return pred.iterator();
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
       */
      @Override
      public int getPredNodeCount(MethodReference N) {
        return IteratorUtil.count(getPredNodes(N));
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
       */
      @Override
      public Iterator<MethodReference> getSuccNodes(MethodReference N) {
        Set<MethodReference> succ = HashSetFactory.make(10);
        MethodReference methodReference = N;
        for (CGNode node : cg.getNodes(methodReference))
          for (CGNode p : Iterator2Iterable.make(cg.getSuccNodes(node)))
            succ.add(p.getMethod().getReference());

        return succ.iterator();
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
       */
      @Override
      public int getSuccNodeCount(MethodReference N) {
        return IteratorUtil.count(getSuccNodes(N));
      }

      /*
       * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
       */
      @Override
      public void addNode(MethodReference n) {
        Assertions.UNREACHABLE();
      }

      /*
       * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
       */
      @Override
      public void removeNode(MethodReference n) {
        Assertions.UNREACHABLE();
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
       */
      @Override
      public void addEdge(MethodReference src, MethodReference dst) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeEdge(MethodReference src, MethodReference dst) {
        Assertions.UNREACHABLE();
      }

      /*
       * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
       */
      @Override
      public void removeAllIncidentEdges(MethodReference node) {
        Assertions.UNREACHABLE();
      }

      /*
       * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
       */
      @Override
      public void removeNodeAndEdges(MethodReference N) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeIncomingEdges(MethodReference node) {
        // TODO Auto-generated method stubMethodReference
        Assertions.UNREACHABLE();

      }

      @Override
      public void removeOutgoingEdges(MethodReference node) {
        // TODO Auto-generated method stub
        Assertions.UNREACHABLE();

      }

      @Override
      public boolean hasEdge(MethodReference src, MethodReference dst) {
        for (MethodReference succ : Iterator2Iterable.make(getSuccNodes(src))) {
          if (dst.equals(succ)) {
            return true;
          }
        }
        return false;
      }
    };
  }

}
