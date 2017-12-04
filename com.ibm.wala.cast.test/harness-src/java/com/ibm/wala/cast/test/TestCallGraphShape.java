/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.test;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;

public abstract class TestCallGraphShape extends WalaTestCase {

  protected void verifyCFGAssertions(CallGraph CG, Object[][] assertionData) {
    for(Object[] dat : assertionData) {
      String function = (String) dat[0];
      for(CGNode N : getNodes(CG, function)) {
        int[][] edges = (int[][]) dat[1];
        SSACFG cfg = N.getIR().getControlFlowGraph();
        for (int i = 0; i < edges.length; i++) {
          SSACFG.BasicBlock bb = cfg.getNode(i);
          Assert.assertEquals("basic block " + i, edges[i].length, cfg.getSuccNodeCount(bb));
          for (int j = 0; j < edges[i].length; j++) {
            Assert.assertTrue(cfg.hasEdge(bb, cfg.getNode(edges[i][j])));
          }
        }
      }
    }
  }
  
  protected void verifySourceAssertions(CallGraph CG, Object[][] assertionData) {
    for(Object[] dat : assertionData) {
      String function = (String) dat[0];
      for(CGNode N : getNodes(CG, function)) {
        if (N.getMethod() instanceof AstMethod) {
          AstMethod M = (AstMethod) N.getMethod();
          SSAInstruction[] insts = N.getIR().getInstructions();
          insts: for(int i = 0; i < insts.length; i++) {
            SSAInstruction inst = insts[i];
            if (inst != null) {
              Position pos = M.getSourcePosition(i);
              if (pos != null) {
                String fileName = pos.getURL().toString();
                if (fileName.lastIndexOf('/') >= 0) {
                  fileName = fileName.substring(fileName.lastIndexOf('/')+1);
                }
                for(int j = 0; j < assertionData.length; j++) {
                  String file = (String) assertionData[j][1];
                  if (file.indexOf('/') >= 0) {
                    file = file.substring(file.lastIndexOf('/') + 1);
                  }
                  if (file.equalsIgnoreCase(fileName)) {
                    if (pos.getFirstLine() >= (Integer) assertionData[j][2]
                                           &&
                        (pos.getLastLine() != -1? pos.getLastLine(): pos.getFirstLine()) <= (Integer) assertionData[j][3]) {
                      System.err.println("found " + inst + " of " + M + " at expected position " + pos);
                      continue insts;
                    }
                  }
                }

                Assert.assertTrue("unexpected location " + pos + " for " + inst + " of " + M + "\n" + N.getIR(), false);
              }
            }
          }
        }
      }
    }
  }
  
  protected static class Name {
    String name;

    int instructionIndex;

    int vn;

    public Name(int vn, int instructionIndex, String name) {
      this.vn = vn;
      this.name = name;
      this.instructionIndex = instructionIndex;
    }
  }

  protected void verifyNameAssertions(CallGraph CG, Object[][] assertionData) {
    for (Object[] element : assertionData) {
      Iterator<CGNode> NS = getNodes(CG, (String) element[0]).iterator();
      while (NS.hasNext()) {
        CGNode N = NS.next();
        IR ir = N.getIR();
        Name[] names = (Name[]) element[1];
        for (Name name : names) {

          System.err.println("looking for " + name.name + ", " + name.vn + " in " + N);

          String[] localNames = ir.getLocalNames(name.instructionIndex, name.vn);

          boolean found = false;
          for (String localName : localNames) {
            if (localName.equals(name.name)) {
              found = true;
            }
          }

          Assert.assertTrue("no name " + name.name + " for " + N + "\n" + ir, found);
        }
      }
    }
  }

  protected void verifyGraphAssertions(CallGraph CG, Object[][] assertionData) {
    // System.err.println(CG);

    if (assertionData == null) {
      return;
    }
    
    for (int i = 0; i < assertionData.length; i++) {

      check_target: for (int j = 0; j < ((String[]) assertionData[i][1]).length; j++) {
        Iterator<CGNode> srcs = (assertionData[i][0] instanceof String) ? getNodes(CG, (String) assertionData[i][0]).iterator()
            : new NonNullSingletonIterator<>(CG.getFakeRootNode());

        Assert.assertTrue("cannot find " + assertionData[i][0], srcs.hasNext());

        boolean checkAbsence = false;
        String targetName = ((String[]) assertionData[i][1])[j];
        if (targetName.startsWith("!")) {
          checkAbsence = true;
          targetName = targetName.substring(1);
        }

        while (srcs.hasNext()) {
          CGNode src = srcs.next();
          for (CallSiteReference sr : Iterator2Iterable.make(src.iterateCallSites())) {
           
            Iterator<CGNode> dsts = getNodes(CG, targetName).iterator();
            if (! checkAbsence) {
              Assert.assertTrue("cannot find " + targetName, dsts.hasNext());
            }
            
            while (dsts.hasNext()) {
              CGNode dst = dsts.next();
              for (CGNode cgNode : CG.getPossibleTargets(src, sr)) {
                if (cgNode.equals(dst)) {
                  if (checkAbsence) {
                    System.err.println(("found unexpected " + src + " --> " + dst + " at " + sr));
                    Assert.assertTrue("found edge " + assertionData[i][0] + " ---> " + targetName, false);
                  } else {
                    System.err.println(("found expected " + src + " --> " + dst + " at " + sr));
                    continue check_target;
                  }
                }
              }
            }
          }
        }

        System.err.println("cannot find edge " + assertionData[i][0] + " ---> " + targetName);
        Assert.assertTrue("cannot find edge " + assertionData[i][0] + " ---> " + targetName, checkAbsence);
      }
    }
  }


  /**
   * Verifies that none of the nodes that match the source description has an edge to any of the nodes that match the destination
   * description. (Used for checking for false connections in the callgraph)
   * 
   * @param CG
   * @param sourceDescription
   * @param destDescription
   */
  protected void verifyNoEdges(CallGraph CG, String sourceDescription, String destDescription) {
    Collection<CGNode> sources = getNodes(CG, sourceDescription);
    Collection<CGNode> dests = getNodes(CG, destDescription);
    for (Object source : sources) {
      for (Object dest : dests) {
        for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes((CGNode) source))) {
          if (n.equals(dest)) {
            Assert.fail("Found a link from " + source + " to " + dest);
          }
        }
      }
    }
  }
  
  protected static final Object ROOT = new Object() {
    @Override
    public String toString() {
      return "CallGraphRoot";
    }
  };

  protected abstract Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier);

}
