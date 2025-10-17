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
import com.ibm.wala.util.collections.Pair;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class TestCallGraphShape {

  public static class CFGAssertion {
    final String function;
    final int[][] edges;

    public CFGAssertion(final String function, final int[][] edges) {
      this.function = function;
      this.edges = edges;
    }
  }

  public void verifyCFGAssertions(CallGraph CG, List<CFGAssertion> assertionData) {
    for (final var dat : assertionData) {
      final var function = dat.function;
      for (CGNode N : getNodes(CG, function)) {
        final var edges = dat.edges;
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

  public static class SourceAssertion {
    String function;
    String file;
    int firstLine;
    int lastLine;

    public SourceAssertion(String function, String file, int firstLine, int lastLine) {
      this.function = function;
      this.file = file;
      this.firstLine = firstLine;
      this.lastLine = lastLine;
    }
  }

  public void verifySourceAssertions(CallGraph CG, List<SourceAssertion> assertionData) {
    for (final var dat : assertionData) {
      final var function = dat.function;
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
                for (final var assertionDatum : assertionData) {
                  var file = assertionDatum.file;
                  if (file.indexOf('/') >= 0) {
                    file = file.substring(file.lastIndexOf('/') + 1);
                  }
                  if (file.equalsIgnoreCase(fileName)) {
                    if (pos.getFirstLine() >= assertionDatum.firstLine
                        && (pos.getLastLine() != -1 ? pos.getLastLine() : pos.getFirstLine())
                            <= assertionDatum.lastLine) {
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

  public void verifyNameAssertions(CallGraph CG, List<Pair<String, List<Name>>> assertionData) {
    for (final var element : assertionData) {
      for (final var N : getNodes(CG, element.fst)) {
        IR ir = N.getIR();
        final var names = element.snd;
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

  public static class GraphAssertion {
    Object source;
    String[] targets;

    public GraphAssertion(Object source, String[] targets) {
      this.source = source;
      this.targets = targets;
    }
  }

  protected void verifyGraphAssertions(CallGraph CG, List<GraphAssertion> assertionData) {
    // System.err.println(CG);

    if (assertionData == null) {
      return;
    }

    for (final var assertionDatum : assertionData) {

      check_target:
      for (int j = 0; j < assertionDatum.targets.length; j++) {
        Iterator<CGNode> srcs =
            (assertionDatum.source instanceof String)
                ? getNodes(CG, (String) assertionDatum.source).iterator()
                : new NonNullSingletonIterator<>(CG.getFakeRootNode());

        assert srcs.hasNext() : "cannot find " + assertionDatum.source;

        boolean checkAbsence = false;
        String targetName = assertionDatum.targets[j];
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
                    assert false : "found edge " + assertionDatum.source + " ---> " + targetName;
                  } else {
                    continue check_target;
                  }
                }
              }
            }
          }
        }

        System.err.println("cannot find edge " + assertionDatum.source + " ---> " + targetName);
        assert checkAbsence : "cannot find edge " + assertionDatum.source + " ---> " + targetName;
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
    for (CGNode source : sources) {
      for (Object dest : dests) {
        for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes(source))) {
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
