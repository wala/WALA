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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.examples.drivers.GVSlice;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.warnings.WarningSet;

public class SlicerTest extends TestCase {

  public void testSlice1() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE1_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);

    int i = 0;
    for(Iterator ss = slice.iterator(); ss.hasNext(); ) {
      Statement st = (Statement) ss.next();
      if (st.getNode().getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
	i++;
      }
    }
    assertEquals(14, i);
  }
  
  public void testSlice2() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE2_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMethod(cg, "baz");

    Statement s = findCallTo(main, "println");
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(30, slice.size());
  }
  
  public void testSlice3() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE3_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMethod(cg, "main");

    Statement s = findCallTo(main, "doNothing");
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(1, countAllocations(slice));
  }
  
  public void testSlice4() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE4_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);
    Statement s = findCallTo(main, "foo");
    s = GVSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(4, slice.size());
  }
  
  public void testSlice5() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE5_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode n = findMethod(cg, "baz");
    Statement s = findCallTo(n, "foo");
    s = GVSlice.getReturnStatementForCall(s);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(7, slice.size());
  }

  public void testTestCD1() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTCD1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    assertEquals(2, countConditionals(slice));
  }

  public void testTestCD2() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTCD2);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    assertEquals(1, countConditionals(slice));
  }

  public void testTestCD3() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTCD3);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.NONE,
        ControlDependenceOptions.FULL);
    dumpSlice(slice);
    assertEquals(0, countConditionals(slice));
  }

  public void testTestId() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTID);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(1, countAllocations(slice));
  }

  public void testTestArrays() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTARRAYS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(2, countAllocations(slice));
    assertEquals(1, countAloads(slice));
  }

  public void testTestFields() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTFIELDS);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(2, countAllocations(slice));
    assertEquals(1, countPutfields(slice));
  }

  public void testThin1() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTTHIN1);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute normal data slice
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(3, countAllocations(slice));
    assertEquals(2, countPutfields(slice));

    // compute thin slice .. ignore base pointers
    slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.NO_BASE_PTRS,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(2, countAllocations(slice));
    assertEquals(1, countPutfields(slice));
  }

  public void testTestGlobal() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.SLICE_TESTGLOBAL);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(1, countAllocations(slice));
    assertEquals(2, countPutstatics(slice));
    assertEquals(2, countGetstatics(slice));
  }

  public void testTestMultiTarget() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTMULTITARGET);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);
    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(2, countAllocations(slice));
  }

  public void testTestRecursion() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.SLICE_TESTRECURSION);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    CGNode main = findMainMethod(cg);

    Statement s = findCallToDoNothing(main);
    System.err.println("Statement: " + s);

    // compute a data slice
    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), DataDependenceOptions.FULL,
        ControlDependenceOptions.NONE);
    dumpSlice(slice);
    assertEquals(3, countAllocations(slice));
    assertEquals(2, countPutfields(slice));
  }

  private int countAllocations(Collection<Statement> slice) {
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

  private int countAloads(Collection<Statement> slice) {
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

  private int countConditionals(Collection<Statement> slice) {
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

  private int countPutfields(Collection<Statement> slice) {
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

  private int countPutstatics(Collection<Statement> slice) {
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

  private int countGetstatics(Collection<Statement> slice) {
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
    dumpSlice(slice,new PrintWriter(System.err));
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
    PrintWriter w = new PrintWriter(fo);
    dumpSlice(slice,w);
  }

  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
      CGNode n = it.next();
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find main() method");
    return null;
  }

  
  public static CGNode findMethod(CallGraph cg, String name) {
    Atom a = Atom.findOrCreateUnicodeAtom(name);
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      if (n.getMethod().getName().equals(a)) {
        return n;
      }
    }
    System.err.println("call graph " + cg);
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }
  
  public static Statement findCallTo(CGNode n, String methodName) {
    IR ir = n.getCallGraph().getInterpreter(n).getIR(n, new WarningSet());
    for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction)s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
          Assertions.productionAssertion(indices.size()== 1, "expected 1 but got " + indices.size());
          return new NormalStatement(n, indices.intIterator().next());
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }

  private static Statement findCallToDoNothing(CGNode n) {
    return findCallTo(n, "doNothing");
  }
}
