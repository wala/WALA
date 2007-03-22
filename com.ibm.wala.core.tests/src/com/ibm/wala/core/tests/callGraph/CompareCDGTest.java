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

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.cdg.BVControlDependenceGraph;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;
import com.ibm.wala.viz.DotUtil;

/**
 * @author Mangala Gowri Nanda (minor hacks by Julian Dolby (dolby@us.ibm.com)
 *         to fit into domo junit test framework)
 * 
 */
public class CompareCDGTest extends WalaTestCase {

  /**
   * Usage: CompareCDGTest <jar file name>
   * 
   * The "jar file name" should be something like "/tmp/testdata/java_cup.jar"
   * 
   * @param args
   */
  public static void main(String[] args) {
    run(args);
  }

  /**
   * Usage: args = "-appJar [jar file name] " The "jar file name" should be
   * something like "/tmp/testdata/java_cup.jar"
   * 
   * @param args
   */
  public static void run(String[] args) {
    try {
      run(buildCallGraphCommandLine(args[0]));
    } catch (WalaException e) {
      Trace.println(e);
    }
  }

  public static void run(CallGraph g) {
    compareCDGs(g);
    Trace.println(CallGraphStats.getStats(g));
  }

  private static void compareCDGs(CallGraph g) {
    long cdgTime = 0;
    long bvTime = 0;
    String dotExe = "dot";
    for (Iterator<? extends CGNode> it = g.iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      MethodReference mref = n.getMethod().getReference();
      Trace.println(mref.toString());
      // if(app.equals("Application"))
      {
        IR ir = g.getInterpreter(n).getIR(n, new WarningSet());
        if (ir != null) {
          SSACFG cfg = ir.getControlFlowGraph();
          long startTime = System.currentTimeMillis();
          ControlDependenceGraph cdg = new ControlDependenceGraph(cfg, true);
          long diff = System.currentTimeMillis() - startTime;
          cdgTime += diff;

          startTime = System.currentTimeMillis();
          BVControlDependenceGraph bvcdg = new BVControlDependenceGraph(cfg);
          diff = System.currentTimeMillis() - startTime;
          bvTime += diff;

          if (!compatible(cfg, cdg, bvcdg)) {
            Trace.println("\tMISMATCH!!");
            Vector<BasicBlock> vec = checkCFG(cfg, mref);
            if (vec != null) {
              Trace.println(mref + " has " + vec.size() + " blocks with no predecessors");
            }

            Trace.println("\nControlDependenceGraph output::");
            dumpCDGInfo(cfg, cdg);
            Trace.println("\nBitVector ControlDependenceGraph output::");
            dumpCDGInfo(cfg, bvcdg);
            Trace.println("");

            String dotFile = "tmp.dot";
            String psFile = mref.getName() + ".cdg.ps";
            try {
              DotUtil.dotify(cdg, null, dotFile, psFile, dotExe);
            } catch (WalaException e) {
              e.printStackTrace();
            }

            psFile = mref.getName() + ".bv.ps";
            try {
              DotUtil.dotify(bvcdg, null, dotFile, psFile, dotExe);
            } catch (WalaException e) {
              e.printStackTrace();
            }

            Assertions.UNREACHABLE();
          }
        }
      }
    }
    Trace.println("Time to compute ControlDependenceGraph=" + cdgTime);
    Trace.println("Time to compute BVControlDependenceGraph=" + bvTime);
  }

  private static boolean compatible(SSACFG cfg, ControlDependenceGraph cdg, BVControlDependenceGraph bv) {
    boolean ret = true;
    for (Iterator<? extends IBasicBlock> it = cfg.iterator(); it.hasNext();) {
      SSACFG.BasicBlock ibb = (SSACFG.BasicBlock) it.next();
      int cCount = cdg.getPredNodeCount(ibb);
      int bCount = bv.getPredNodeCount(ibb);
      if (cCount != bCount) {
        Trace.println("\tPred Count mismatch for " + ibb);
        ret = false;
      }
      Iterator<? extends IBasicBlock> cdit = cdg.getPredNodes(ibb);
      while (cdit.hasNext()) {
        SSACFG.BasicBlock cdgbb = (SSACFG.BasicBlock) cdit.next();
        if (!bv.hasEdge(cdgbb, ibb)) {
          Trace.println("\tPred " + cdgbb + " mismatch for " + ibb);
          ret = false;
        }
        Set<Object> clabels = cdg.getEdgeLabels(cdgbb, ibb);
        Set<? extends Object> blabels = bv.getEdgeLabels(cdgbb, ibb);
        Iterator<Object> labit = clabels.iterator();
        while (labit.hasNext()) {
          Object lab = labit.next();
          if (!blabels.contains(lab)) {
            Trace.println("\tLabel " + lab + " missing at " + cdgbb + " --> " + ibb);
            ret = false;
          }
        }
      }

      cCount = cdg.getSuccNodeCount(ibb);
      bCount = bv.getSuccNodeCount(ibb);
      if (cCount != bCount) {
        Trace.println("\tSucc Count mismatch for " + ibb);
        ret = false;
      }
      cdit = cdg.getSuccNodes(ibb);
      while (cdit.hasNext()) {
        SSACFG.BasicBlock cdgbb = (SSACFG.BasicBlock) cdit.next();
        if (!bv.hasEdge(ibb, cdgbb)) {
          Trace.println("\tSucc " + cdgbb + " mismatch for " + ibb);
          ret = false;
        }
        Set<Object> clabels = cdg.getEdgeLabels(ibb, cdgbb);
        Set<? extends Object> blabels = bv.getEdgeLabels(ibb, cdgbb);
        Iterator<Object> labit = clabels.iterator();
        while (labit.hasNext()) {
          Object lab = labit.next();
          if (!blabels.contains(lab)) {
            Trace.println("\tLabel " + lab + " missing at " + ibb + " --> " + cdgbb);
            ret = false;
          }
        }
      }

    }
    return ret;
  }

  private static void dumpCDGInfo(SSACFG cfg, ControlDependenceGraph cdg) {
    Trace.println("{\n");
    Vector<SSACFG.BasicBlock> seen = new Vector<SSACFG.BasicBlock>();
    SSACFG.BasicBlock entry = (SSACFG.BasicBlock) cfg.entry();
    Vector<SSACFG.BasicBlock> worklist = new Vector<SSACFG.BasicBlock>();
    worklist.add(entry);
    while (worklist.size() > 0) {
      SSACFG.BasicBlock ibb = worklist.remove(worklist.size() - 1);
      if (seen.contains(ibb))
        continue;
      seen.add(ibb);

      int number = ibb.getNumber();
      Trace.print("\n\tBB ID:" + number + "::" + ibb + " (CD=");
      Iterator<? extends IBasicBlock> cdit = cdg.getPredNodes(ibb);
      while (cdit.hasNext()) {
        SSACFG.BasicBlock cdgbb = (SSACFG.BasicBlock) cdit.next();
        Trace.print(cdgbb.getNumber() + "[");
        Set<Object> labels = cdg.getEdgeLabels(cdgbb, ibb);
        Iterator<Object> labit = labels.iterator();
        while (labit.hasNext()) {
          Object lab = labit.next();
          Trace.print(lab + ",");
        }
        Trace.print("] ");
      }
      Trace.print(")");

      Iterator<? extends IBasicBlock> succ = cfg.getSuccNodes(ibb);
      Trace.print(" (SUCC=");
      while (succ.hasNext()) {
        SSACFG.BasicBlock isc = (SSACFG.BasicBlock) succ.next();
        Trace.print(isc.getNumber() + ", ");
        worklist.add(isc);
      }
      Trace.print(")");

      Iterator<? extends IBasicBlock> pred = cfg.getPredNodes(ibb);
      Trace.print(" (PRED=");
      while (pred.hasNext()) {
        SSACFG.BasicBlock isc = (SSACFG.BasicBlock) pred.next();
        Trace.print(isc.getNumber() + ", ");
      }
      Trace.println(") {");

      Iterator<? extends IInstruction> it = ibb.iterator();
      int j = 0;
      while (it.hasNext()) {
        SSAInstruction inst = (SSAInstruction) it.next();
        if (inst != null) {
          Trace.println("\t\t" + j++ + ":" + inst.toString());
        }
      }
      Trace.println("\t}\n");
    }
    Trace.println("}\n");
  }

  private static void dumpCDGInfo(SSACFG cfg, BVControlDependenceGraph cdg) {
    Trace.println("{\n");
    Vector<SSACFG.BasicBlock> seen = new Vector<SSACFG.BasicBlock>();
    SSACFG.BasicBlock entry = (SSACFG.BasicBlock) cfg.entry();
    Vector<SSACFG.BasicBlock> worklist = new Vector<SSACFG.BasicBlock>();
    worklist.add(entry);
    while (worklist.size() > 0) {
      SSACFG.BasicBlock ibb = worklist.remove(worklist.size() - 1);
      if (seen.contains(ibb))
        continue;
      seen.add(ibb);

      int number = ibb.getNumber();
      Trace.print("\n\tBB ID:" + number + "::" + ibb + " (CD=");
      Iterator<? extends IBasicBlock> cdit = cdg.getPredNodes(ibb);
      while (cdit.hasNext()) {
        SSACFG.BasicBlock cdgbb = (SSACFG.BasicBlock) cdit.next();
        Trace.print(cdgbb.getNumber() + "[");
        Set<? extends Object> labels = cdg.getEdgeLabels(cdgbb, ibb);
        Iterator<? extends Object> labit = labels.iterator();
        while (labit.hasNext()) {
          Object lab = labit.next();
          Trace.print(lab + ",");
        }
        Trace.print("] ");
      }
      Trace.print(")");

      Iterator<? extends IBasicBlock> succ = cfg.getSuccNodes(ibb);
      Trace.print(" (SUCC=");
      while (succ.hasNext()) {
        SSACFG.BasicBlock isc = (SSACFG.BasicBlock) succ.next();
        Trace.print(isc.getNumber() + ", ");
        worklist.add(isc);
      }
      Trace.print(")");

      Iterator<IBasicBlock> pred = cfg.getPredNodes(ibb);
      Trace.print(" (PRED=");
      while (pred.hasNext()) {
        SSACFG.BasicBlock isc = (SSACFG.BasicBlock) pred.next();
        Trace.print(isc.getNumber() + ", ");
      }
      Trace.println(") {");

      Iterator<? extends IInstruction> it = ibb.iterator();
      int j = 0;
      while (it.hasNext()) {
        SSAInstruction inst = (SSAInstruction) it.next();
        if (inst != null) {
          Trace.println("\t\t" + j++ + ":" + inst.toString());
        }
      }
      Trace.println("\t}\n");
    }
    Trace.println("}\n");
  }

  /*
   * private static void buildUncheckedCDGs(CallGraph g) { String dotExe =
   * "dot"; int count = 0; for (Iterator it = g.iterateNodes(); it.hasNext();) {
   * count++; CGNode n = (CGNode) it.next(); MethodReference mref =
   * n.getMethod().getReference(); IR ir = ((SSAContextInterpreter)
   * g.getInterpreter(n)).getIR(n, new WarningSet()); if (ir != null) { SSACFG
   * cfg = ir.getControlFlowGraph(); Vector vec = checkCFG(cfg, mref); if ( vec !=
   * null ) { // Trace.println(mref.toString()); ControlDependenceGraph cdg =
   * new ControlDependenceGraph(cfg,true); boolean found = false; for ( int i=0;
   * i<vec.size() ; i++ ) { Object o = vec.get(i); if (
   * cdg.getSuccNodeCount(o)>0 ) { found = true; break; } } if ( !found )
   * continue;
   * 
   * Trace.println(mref+" has "+vec.size()+ " blocks with no predecessors"); for (
   * int i=0 ; i<vec.size() ; i++ ) { SSACFG.BasicBlock bb =
   * (SSACFG.BasicBlock)vec.get(i); Trace.println("\t"+dumpIBB(bb, cfg)); }
   * 
   * String dotFile = "tmp.dot"; String psFile = ""+mref.getName()+count+".ps";
   * try { DotUtil.dotify(cdg, null, dotFile, psFile, dotExe); } catch
   * (WalaException e) { e.printStackTrace(); } } } } }
   */
  private static Vector<BasicBlock> checkCFG(SSACFG cfg, MethodReference mref) {
    Vector<SSACFG.BasicBlock> vec = new Vector<SSACFG.BasicBlock>();
    for (Iterator<? extends IBasicBlock> it = cfg.iterator(); it.hasNext();) {
      SSACFG.BasicBlock bb = (SSACFG.BasicBlock) it.next();
      if (cfg.getPredNodeCount(bb) == 0)
        vec.add(bb);
    }
    if (vec.size() > 1) {
      /*
       * Trace.println(mref+" has more than one block with no predecessors");
       * for ( int i=0 ; i<vec.size() ; i++ ) { SSACFG.BasicBlock bb =
       * (SSACFG.BasicBlock)vec.get(i); Trace.println("\t"+dumpIBB(bb, cfg)); }
       */
      return vec;
    }
    return null;
  }

  /*
   * private static String dumpIBB ( SSACFG.BasicBlock ibb, SSACFG ssaCfg ) {
   * int number = ibb.getNumber(); String ret = ""; ret += "BB ID:"+number +
   * "::" + ibb; Iterator succ = ssaCfg.getSuccNodes(ibb); ret += " (SUCC=";
   * while (succ.hasNext()) { SSACFG.BasicBlock isc = (SSACFG.BasicBlock)
   * succ.next(); ret += "" + isc.getNumber()+", "; } ret += ")";
   * 
   * Iterator pred = ssaCfg.getPredNodes(ibb); ret += " (PRED="; while
   * (pred.hasNext()) { SSACFG.BasicBlock isc = (SSACFG.BasicBlock) pred.next();
   * ret += "" + isc.getNumber()+", "; } ret += ") {\n";
   * 
   * Iterator it = ibb.iterateAllInstructions(); int j = 0; while (it.hasNext()) {
   * SSAInstruction inst = (SSAInstruction) it.next(); if ( inst != null ) { ret +=
   * "\t\t"+ j++ + ":" + inst.toString() + "\n"; } } ret += "\t}\n"; return ret; }
   */

  /**
   * @param appJar
   *          something like "c:/temp/testdata/java_cup.jar"
   * @return a call graph
   * @throws WalaException
   * @throws ClassHierarchyException 
   */
  public static CallGraph buildCallGraphCommandLine(String appJar) throws WalaException, ClassHierarchyException {
    EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

    // generate a DOMO-consumable wrapper around the incoming scope object
    EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);

    // TODO: return the warning set
    // invoke DOMO to build a DOMO class hierarchy object
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);

    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // //
    // build the call graph
    // //
    CFABuilder builder = Util.makeZeroCFABuilder(options, cha, scope, warnings, null, null);
    CallGraph cg = builder.makeCallGraph(options);
    return cg;

  }

  public void testJavaCup() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.JAVA_CUP);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.JAVA_CUP_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    run(CallGraphTestUtil.buildZeroCFA(options, cha, scope, warnings));
  }

  public void testBcelVerifier() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.BCEL);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util
        .makeMainEntrypoints(scope, cha, TestConstants.BCEL_VERIFIER_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    run(CallGraphTestUtil.buildZeroCFA(options, cha, scope, warnings));
  }

  public void testJLex() throws ClassHierarchyException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.JLEX);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.JLEX_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    run(CallGraphTestUtil.buildZeroCFA(options, cha, scope, warnings));
  }
}
