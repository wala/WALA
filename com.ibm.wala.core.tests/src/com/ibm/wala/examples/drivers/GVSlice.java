/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.drivers;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.ParamStatement.CallStatementCarrier;
import com.ibm.wala.ipa.slicer.ParamStatement.ValueNumberCarrier;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NodeDecorator;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.GVUtil;

/**
 * 
 * This simple example WALA application builds an SDG and fires off ghostview to
 * viz a DOT representation of a slice in the SDG
 * 
 * @author sfink
 */
public class GVSlice {

  private final static String PS_FILE = "slice.ps";

  /**
   * Usage: GVSDG -appJar [jar file name] -mainclass [main class] -src [method
   * name]
   * 
   * The "jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   */
  public static void main(String[] args) {
    run(args);
  }

  /**
   */
  public static Process run(String[] args) {
    Properties p = CommandLine.parse(args);
    validateCommandLine(p);
    return run(p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"),
        goBackward(p), GVSDG.getDataDependenceOptions(p), GVSDG.getControlDependenceOptions(p));
  }

  private static boolean goBackward(Properties p) {
    return !p.getProperty("dir","backward").equals("forward");
  }

  /**
   */
  public static Process run(String appJar, String mainClass, String srcCaller, String srcCallee, boolean goBackward, DataDependenceOptions dOptions,
      ControlDependenceOptions cOptions) {
    try {
      EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

      // generate a WALA-consumable wrapper around the incoming scope object
      EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
      WarningSet warnings = new WarningSet();
      ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
      Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
      AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

      CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
      CallGraph cg = builder.makeCallGraph(options);
      SDG sdg = new SDG(cg, builder.getPointerAnalysis(), dOptions, cOptions);

      CGNode main = SlicerTest.findMethod(cg, srcCaller);
      Statement s = SlicerTest.findCallTo(main, srcCallee);
      System.err.println("Statement: " + s);
      Collection<Statement> slice = null;
      if (goBackward) { 
        slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      } else {
        // for forward slices ... we actually slice from the return value of calls.
        s = getReturnStatementForCall(s);
        slice = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      }
      SlicerTest.dumpSlice(slice);

      Graph<Statement> g = pruneSDG(sdg, slice);
      try {
        GraphIntegrity.check(g);
      } catch (UnsoundGraphException e1) {
        e1.printStackTrace();
        Assertions.UNREACHABLE();
      }
      Assertions.productionAssertion(g.getNumberOfNodes() == slice.size(), "panic " + g.getNumberOfNodes() + " " + slice.size());

      Properties p = null;
      try {
        p = WalaExamplesProperties.loadProperties();
        p.putAll(WalaProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PS_FILE;

      String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);

      DotUtil.dotify(g, makeNodeDecorator(), GVTypeHierarchy.DOT_FILE, psFile, dotExe);

      String gvExe = p.getProperty(WalaExamplesProperties.GHOSTVIEW_EXE);
      return GVUtil.launchGV(psFile, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  /**
   * If s is a call statement, return the statement representing the normal return from s
   */
  public static Statement getReturnStatementForCall(Statement s) {
    if (s.getKind() == Kind.NORMAL) {
      SSAInstruction st = ((NormalStatement)s).getInstruction();
      if (st instanceof SSAInvokeInstruction) {
        return new ParamStatement.NormalReturnCaller(s.getNode(),(SSAInvokeInstruction)st);
      } else {
        return s;
      }
    } else {
      return s;
    }
  }

  public static Graph<Statement> pruneSDG(SDG sdg, final Collection<Statement> slice) {
    Filter f = new Filter() {
      public boolean accepts(Object o) {
        return slice.contains(o);
      }
    };
    return GraphSlicer.prune(sdg, f);
  }

  public static NodeDecorator makeNodeDecorator() {
    return new NodeDecorator() {
      public String getLabel(Object o) throws WalaException {
        Statement s = (Statement) o;
        switch (s.getKind()) {
        case HEAP_PARAM_CALLEE:
        case HEAP_PARAM_CALLER:
        case HEAP_RET_CALLEE:
        case HEAP_RET_CALLER:
          HeapStatement h = (HeapStatement) s;
          return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
        case NORMAL:
          NormalStatement n = (NormalStatement) s;
          return n.getNode() + "\\n" + n.getInstruction();
        case PARAM_CALLEE:
        case PARAM_CALLER:
          if (s instanceof ValueNumberCarrier) {
            ValueNumberCarrier vc = (ValueNumberCarrier) s;
            if (s instanceof CallStatementCarrier) {
              CallStatementCarrier cc = (CallStatementCarrier) s;
              return s.getKind() + "\\n" + s.getNode() + "\\n" + cc.getCall() + "\\nv" + vc.getValueNumber();
            } else {
              return s.getKind() + "\\n" + s.getNode() + "\\nv" + vc.getValueNumber();
            }
          } else {
            if (s instanceof CallStatementCarrier) {
              CallStatementCarrier cc = (CallStatementCarrier) s;
              return s.getKind() + "\\n" + s.getNode() + "\\n" + cc.getCall();
            } else {
              return s.toString();
            }
          }
        case EXC_RET_CALLEE:
        case EXC_RET_CALLER:
        case NORMAL_RET_CALLEE:
        case NORMAL_RET_CALLER:
        case PHI:
        default:
          return s.toString();
        }
      }

    };
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li> args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar"
   * <li> args[2] : "-mainClass"
   * <li> args[3] : something like "Lslice/TestRecursion" *
   * <li> args[4] : "-srcCallee"
   * <li> args[5] : something like "print" *
   * <li> args[4] : "-srcCaller"
   * <li> args[5] : something like "main"
   * 
   * @throws UnsupportedOperationException
   *           if command-line is malformed.
   */
  static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
    if (p.get("mainClass") == null) {
      throw new UnsupportedOperationException("expected command-line to include -mainClass");
    }
    if (p.get("srcCallee") == null) {
      throw new UnsupportedOperationException("expected command-line to include -srcCallee");
    }
    if (p.get("srcCaller") == null) {
      throw new UnsupportedOperationException("expected command-line to include -srcCaller");
    }
  }
}