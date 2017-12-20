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
package com.ibm.wala.core.tests.slicer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.examples.drivers.PDFSlice;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

public class SlicerTest {

  private static AnalysisScope cachedScope;

  // more aggressive exclusions to avoid library blowup
  // in interprocedural tests
  private static final String EXCLUSIONS = "java\\/awt\\/.*\n" +
      "javax\\/swing\\/.*\n" +
      "sun\\/awt\\/.*\n" +
      "sun\\/swing\\/.*\n" +
      "com\\/sun\\/.*\n" +
      "sun\\/.*\n" +
      "org\\/netbeans\\/.*\n" +
      "org\\/openide\\/.*\n" +
      "com\\/ibm\\/crypto\\/.*\n" +
      "com\\/ibm\\/security\\/.*\n" +
      "org\\/apache\\/xerces\\/.*\n" +
      "java\\/security\\/.*\n" +
      "";

  private static AnalysisScope findOrCreateAnalysisScope() throws IOException {
    if (cachedScope == null) {
      cachedScope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, null, SlicerTest.class.getClassLoader());
      cachedScope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));

    }
    return cachedScope;
  }

  private static IClassHierarchy cachedCHA;

  private static IClassHierarchy findOrCreateCHA(AnalysisScope scope) throws ClassHierarchyException {
    if (cachedCHA == null) {
      cachedCHA = ClassHierarchyFactory.make(scope);
    }
    return cachedCHA;
  }

  @AfterClass
  public static void afterClass() {
    cachedCHA = null;
    cachedScope = null;
  }

  @Test
  public void testSlice1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE1_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> computeBackwardSlice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    Collection<Statement> slice = computeBackwardSlice;
    dumpSlice(slice);

    int i = 0;
    for (Statement st : slice) {
      if (st.getNode().getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        i++;
      }
    }
    Assert.assertEquals(16, i);
  }

  @Test
  public void testSlice2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE2_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMethod(cg, "baz");

    Statement s = findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> computeBackwardSlice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    Collection<Statement> slice = computeBackwardSlice;
    dumpSlice(slice);

    Assert.assertEquals(slice.toString(), 9, countNormals(slice));
  }

  @Test
  public void testSlice3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE3_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMethod(cg, "main");

    Statement s = findCallTo(main, "doNothing");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countAllocations(slice));
  }

  @Test
  public void testSlice4() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE4_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);
    Statement s = findCallTo(main, "foo");
    s = PDFSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 4, slice.size());
  }

  @Test
  public void testSlice5() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE5_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode n = findMethod(cg, "baz");
    Statement s = findCallTo(n, "foo");
    s = PDFSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 7, slice.size());
  }

  /**
   * test unreproduced bug reported on mailing list by Sameer Madan, 7/3/2007
   *
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @Test
  public void testSlice7() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE7_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);
    Statement s = findFirstAllocation(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    dumpSlice(slice);
  }

  /**
   * test bug reported on mailing list by Ravi Chandhran, 4/16/2010
   *
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  @Test
  public void testSlice8() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE8_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode process = findMethod(cg, Descriptor.findOrCreateUTF8("()V"), Atom.findOrCreateUnicodeAtom("process"));
    Statement s = findCallToDoNothing(process);
    System.err.println("Statement: " + s);
    // compute a backward slice, with data dependence and no exceptional control dependence
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    dumpSlice(slice);
    Assert.assertEquals(4, countInvokes(slice));
    // should only get 4 statements total when ignoring control dependences completely
    slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    Assert.assertEquals(slice.toString(), 4, slice.size());
  }

  @Test
  public void testSlice9() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE9_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);
    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a backward slice, with data dependence and no exceptional control dependence
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    //dumpSlice(slice);
    Assert.assertEquals(/*slice.toString(), */5, countApplicationNormals(slice));
  }

  @Test
  public void testTestCD1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countConditionals(slice));
  }

  @Test
  public void testTestCD2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD2);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countConditionals(slice));
  }

  @Test
  public void testTestCD3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD3);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 0, countConditionals(slice));
  }

  @Test
  public void testTestCD4() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD4);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(0, countConditionals(slice));

    // compute a full slice
    slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countConditionals(slice));
  }

  @Test
  public void testTestCD5() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD5);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.NONE, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    dumpSlice(slice);
    Assert.assertEquals(10, slice.size());
    Assert.assertEquals(3, countReturns(slice));
  }

  @Test
  public void testTestCD5NoInterproc() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD5);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.NONE, ControlDependenceOptions.NO_INTERPROC_NO_EXCEPTION);
    dumpSlice(slice);
    Assert.assertEquals(8, slice.size());
    Assert.assertEquals(2, countReturns(slice));
  }

  @Test
  public void testTestCD6() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTCD6);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.NONE, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countInvokes(slice));
  }

  @Test
  public void testTestId() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTID);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countAllocations(slice));
  }

  @Test
  public void testTestArrays() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTARRAYS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countAloads(slice));
  }

  @Test
  public void testTestFields() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTFIELDS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countPutfields(slice));
  }

  @Test
  public void testThin1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTTHIN1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute normal data slice
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(3, countAllocations(slice));
    Assert.assertEquals(2, countPutfields(slice));

    // compute thin slice .. ignore base pointers
    Collection<Statement> computeBackwardSlice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis,
        DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
    slice = computeBackwardSlice;
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countPutfields(slice));
  }

  @Test
  public void testTestGlobal() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTGLOBAL);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 2, countPutstatics(slice));
    Assert.assertEquals(slice.toString(), 2, countGetstatics(slice));
  }

  @Test
  public void testTestMultiTarget() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTMULTITARGET);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, countAllocations(slice));
  }

  @Test
  public void testTestRecursion() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTRECURSION);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 3, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 2, countPutfields(slice));
  }

  @Test
  public void testPrimGetterSetter() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TEST_PRIM_GETTER_SETTER);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode test = findMethod(cg, "test");

    PartialCallGraph pcg = PartialCallGraph.make(cg, Collections.singleton(test));

    Statement s = findCallToDoNothing(test);
    System.err.println("Statement: " + s);

    // compute full slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, pcg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 0, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countPutfields(slice));
  }

  /**
   * Test of using N-CFA builder to distinguish receiver objects for two calls
   * to a getter method. Also tests disabling SMUSH_PRIMITIVE_HOLDERS to ensure
   * we get distinct abstract objects for two different primitive holders.
   */
  @Test
  public void testPrimGetterSetter2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TEST_PRIM_GETTER_SETTER2);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    SSAPropagationCallGraphBuilder builder = new nCFABuilder(1, cha, options, new AnalysisCacheImpl(), appSelector, appInterpreter);
    // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
    // NOTE: we disable ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS for this test, since IntWrapper
    // is a primitive holder
    builder.setInstanceKeys(new ZeroXInstanceKeys(options, cha, builder.getContextInterpreter(), ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_MANY /* | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS */ | ZeroXInstanceKeys.SMUSH_STRINGS
        | ZeroXInstanceKeys.SMUSH_THROWABLES));

    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode test = findMainMethod(cg);

    PartialCallGraph pcg = PartialCallGraph.make(cg, Collections.singleton(test));

    Statement s = findCallToDoNothing(test);
    System.err.println("Statement: " + s);

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, pcg, pointerAnalysis,
        DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countPutfields(slice));
  }
  @Test
  public void testTestThrowCatch() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTTHROWCATCH);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, countApplicationAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, countThrows(slice));
    Assert.assertEquals(slice.toString(), 1, countGetfields(slice));
  }

  @Test
  public void testTestMessageFormat() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTMESSAGEFORMAT);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);
    Statement seed = new NormalStatement(main, 2);

    System.err.println("Statement: " + seed);
    // compute a backwards thin slice
    ThinSlicer ts = new ThinSlicer(cg, builder.getPointerAnalysis());
    Collection<Statement> slice = ts.computeBackwardThinSlice(seed);
    dumpSlice(slice);

  }

  /**
   * test for bug reported on mailing list by Joshua Garcia, 5/16/2010
   */
  @Test
  public void testTestInetAddr() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException, UnsoundGraphException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTINETADDR);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    SDG<?> sdg = new SDG<>(cg, builder.getPointerAnalysis(), DataDependenceOptions.NO_BASE_NO_HEAP, ControlDependenceOptions.FULL);
    GraphIntegrity.check(sdg);
  }

  @Test
  public void testJustThrow() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_JUSTTHROW);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, DataDependenceOptions.FULL,
        ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    dumpSlice(slice);
  }

  public static int countAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countApplicationAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
          if (scope.isApplicationLoader(s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countThrows(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractThrowInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countAloads(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAArrayLoadInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        count++;
      }
    }
    return count;
  }

  public static int countApplicationNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
        if (scope.isApplicationLoader(s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
          count++;
        }
      }
    }
    return count;
  }
  public static int countConditionals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAConditionalBranchInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countInvokes(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractInvokeInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countPutfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countReturns(Collection<Statement> slice) {
    int count = 0;
    for (Statement s: slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAReturnInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countGetfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countPutstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countGetstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static void dumpSlice(Collection<Statement> slice) {
    dumpSlice(slice, new PrintWriter(System.err));
  }

  public static void dumpSlice(Collection<Statement> slice, PrintWriter w) {
    w.println("SLICE:\n");
    int i = 1;
    for (Statement s : slice) {
      String line = (i++) + "   " + s;
      w.println(line);
      w.flush();
    }
  }

  public static void dumpSliceToFile(Collection<Statement> slice, String fileName) throws FileNotFoundException {
    File f = new File(fileName);
    FileOutputStream fo = new FileOutputStream(f);
    try (final PrintWriter w = new PrintWriter(fo)) {
      dumpSlice(slice, w);
    }
  }

  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    return findMethod(cg, d, name);
  }

  /**
   * @param cg
   * @param d
   * @param name
   */
  private static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
    for (CGNode n : Iterator2Iterable.make(cg.getSuccNodes(cg.getFakeRootNode()))) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    // if it's not a successor of fake root, just iterate over everything
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }

  public static CGNode findMethod(CallGraph cg, String name) {
    Atom a = Atom.findOrCreateUnicodeAtom(name);
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(a)) {
        return n;
      }
    }
    System.err.println("call graph " + cg);
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }

  public static Statement findCallTo(CGNode n, String methodName) {
    IR ir = n.getIR();
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction) s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
          Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
          return new NormalStatement(n, indices.intIterator().next());
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }

  public static Statement findFirstAllocation(CGNode n) {
    IR ir = n.getIR();
    for (int i = 0; i < ir.getInstructions().length; i++) {
      SSAInstruction s = ir.getInstructions()[i];
      if (s instanceof SSANewInstruction) {
        return new NormalStatement(n, i);
      }
    }
    Assertions.UNREACHABLE("failed to find allocation in " + n);
    return null;
  }

  private static Statement findCallToDoNothing(CGNode n) {
    return findCallTo(n, "doNothing");
  }
}
