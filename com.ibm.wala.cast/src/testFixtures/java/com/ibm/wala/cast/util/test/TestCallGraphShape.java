/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.util.test;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import java.util.Collection;
import java.util.Iterator;

public abstract class TestCallGraphShape {

  public void verifyCFGAssertions(CallGraph CG, Object[][] assertionData) {
    for (Object[] dat : assertionData) {
      String function = (String) dat[0];
      for (CGNode N : getNodes(CG, function)) {
        int[][] edges = (int[][]) dat[1];
        SSACFG cfg = N.getIR().getControlFlowGraph();
        for (int i = 0; i < edges.length; i++) {
          SSACFG.BasicBlock bb = cfg.getNode(i);
          assert edges[i].length == cfg.getSuccNodeCount(bb) : "basic block " + i;
          for (int j = 0; j < edges[i].length; j++) {
            assert cfg.hasEdge(bb, cfg.getNode(edges[i][j]));
          }
        }
      }
    }
  }

  public void verifySourceAssertions(CallGraph CG, Object[][] assertionData) {
    for (Object[] dat : assertionData) {
      String function = (String) dat[0];
      for (CGNode N : getNodes(CG, function)) {
        if (N.getMethod() instanceof AstMethod) {
          AstMethod M = (AstMethod) N.getMethod();
          SSAInstruction[] insts = N.getIR().getInstructions();
          insts:
          for (int i = 0; i < insts.length; i++) {
            SSAInstruction inst = insts[i];
            if (inst != null) {
              Position pos = M.getSourcePosition(i);
              if (pos != null) {
                String fileName = pos.getURL().toString();
                if (fileName.lastIndexOf('/') >= 0) {
                  fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }
                for (Object[] assertionDatum : assertionData) {
                  String file = (String) assertionDatum[1];
                  if (file.indexOf('/') >= 0) {
                    file = file.substring(file.lastIndexOf('/') + 1);
                  }
                  if (file.equalsIgnoreCase(fileName)) {
                    if (pos.getFirstLine() >= (Integer) assertionDatum[2]
                        && (pos.getLastLine() != -1 ? pos.getLastLine() : pos.getFirstLine())
                            <= (Integer) assertionDatum[3]) {
                      System.err.println(
                          "found " + inst + " of " + M + " at expected position " + pos);
                      continue insts;
                    }
                  }
                }

                assert false
                    : "unexpected location " + pos + " for " + inst + " of " + M + "\n" + N.getIR();
              }
            }
          }
        }
      }
    }
  }

  public static class Name {
    String name;

    int instructionIndex;

    int vn;

    public Name(int vn, int instructionIndex, String name) {
      this.vn = vn;
      this.name = name;
      this.instructionIndex = instructionIndex;
    }
  }

  public void verifyNameAssertions(CallGraph CG, Object[][] assertionData) {
    for (Object[] element : assertionData) {
      for (CGNode N : getNodes(CG, (String) element[0])) {
        IR ir = N.getIR();
        Name[] names = (Name[]) element[1];
        for (Name name : names) {

          System.err.println("looking for " + name.name + ", " + name.vn + " in " + N);

          String[] localNames = ir.getLocalNames(name.instructionIndex, name.vn);

          boolean found = false;
          for (String localName : localNames) {
            if (localName.equals(name.name)) {
              found = true;
              break;
            }
          }

          assert found : "no name " + name.name + " for " + N + "\n" + ir;
        }
      }
    }
  }

  protected void verifyGraphAssertions(CallGraph CG, Object[][] assertionData) {
    // System.err.println(CG);

    if (assertionData == null) {
      return;
    }

    for (Object[] assertionDatum : assertionData) {

      check_target:
      for (int j = 0; j < ((String[]) assertionDatum[1]).length; j++) {
        Iterator<CGNode> srcs =
            (assertionDatum[0] instanceof String)
                ? getNodes(CG, (String) assertionDatum[0]).iterator()
                : new NonNullSingletonIterator<>(CG.getFakeRootNode());

        assert srcs.hasNext() : "cannot find " + assertionDatum[0];

        boolean checkAbsence = false;
        String targetName = ((String[]) assertionDatum[1])[j];
        if (targetName.startsWith("!")) {
          checkAbsence = true;
          targetName = targetName.substring(1);
        }

        while (srcs.hasNext()) {
          CGNode src = srcs.next();
          for (CallSiteReference sr : Iterator2Iterable.make(src.iterateCallSites())) {

            Iterator<CGNode> dsts = getNodes(CG, targetName).iterator();
            if (!checkAbsence) {
              assert dsts.hasNext() : "cannot find " + targetName;
            }

            while (dsts.hasNext()) {
              CGNode dst = dsts.next();
              for (CGNode cgNode : CG.getPossibleTargets(src, sr)) {
                if (cgNode.equals(dst)) {
                  if (checkAbsence) {
                    System.err.println(("found unexpected " + src + " --> " + dst + " at " + sr));
                    assert false : "found edge " + assertionDatum[0] + " ---> " + targetName;
                  } else {
                    System.err.println(("found expected " + src + " --> " + dst + " at " + sr));
                    continue check_target;
                  }
                }
              }
            }
          }
        }

        System.err.println("cannot find edge " + assertionDatum[0] + " ---> " + targetName);
        assert checkAbsence : "cannot find edge " + assertionDatum[0] + " ---> " + targetName;
      }
    }
  }

  /**
   * Verifies that none of the nodes that match the source description has an edge to any of the
   * nodes that match the destination description. (Used for checking for false connections in the
   * callgraph)
   */
  public void verifyNoEdges(CallGraph CG, String sourceDescription, String destDescription) {
    Collection<CGNode> sources = getNodes(CG, sourceDescription);
    Collection<CGNode> dests = getNodes(CG, destDescription);
    for (Object source : sources) {
      for (Object dest : dests) {
        for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes((CGNode) source))) {
          if (n.equals(dest)) {
            assert false : "Found a link from " + source + " to " + dest;
          }
        }
      }
    }
  }

  public static final Object ROOT =
      new Object() {
        @Override
        public String toString() {
          return "CallGraphRoot";
        }
      };

  public abstract Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier);
}
