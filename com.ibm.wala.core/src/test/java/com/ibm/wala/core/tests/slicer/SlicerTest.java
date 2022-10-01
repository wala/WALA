/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.slicer;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.examples.drivers.PDFSlice;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapReturnCaller;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.SlicerUtil;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class SlicerTest {

  private static AnalysisScope cachedScope;

  private static String makeSlicerExclusions() {
    try {
      try (InputStream is =
          new FileProvider()
              .getInputStreamFromClassLoader(
                  CallGraphTestUtil.REGRESSION_EXCLUSIONS, SlicerTest.class.getClassLoader())) {
        String exclusions = new String(FileUtil.readBytes(is), "UTF-8");
        // we also need to exclude java.security to avoid blowup during slicing
        return exclusions + "java\\/security\\/.*\n";
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static AnalysisScope findOrCreateAnalysisScope() throws IOException {
    if (cachedScope == null) {
      cachedScope =
          AnalysisScopeReader.instance.readJavaScope(
              TestConstants.WALA_TESTDATA, null, SlicerTest.class.getClassLoader());
      cachedScope.setExclusions(
          new FileOfClasses(new ByteArrayInputStream(makeSlicerExclusions().getBytes("UTF-8"))));
    }
    return cachedScope;
  }

  private static IClassHierarchy cachedCHA;

  private static IClassHierarchy findOrCreateCHA(AnalysisScope scope)
      throws ClassHierarchyException {
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

  public static Statement findCallToDoNothing(CGNode n) {
    return SlicerUtil.findCallTo(n, "doNothing");
  }

  @Test
  public void testSlice1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE1_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = SlicerUtil.findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> computeBackwardSlice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    Collection<Statement> slice = computeBackwardSlice;
    SlicerUtil.dumpSlice(slice);

    int i = 0;
    for (Statement st : slice) {
      if (st.getNode()
          .getMethod()
          .getDeclaringClass()
          .getClassLoader()
          .getReference()
          .equals(ClassLoaderReference.Application)) {
        i++;
      }
    }
    Assert.assertEquals(16, i);
  }

  @Test
  public void testSlice2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE2_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMethod(cg, "baz");

    Statement s = SlicerUtil.findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> computeBackwardSlice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    Collection<Statement> slice = computeBackwardSlice;
    SlicerUtil.dumpSlice(slice);

    Assert.assertEquals(slice.toString(), 9, SlicerUtil.countNormals(slice));
  }

  @Test
  public void testSlice3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();
    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE3_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMethod(cg, "main");

    Statement s = SlicerUtil.findCallTo(main, "doNothing");
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countAllocations(slice));
  }

  @Test
  public void testSlice4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE4_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);
    Statement s = SlicerUtil.findCallTo(main, "foo");
    s = PDFSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 4, slice.size());
  }

  @Test
  public void testSlice5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE5_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode n = CallGraphSearchUtil.findMethod(cg, "baz");
    Statement s = SlicerUtil.findCallTo(n, "foo");
    s = PDFSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 7, slice.size());
  }

  /** test unreproduced bug reported on mailing list by Sameer Madan, 7/3/2007 */
  @Test
  public void testSlice7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE7_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);
    Statement s = SlicerUtil.findFirstAllocation(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
  }

  /** test bug reported on mailing list by Ravi Chandhran, 4/16/2010 */
  @Test
  public void testSlice8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE8_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode process =
        CallGraphSearchUtil.findMethod(
            cg, Descriptor.findOrCreateUTF8("()V"), Atom.findOrCreateUnicodeAtom("process"));
    Statement s = findCallToDoNothing(process);
    System.err.println("Statement: " + s);
    // compute a backward slice, with data dependence and no exceptional control dependence
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(4, SlicerUtil.countInvokes(slice));
    // should only get 4 statements total when ignoring control dependences completely
    slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    Assert.assertEquals(slice.toString(), 4, slice.size());
  }

  @Test
  public void testSlice9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE9_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);
    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a backward slice, with data dependence and no exceptional control dependence
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    // dumpSlice(slice);
    Assert.assertEquals(/*slice.toString(), */ 5, SlicerUtil.countApplicationNormals(slice));
  }

  @Test
  public void testTestCD1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countConditionals(slice));
  }

  @Test
  public void testTestCD2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD2);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countConditionals(slice));
  }

  @Test
  public void testTestCD3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD3);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 0, SlicerUtil.countConditionals(slice));
  }

  @Test
  public void testTestCD4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD4);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(0, SlicerUtil.countConditionals(slice));

    // compute a full slice
    slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countConditionals(slice));
  }

  @Test
  public void testTestCD5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD5);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.NONE,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(10, slice.size());
    Assert.assertEquals(3, SlicerUtil.countReturns(slice));
  }

  @Test
  public void testTestCD5NoInterproc()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD5);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.NONE,
            ControlDependenceOptions.NO_INTERPROC_NO_EXCEPTION);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(8, slice.size());
    Assert.assertEquals(2, SlicerUtil.countReturns(slice));
  }

  @Test
  public void testTestCD6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTCD6);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = new MethodEntryStatement(main);
    System.err.println("Statement: " + s);

    // compute a no-data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeForwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.NONE,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countInvokes(slice));
  }

  @Test
  public void testTestId()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, TestConstants.SLICE_TESTID);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countAllocations(slice));
  }

  @Test
  public void testTestArrays()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTARRAYS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countAloads(slice));
  }

  @Test
  public void testTestFields()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTFIELDS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countPutfields(slice));
  }

  @Test
  public void testThin1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTTHIN1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute normal data slice
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(3, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(2, SlicerUtil.countPutfields(slice));

    // compute thin slice .. ignore base pointers
    Collection<Statement> computeBackwardSlice =
        Slicer.computeBackwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.NO_BASE_PTRS,
            ControlDependenceOptions.NONE);
    slice = computeBackwardSlice;
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countPutfields(slice));
  }

  @Test
  public void testTestGlobal()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTGLOBAL);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countPutstatics(slice));
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countGetstatics(slice));
  }

  @Test
  public void testTestMultiTarget()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTMULTITARGET);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countAllocations(slice));
  }

  @Test
  public void testTestRecursion()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTRECURSION);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 3, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 2, SlicerUtil.countPutfields(slice));
  }

  @Test
  public void testPrimGetterSetter()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TEST_PRIM_GETTER_SETTER);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode test = CallGraphSearchUtil.findMethod(cg, "test");

    PartialCallGraph pcg = PartialCallGraph.make(cg, Collections.singleton(test));

    Statement s = findCallToDoNothing(test);
    System.err.println("Statement: " + s);

    // compute full slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, pcg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 0, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countPutfields(slice));
  }

  /**
   * Test of using N-CFA builder to distinguish receiver objects for two calls to a getter method.
   * Also tests disabling SMUSH_PRIMITIVE_HOLDERS to ensure we get distinct abstract objects for two
   * different primitive holders.
   */
  @Test
  public void testPrimGetterSetter2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TEST_PRIM_GETTER_SETTER2);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    SSAPropagationCallGraphBuilder builder =
        new nCFABuilder(
            1,
            Language.JAVA.getFakeRootMethod(cha, options, cache),
            options,
            cache,
            appSelector,
            appInterpreter);
    // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
    // NOTE: we disable ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS for this test, since IntWrapper
    // is a primitive holder
    builder.setInstanceKeys(
        new ZeroXInstanceKeys(
            options,
            cha,
            builder.getContextInterpreter(),
            ZeroXInstanceKeys.ALLOCATIONS
                | ZeroXInstanceKeys.SMUSH_MANY /* | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS */
                | ZeroXInstanceKeys.SMUSH_STRINGS
                | ZeroXInstanceKeys.SMUSH_THROWABLES));

    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode test = CallGraphSearchUtil.findMainMethod(cg);

    PartialCallGraph pcg = PartialCallGraph.make(cg, Collections.singleton(test));

    Statement s = findCallToDoNothing(test);
    System.err.println("Statement: " + s);

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, pcg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countPutfields(slice));
  }

  @Test
  public void testTestThrowCatch()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTTHROWCATCH);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
    SlicerUtil.dumpSlice(slice);
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countApplicationAllocations(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countThrows(slice));
    Assert.assertEquals(slice.toString(), 1, SlicerUtil.countGetfields(slice));
  }

  @Test
  public void testTestMessageFormat()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTMESSAGEFORMAT);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);
    Statement seed = new NormalStatement(main, 2);

    System.err.println("Statement: " + seed);
    // compute a backwards thin slice
    ThinSlicer ts = new ThinSlicer(cg, builder.getPointerAnalysis());
    Collection<Statement> slice = ts.computeBackwardThinSlice(seed);
    SlicerUtil.dumpSlice(slice);
  }

  /** test for bug reported on mailing list by Joshua Garcia, 5/16/2010 */
  @Test
  public void testTestInetAddr()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException,
          UnsoundGraphException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_TESTINETADDR);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    SDG<?> sdg =
        new SDG<>(
            cg,
            builder.getPointerAnalysis(),
            DataDependenceOptions.NO_BASE_NO_HEAP,
            ControlDependenceOptions.FULL);
    GraphIntegrity.check(sdg);
  }

  @Test
  public void testJustThrow()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.SLICE_JUSTTHROW);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s,
            cg,
            pointerAnalysis,
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    SlicerUtil.dumpSlice(slice);
  }

  @Test
  public void testList()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lslice/TestList");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    NormalStatement getCall = (NormalStatement) SlicerUtil.findCallTo(main, "get");
    // we need a NormalReturnCaller statement to slice from the return value
    NormalReturnCaller nrc = new NormalReturnCaller(main, getCall.getInstructionIndex());

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            nrc,
            cg,
            pointerAnalysis,
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    List<Statement> normalsInMain =
        slice.stream()
            .filter(s -> s instanceof NormalStatement && s.getNode().equals(main))
            .collect(Collectors.toList());
    normalsInMain.stream().forEach(System.err::println);
    Assert.assertEquals(7, normalsInMain.size());
  }

  @Test
  public void testListIterator()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lslice/TestListIterator");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    NormalStatement getCall = (NormalStatement) SlicerUtil.findCallTo(main, "hasNext");
    // we need a NormalReturnCaller statement to slice from the return value
    NormalReturnCaller nrc = new NormalReturnCaller(main, getCall.getInstructionIndex());

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            nrc,
            cg,
            pointerAnalysis,
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NO_INTERPROC_NO_EXCEPTION);
    List<Statement> inMain =
        slice.stream().filter(s -> s.getNode().equals(main)).collect(Collectors.toList());
    // check that we are tracking the size field in a HeapReturnCaller statement for the add() call
    Assert.assertTrue(
        "couldn't find HeapReturnCaller for size field",
        inMain.stream()
            .filter(
                st -> {
                  if (st instanceof HeapReturnCaller) {
                    HeapReturnCaller hrc = (HeapReturnCaller) st;
                    if (hrc.getCall().getDeclaredTarget().getName().toString().equals("add")) {
                      PointerKey location = hrc.getLocation();
                      if (location instanceof InstanceFieldKey) {
                        InstanceFieldKey ifk = (InstanceFieldKey) location;
                        return ifk.getField().getName().toString().equals("size");
                      }
                    }
                  }
                  return false;
                })
            .findFirst()
            .isPresent());
  }

  @Test
  public void testIntegerValueOf()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = findOrCreateAnalysisScope();

    IClassHierarchy cha = findOrCreateCHA(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lslice/TestIntegerValueOf");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);

    CGNode main = CallGraphSearchUtil.findMainMethod(cg);

    Statement s = SlicerUtil.findCallTo(main, "doNothing");

    final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    Collection<Statement> slice =
        Slicer.computeBackwardSlice(
            s, cg, pointerAnalysis, DataDependenceOptions.NO_HEAP, ControlDependenceOptions.NONE);
    // SlicerUtil.dumpSlice(slice);
    List<Statement> inMain =
        slice.stream().filter(st -> st.getNode().equals(main)).collect(Collectors.toList());
    inMain.stream().forEach(System.err::println);
    Assert.assertEquals(4, inMain.size());
    // returns for Integer.valueOf() and getInt()
    Assert.assertEquals(2, inMain.stream().filter(st -> st instanceof NormalReturnCaller).count());
  }
}
