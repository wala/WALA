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
package com.ibm.wala.ipa.cfg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.TwoExitCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.Function;
import com.ibm.wala.util.IndiscriminateFilter;
import com.ibm.wala.util.MapIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.perf.EngineTimings;

/**
 * 
 * Interprocedural control-flow graph.
 * 
 * TODO: think about a better implementation; perhaps a lazy view of the
 * constituent CFGs Lots of ways this can be optimized?
 * 
 * @author sfink
 * @author Julian Dolby
 */
public class InterproceduralCFG implements NumberedGraph<BasicBlockInContext> {

  private static final int DEBUG_LEVEL = 0;

  /**
   * Should the graph include call-to-return edges?
   */
  private final static boolean CALL_TO_RETURN_EDGES = true;

  /**
   * Graph implementation we delegate to.
   */
  final private NumberedGraph<BasicBlockInContext> G = new SlowSparseNumberedGraph<BasicBlockInContext>(2);

  /**
   * Governing call graph
   */
  private final CallGraph cg;

  /**
   * Should we introduce a distinguished "exceptional exit" from each individual
   * CFG?
   */
  final private boolean partitionExits;

  /**
   * Filter that determines relevant call graph nodes
   */
  private final Filter<CGNode> relevant;

  /**
   * a cache: for each node (Basic Block), does that block end in a call?
   */
  private final BitVector hasCallVector = new BitVector();

  /**
   * Build an Interprocedural CFG from a call graph. This version defaults to
   * using whatever CFGs the call graph provides by default, and includes all
   * nodes in the call graph.
   * 
   * @param CG
   *            the call graph
   */
  @SuppressWarnings("unchecked")
  public InterproceduralCFG(CallGraph CG) {
    this(CG, IndiscriminateFilter.singleton(), false);
  }

  /**
   * Build an Interprocedural CFG from a call graph.
   * 
   * @param CG
   *            the call graph
   * @param relevant
   *            a filter which accepts those call graph nodes which should be
   *            included in the I-CFG. Other nodes are ignored.
   */
  public InterproceduralCFG(CallGraph CG, Filter<CGNode> relevant, boolean partitionExits) {

    EngineTimings.startVirtual("InterproceduralCFG.<init>");
    this.cg = CG;
    this.relevant = relevant;
    this.partitionExits = partitionExits;

    // create nodes for IPCFG
    createNodes();

    // create edges for IPCFG
    createEdges();

    if (DEBUG_LEVEL > 1) {
      try {
        GraphIntegrity.check(this);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
    EngineTimings.finishVirtual("InterproceduralCFG.<init>");
  }

  /**
   * create the edges that define this graph.
   */
  private void createEdges() {

    for (Iterator ns = cg.iterator(); ns.hasNext();) {
      CGNode n = (CGNode) ns.next();
      if (relevant.accepts(n)) {
        // retrieve a cfg for node n.
        ControlFlowGraph<ISSABasicBlock> cfg = n.getCFG();
        if (cfg == null) {
          // n is an unmodelled native method
          continue;
        }
        if (partitionExits) {
          cfg = new TwoExitCFG(cfg);
        }
        IInstruction[] instrs = cfg.getInstructions();
        // create edges for node n.
        for (Iterator bbs = cfg.iterator(); bbs.hasNext();) {
          ISSABasicBlock bb = (ISSABasicBlock) bbs.next();
          // entry node gets edges from callers
          if (bb == cfg.entry()) {
            addEdgesToEntryBlock(n, bb);
          }
          // other instructions get edges from predecessors,
          // with special handling for calls.
          else {
            addEdgesToNonEntryBlock(n, cfg, instrs, bb);
          }
        }
      }
    }
  }

  /**
   * create the nodes. side effect: populates the hasCallVector
   */
  private void createNodes() {
    for (Iterator ns = cg.iterator(); ns.hasNext();) {
      CGNode n = (CGNode) ns.next();
      if (relevant.accepts(n)) {
        if (DEBUG_LEVEL > 0) {
          Trace.println("Found a relevant node: " + n);
        }

        // retrieve a cfg for node n.
        ControlFlowGraph<ISSABasicBlock> cfg = getCFG(n);
        if (cfg != null) {
          // create a node for each basic block.
          addNodeForEachBasicBlock(cfg, n);
        }
      }
    }
  }

  /**
   * @return the cfg for n, or null if none found
   * @throws IllegalArgumentException
   *             if n == null
   */
  public ControlFlowGraph<ISSABasicBlock> getCFG(CGNode n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n == null");
    }
    ControlFlowGraph<ISSABasicBlock> cfg = n.getCFG();
    if (cfg == null) {
      return null;
    }

    if (DEBUG_LEVEL > 1) {
      // TODO: we NEED TO FIX synthetic methods to have
      // meaningful IRs for flow-sensitive analysis
      if (!n.getMethod().isSynthetic()) {
        try {
          GraphIntegrity.check(cfg);
        } catch (UnsoundGraphException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
        }
      }
    }
    if (partitionExits) {
      cfg = new TwoExitCFG(cfg);
    }
    if (DEBUG_LEVEL > 1) {
      // TODO: we NEED TO FIX synthetic methods to have
      // meaningful IRs for flow-sensitive analysis
      if (!n.getMethod().isSynthetic()) {
        try {
          GraphIntegrity.check(cfg);
        } catch (UnsoundGraphException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
        }
      }
    }
    return cfg;
  }

  /**
   * Add edges to the IPCFG for the incoming edges incident on a basic block bb
   * 
   * @param n
   *            a call graph node
   * @param cfg
   *            the CFG for n
   * @param instrs
   *            the instructions for node n
   * @param bb
   *            a basic block in the CFG
   */
  private void addEdgesToNonEntryBlock(CGNode n, ControlFlowGraph<ISSABasicBlock> cfg, IInstruction[] instrs, ISSABasicBlock bb) {

    if (DEBUG_LEVEL > 0) {
      Trace.println("addEdgesForNonEntryBlock: " + bb);
      Trace.println("cfg class: " + cfg.getClass());
      Trace.println("nPred: " + cfg.getPredNodeCount(bb));
    }

    for (Iterator ps = cfg.getPredNodes(bb); ps.hasNext();) {
      ISSABasicBlock pb = (ISSABasicBlock) ps.next();
      if (DEBUG_LEVEL > 0) {
        Trace.println("Consider previous block: " + pb);
      }

      if (pb.getLastInstructionIndex() < 0) {
        // pb is the entry block for a cfg with
        // no instructions.
        BasicBlockInContext p = new BasicBlockInContext(n, pb);
        BasicBlockInContext b = new BasicBlockInContext(n, bb);
        G.addEdge(p, b);
        continue;
      }

      int index = pb.getLastInstructionIndex();
      IInstruction inst = instrs[index];
      if (DEBUG_LEVEL > 0) {
        Trace.println("Last instruction is : " + inst);
      }
      if (inst instanceof IInvokeInstruction) {
        // a previous instruction is a call instruction. If necessary,
        // add an edge from the exit() of each target of the call.
        IInvokeInstruction call = (IInvokeInstruction) inst;
        CallSiteReference site = makeCallSiteReference(n.getMethod().getDeclaringClass().getClassLoader().getReference(), cfg
            .getProgramCounter(index), call);
        if (DEBUG_LEVEL > 0) {
          Trace.println("got Site: " + site);
        }
        boolean irrelevantTargets = false;
        for (Iterator ts = cg.getPossibleTargets(n, site).iterator(); ts.hasNext();) {
          CGNode tn = (CGNode) ts.next();
          if (!relevant.accepts(tn)) {
            if (DEBUG_LEVEL > 0) {
              Trace.println("Irrelevant target: " + tn);
            }
            irrelevantTargets = true;
            continue;
          }

          if (DEBUG_LEVEL > 0) {
            Trace.println("Relevant target: " + tn);
          }
          // add an edge from tn exit to this node
          ControlFlowGraph<ISSABasicBlock> tcfg = tn.getCFG();
          // tcfg might be null if tn is an unmodelled native method
          if (tcfg != null) {
            if (partitionExits) {
              if (DEBUG_LEVEL > 0) {
                Trace.println("normal? " + representsNormalReturn(cfg, bb, pb));
                Trace.println("exceptional? " + representsExceptionalReturn(cfg, bb, pb));
              }
              if (!(tcfg instanceof TwoExitCFG)) {
                tcfg = new TwoExitCFG(tcfg);
              }
              if (representsNormalReturn(cfg, bb, pb)) {
                if (DEBUG_LEVEL > 0) {
                  Trace.println("representsNormalReturn: " + bb + " " + pb);
                }
                addEdgesFromNormalExitToReturn(n, bb, tn, (TwoExitCFG) tcfg);
              }
              if (representsExceptionalReturn(cfg, bb, pb)) {
                if (DEBUG_LEVEL > 0) {
                  Trace.println("representsExceptionalReturn: " + bb + " " + pb);
                }
                addEdgesFromExceptionalExitToReturn(n, bb, tn, (TwoExitCFG) tcfg);
              }
            } else {
              addEdgesFromExitToReturn(n, bb, tn, tcfg);
            }
          }
        }

        if (irrelevantTargets || CALL_TO_RETURN_EDGES) {
          // at least one call target was ignored. So add a "normal" edge
          // from the predecessor block to this block.
          BasicBlockInContext p = new BasicBlockInContext(n, pb);
          BasicBlockInContext b = new BasicBlockInContext(n, bb);
          G.addEdge(p, b);
        }
      } else {
        // previous instruction is not a call instruction.
        BasicBlockInContext p = new BasicBlockInContext(n, pb);
        BasicBlockInContext b = new BasicBlockInContext(n, bb);
        if (Assertions.verifyAssertions) {
          if (!G.containsNode(p) || !G.containsNode(b)) {
            Assertions._assert(G.containsNode(p), "IPCFG does not contain " + p);
            Assertions._assert(G.containsNode(b), "IPCFG does not contain " + b);
          }
        }
        G.addEdge(p, b);
      }
    }
  }

  public static CallSiteReference makeCallSiteReference(ClassLoaderReference loader, int pc, IInvokeInstruction call)
      throws IllegalArgumentException, IllegalArgumentException {

    if (call == null) {
      throw new IllegalArgumentException("call == null");
    }
    if (!(call instanceof com.ibm.wala.ssa.SSAInvokeInstruction) && !(call instanceof com.ibm.wala.shrikeBT.InvokeInstruction)) {
      throw new IllegalArgumentException(
          "(not ( call instanceof com.ibm.wala.ssa.SSAInvokeInstruction ) ) and (not ( call instanceof com.ibm.wala.shrikeBT.InvokeInstruction ) )");
    }
    CallSiteReference site = null;
    if (call instanceof InvokeInstruction) {
      InvokeInstruction c = (InvokeInstruction) call;
      site = CallSiteReference.make(pc, MethodReference.findOrCreate(loader, c.getClassType(), c.getMethodName(), c
          .getMethodSignature()), call.getInvocationCode());
    } else {
      com.ibm.wala.ssa.SSAInvokeInstruction c = (com.ibm.wala.ssa.SSAInvokeInstruction) call;
      site = CallSiteReference.make(pc, c.getDeclaredTarget(), call.getInvocationCode());
    }
    return site;
  }

  /**
   * Add an edge from the exceptional exit block of a callee to a return site in
   * the caller
   * 
   * @param returnBlock
   *            the return site for a call
   * @param targetCFG
   *            the called method
   */
  private void addEdgesFromExceptionalExitToReturn(CGNode caller, ISSABasicBlock returnBlock, CGNode target, TwoExitCFG targetCFG) {
    ISSABasicBlock texit = targetCFG.getExceptionalExit();
    BasicBlockInContext exit = new BasicBlockInContext(target, texit);
    BasicBlockInContext ret = new BasicBlockInContext(caller, returnBlock);
    if (Assertions.verifyAssertions) {
      if (!G.containsNode(exit) || !G.containsNode(ret)) {
        Assertions._assert(G.containsNode(exit), "IPCFG does not contain " + exit);
        Assertions._assert(G.containsNode(ret), "IPCFG does not contain " + ret);
      }
    }
    G.addEdge(exit, ret);
  }

  /**
   * @param cfg
   *            the governing cfg
   * @param ret
   *            a return site for the call
   * @param call
   *            a basic block that ends in a call
   * @return true iff bb is reached from pb by exceptional control flow
   */
  private boolean representsExceptionalReturn(ControlFlowGraph<ISSABasicBlock> cfg, ISSABasicBlock ret, ISSABasicBlock call) {
    for (Iterator it = cfg.getExceptionalSuccessors(call).iterator(); it.hasNext();) {
      if (ret.equals(it.next())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add an edge from the normal exit block of a callee to a return site in the
   * caller
   * 
   * @param returnBlock
   *            the return site for a call
   * @param targetCFG
   *            the called method
   */
  private void addEdgesFromNormalExitToReturn(CGNode caller, ISSABasicBlock returnBlock, CGNode target, TwoExitCFG targetCFG) {
    ISSABasicBlock texit = targetCFG.getNormalExit();
    BasicBlockInContext exit = new BasicBlockInContext(target, texit);
    BasicBlockInContext ret = new BasicBlockInContext(caller, returnBlock);
    if (Assertions.verifyAssertions) {
      if (!G.containsNode(exit) || !G.containsNode(ret)) {
        Assertions._assert(G.containsNode(exit), "IPCFG does not contain " + exit);
        Assertions._assert(G.containsNode(ret), "IPCFG does not contain " + ret);
      }
    }
    G.addEdge(exit, ret);
  }

  /**
   * @param cfg
   *            the governing cfg
   * @return true iff bb is reached from pb by a normal return flow
   */
  private boolean representsNormalReturn(ControlFlowGraph<ISSABasicBlock> cfg, ISSABasicBlock ret, ISSABasicBlock call) {
    for (Iterator it = cfg.getNormalSuccessors(call).iterator(); it.hasNext();) {
      if (ret.equals(it.next())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add an edge from the exit() block of a callee to a return site in the
   * caller
   * 
   * @param returnBlock
   *            the return site for a call
   * @param targetCFG
   *            the called method
   */
  private void addEdgesFromExitToReturn(CGNode caller, ISSABasicBlock returnBlock, CGNode target,
      ControlFlowGraph<ISSABasicBlock> targetCFG) {
    ISSABasicBlock texit = targetCFG.exit();
    BasicBlockInContext exit = new BasicBlockInContext(target, texit);
    BasicBlockInContext ret = new BasicBlockInContext(caller, returnBlock);
    if (Assertions.verifyAssertions) {
      if (!G.containsNode(exit) || !G.containsNode(ret)) {
        Assertions._assert(G.containsNode(exit), "IPCFG does not contain " + exit);
        Assertions._assert(G.containsNode(ret), "IPCFG does not contain " + ret);
      }
    }
    G.addEdge(exit, ret);
  }

  /**
   * Add the incoming edges to the entry() block for a call graph node.
   * 
   * @param n
   *            a node in the call graph
   * @param bb
   *            the entry() block for n
   */
  private void addEdgesToEntryBlock(CGNode n, ISSABasicBlock bb) {

    if (DEBUG_LEVEL > 0) {
      Trace.println("addEdgesToEntryBlock " + bb);
    }

    for (Iterator callers = cg.getPredNodes(n); callers.hasNext();) {
      CGNode caller = (CGNode) callers.next();
      if (DEBUG_LEVEL > 0) {
        Trace.println("got caller " + caller);
      }
      if (relevant.accepts(caller)) {
        if (DEBUG_LEVEL > 0) {
          Trace.println("caller " + caller + "is relevant");
        }
        ControlFlowGraph<ISSABasicBlock> ccfg = caller.getCFG();
        IInstruction[] cinsts = ccfg.getInstructions();

        if (DEBUG_LEVEL > 0) {
          Trace.println("Visiting " + cinsts.length + " instructions");
        }
        for (int i = 0; i < cinsts.length; i++) {
          if (cinsts[i] instanceof IInvokeInstruction) {
            if (DEBUG_LEVEL > 0) {
              Trace.println("Checking invokeinstruction: " + cinsts[i]);
            }
            IInvokeInstruction call = (IInvokeInstruction) cinsts[i];
            CallSiteReference site = makeCallSiteReference(n.getMethod().getDeclaringClass().getClassLoader().getReference(), ccfg
                .getProgramCounter(i), call);
            if (cg.getPossibleTargets(caller, site).contains(n)) {
              if (DEBUG_LEVEL > 0) {
                Trace.println("Adding edge " + ccfg.getBlockForInstruction(i) + " to " + bb);
              }
              ISSABasicBlock callerBB = ccfg.getBlockForInstruction(i);
              BasicBlockInContext b1 = new BasicBlockInContext(caller, callerBB);
              BasicBlockInContext b2 = new BasicBlockInContext(n, bb);
              G.addEdge(b1, b2);
            }
          }
        }
      }
    }
  }

  /**
   * Add a node to the IPCFG for each node in a CFG. side effect: populates the
   * hasCallVector
   * 
   * @param cfg
   *            a control-flow graph
   */
  private void addNodeForEachBasicBlock(ControlFlowGraph<ISSABasicBlock> cfg, CGNode N) {
    for (Iterator<ISSABasicBlock> bbs = cfg.iterator(); bbs.hasNext();) {
      ISSABasicBlock bb = bbs.next();
      if (DEBUG_LEVEL > 0) {
        Trace.println("IPCFG Add basic block " + bb);
      }
      BasicBlockInContext b = new BasicBlockInContext(N, bb);
      G.addNode(b);
      if (hasCall(b, cfg)) {
        hasCallVector.set(getNumber(b));
      }
    }
  }

  /**
   * @return the original CFG from whence B came
   * @throws IllegalArgumentException
   *             if B == null
   */
  public ControlFlowGraph<ISSABasicBlock> getCFG(BasicBlockInContext B) throws IllegalArgumentException {
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    return getCFG(getCGNode(B));
  }

  /**
   * @return the original CGNode from whence B came
   * @throws IllegalArgumentException
   *             if B == null
   */
  public CGNode getCGNode(BasicBlockInContext B) throws IllegalArgumentException {
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    return B.getNode();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeNodeAndEdges(BasicBlockInContext N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator<BasicBlockInContext> iterator() {
    return G.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return G.getNumberOfNodes();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(com.ibm.wala.util.graph.Node)
   */
  public void addNode(BasicBlockInContext n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(com.ibm.wala.util.graph.Node)
   */
  public void removeNode(BasicBlockInContext n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<? extends BasicBlockInContext> getPredNodes(BasicBlockInContext N) {
    return G.getPredNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(BasicBlockInContext N) {
    return G.getPredNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<? extends BasicBlockInContext> getSuccNodes(BasicBlockInContext N) {
    return G.getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(BasicBlockInContext N) {
    return G.getSuccNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node,
   *      com.ibm.wala.util.graph.Node)
   */
  public void addEdge(BasicBlockInContext src, BasicBlockInContext dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeEdge(BasicBlockInContext src, BasicBlockInContext dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeAllIncidentEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return G.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(BasicBlockInContext N) {
    return G.containsNode(N);
  }

  /**
   * @param B
   * @return true iff basic block B ends in a call instuction
   */
  public boolean hasCall(BasicBlockInContext B) {
    if (Assertions.verifyAssertions) {
      if (!containsNode(B)) {
        Assertions._assert(containsNode(B));
      }
    }
    return hasCallVector.get(getNumber(B));
  }

  /**
   * @return true iff basic block B ends in a call instuction
   */
  private boolean hasCall(BasicBlockInContext B, ControlFlowGraph cfg) {
    IInstruction[] statements = cfg.getInstructions();

    int lastIndex = B.getLastInstructionIndex();
    if (lastIndex >= 0) {

      if (Assertions.verifyAssertions) {
        if (statements.length <= lastIndex) {
          System.err.println(statements.length);
          System.err.println(cfg);
          Assertions._assert(lastIndex < statements.length, "bad BB " + B + " and CFG for " + getCGNode(B));
        }
      }
      IInstruction last = statements[lastIndex];
      return (last instanceof IInvokeInstruction);
    } else {
      return false;
    }
  }

  /**
   * @param B
   * @return the set of CGNodes that B may call, according to the governing call
   *         graph.
   * @throws IllegalArgumentException
   *             if B is null
   */
  public Set<CGNode> getCallTargets(BasicBlockInContext B) {
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    ControlFlowGraph cfg = getCFG(B);
    return getCallTargets(B, cfg, getCGNode(B));
  }

  /**
   * @return the set of CGNodes that B may call, according to the governing call
   *         graph.
   */
  private Set<CGNode> getCallTargets(IBasicBlock B, ControlFlowGraph cfg, CGNode Bnode) {
    IInstruction[] statements = cfg.getInstructions();
    IInvokeInstruction call = (IInvokeInstruction) statements[B.getLastInstructionIndex()];
    int pc = cfg.getProgramCounter(B.getLastInstructionIndex());
    CallSiteReference site = makeCallSiteReference(B.getMethod().getDeclaringClass().getClassLoader().getReference(), pc, call);
    HashSet<CGNode> result = HashSetFactory.make(cg.getNumberOfTargets(Bnode, site));
    for (Iterator<CGNode> it = cg.getPossibleTargets(Bnode, site).iterator(); it.hasNext();) {
      CGNode target = it.next();
      result.add(target);
    }
    return result;
  }

  public void removeIncomingEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeOutgoingEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean hasEdge(BasicBlockInContext src, BasicBlockInContext dst) {
    return G.hasEdge(src, dst);
  }

  public int getNumber(BasicBlockInContext N) {
    return G.getNumber(N);
  }

  public BasicBlockInContext getNode(int number) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public int getMaxNumber() {
    return G.getMaxNumber();
  }

  public Iterator<BasicBlockInContext> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(BasicBlockInContext node) {
    return G.getSuccNodeNumbers(node);
  }

  public IntSet getPredNodeNumbers(BasicBlockInContext node) {
    return G.getPredNodeNumbers(node);
  }

  public Object getEntry(CGNode n) {
    ControlFlowGraph<ISSABasicBlock> cfg = getCFG(n);
    ISSABasicBlock entry = cfg.entry();
    return new BasicBlockInContext(n, entry);
  }

  /**
   * @param bb
   *            node in the IPCFG that ends in a call
   * @return the nodes that are return sites for this call.
   * @throws IllegalArgumentException
   *             if bb is null
   */
  public Iterator<BasicBlockInContext> getReturnSites(BasicBlockInContext bb) {
    if (bb == null) {
      throw new IllegalArgumentException("bb is null");
    }
    final CGNode node = bb.getNode();

    // a successor node is a return site if it is in the same
    // procedure, and is not the entry() node.
    Filter isReturn = new Filter() {
      public boolean accepts(Object o) {
        if (Assertions.verifyAssertions) {
          Assertions._assert(o instanceof BasicBlockInContext);
        }
        BasicBlockInContext other = (BasicBlockInContext) o;
        return !other.isEntryBlock() && node.equals(other.getNode());
      }
    };
    return new FilterIterator<BasicBlockInContext>(getSuccNodes(bb), isReturn);
  }

  public Iterator<BasicBlockInContext> getCallSites(BasicBlockInContext bb) {
    if (bb == null) {
      throw new IllegalArgumentException("bb is null");
    }
    ControlFlowGraph<ISSABasicBlock> cfg = getCFG(bb);
    Iterator<? extends ISSABasicBlock> it = cfg.getPredNodes(bb.getDelegate());
    final CGNode node = bb.getNode();
    Function<ISSABasicBlock, BasicBlockInContext> toContext = new Function<ISSABasicBlock, BasicBlockInContext>() {
      public BasicBlockInContext apply(ISSABasicBlock object) {
        ISSABasicBlock b = object;
        return new BasicBlockInContext(node, b);
      }
    };
    MapIterator<ISSABasicBlock, BasicBlockInContext> m = new MapIterator<ISSABasicBlock, BasicBlockInContext>(it, toContext);
    return new FilterIterator<BasicBlockInContext>(m, isCall);
  }

  private final Filter isCall = new Filter() {
    public boolean accepts(Object o) {
      return hasCall((BasicBlockInContext) o);
    }
  };

  public boolean isReturn(BasicBlockInContext bb) throws IllegalArgumentException {
    if (bb == null) {
      throw new IllegalArgumentException("bb == null");
    }
    ControlFlowGraph<ISSABasicBlock> cfg = getCFG(bb);
    for (Iterator<? extends ISSABasicBlock> it = cfg.getPredNodes(bb.getDelegate()); it.hasNext();) {
      ISSABasicBlock b = it.next();
      if (hasCall(new BasicBlockInContext(bb.getNode(), b))) {
        return true;
      }
    }
    return false;
  }
}
