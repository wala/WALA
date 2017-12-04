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

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.IndiscriminateFilter;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * Interprocedural control-flow graph, constructed lazily.
 */
public abstract class AbstractInterproceduralCFG<T extends ISSABasicBlock> implements NumberedGraph<BasicBlockInContext<T>> {

  private static final int DEBUG_LEVEL = 0;

  private static final boolean WARN_ON_EAGER_CONSTRUCTION = false;

  private static final boolean FAIL_ON_EAGER_CONSTRUCTION = false;

  /**
   * Should the graph include call-to-return edges? When set to <code>false</code>, the graphs output by {@link IFDSExplorer} look
   * incorrect
   */
  private final static boolean CALL_TO_RETURN_EDGES = true;

  /**
   * Graph implementation we delegate to.
   */
  final private NumberedGraph<BasicBlockInContext<T>> g = new SlowSparseNumberedGraph<>(2);

  /**
   * Governing call graph
   */
  private final CallGraph cg;

  /**
   * Filter that determines relevant call graph nodes
   */
  private final Predicate<CGNode> relevant;

  /**
   * a cache: for each node (Basic Block), does that block end in a call?
   */
  private final BitVector hasCallVector = new BitVector();

  /**
   * CGNodes whose intraprocedural edges have been added to IPCFG
   */
  private MutableIntSet cgNodesVisited = new BitVectorIntSet();

  /**
   * those cg nodes whose edges to callers have been added
   */
  private MutableIntSet cgNodesWithCallerEdges = new BitVectorIntSet();

  /**
   * those call nodes whose successor edges (interprocedural) have been added
   */
  private MutableIntSet handledCalls = new BitVectorIntSet();

  /**
   * those return nodes whose predecessor edges (interprocedural) have been added
   */
  private MutableIntSet handledReturns = new BitVectorIntSet();

  /**
   * those nodes whose successor edges (intra- and inter-procedural) have been added
   */
  private MutableIntSet addedSuccs = new BitVectorIntSet();

  /**
   * those nodes whose predecessor edges (intra- and inter-procedural) have been added
   */
  private MutableIntSet addedPreds = new BitVectorIntSet();

  /**
   * Should be invoked when the underlying call graph has changed. This will cause certain successor and predecessor edges to be
   * recomputed. USE WITH EXTREME CARE.
   */
  public void callGraphUpdated() {
    cgNodesVisited = new BitVectorIntSet();
    cgNodesWithCallerEdges = new BitVectorIntSet();
    handledCalls = new BitVectorIntSet();
    handledReturns = new BitVectorIntSet();
    addedSuccs = new BitVectorIntSet();
    addedPreds = new BitVectorIntSet();
  }

  public abstract ControlFlowGraph<SSAInstruction, T> getCFG(CGNode n);

  /**
   * Build an Interprocedural CFG from a call graph. This version defaults to using whatever CFGs the call graph provides by
   * default, and includes all nodes in the call graph.
   * 
   * @param cg the call graph
   */
  public AbstractInterproceduralCFG(CallGraph cg) {
    this(cg, IndiscriminateFilter.<CGNode> singleton());
  }

  /**
   * Build an Interprocedural CFG from a call graph.
   * 
   * @param CG the call graph
   * @param relevant a filter which accepts those call graph nodes which should be included in the I-CFG. Other nodes are ignored.
   */
  public AbstractInterproceduralCFG(CallGraph CG, Predicate<CGNode> relevant) {

    this.cg = CG;
    this.relevant = relevant;

  }

  /**
   * If n is relevant and its cfg has not already been added, add nodes and edges for n
   * 
   * @param n
   */
  @SuppressWarnings("unused")
  private void addIntraproceduralNodesAndEdgesForCGNodeIfNeeded(CGNode n) {
    if (!cgNodesVisited.contains(cg.getNumber(n)) && relevant.test(n)) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("Adding nodes and edges for cg node: " + n);
      }
      cgNodesVisited.add(cg.getNumber(n));
      // retrieve a cfg for node n.
      ControlFlowGraph<SSAInstruction, T> cfg = getCFG(n);
      if (cfg != null) {
        // create a node for each basic block.
        addNodeForEachBasicBlock(cfg, n);
        SSAInstruction[] instrs = cfg.getInstructions();
        // create edges for node n.
        for (T bb : cfg) {
          if (bb != cfg.entry())
            addEdgesToNonEntryBlock(n, cfg, instrs, bb);
        }
      }
    }
  }

  /**
   * Add edges to the IPCFG for the incoming edges incident on a basic block bb.
   * 
   * @param n a call graph node
   * @param cfg the CFG for n
   * @param instrs the instructions for node n
   * @param bb a basic block in the CFG
   */
  @SuppressWarnings("unused")
  protected void addEdgesToNonEntryBlock(CGNode n, ControlFlowGraph<?, T> cfg, SSAInstruction[] instrs, T bb) {
    if (DEBUG_LEVEL > 1) {
      System.err.println("addEdgesToNonEntryBlock: " + bb);
      System.err.println("nPred: " + cfg.getPredNodeCount(bb));
    }

    for (T pb : Iterator2Iterable.make(cfg.getPredNodes(bb))) {
      if (DEBUG_LEVEL > 1) {
        System.err.println("Consider previous block: " + pb);
      }

      if (pb.equals(cfg.entry())) {
        // entry block has no instructions
        BasicBlockInContext<T> p = new BasicBlockInContext<>(n, pb);
        BasicBlockInContext<T> b = new BasicBlockInContext<>(n, bb);
        g.addEdge(p, b);
        continue;
      }
      
      SSAInstruction inst = getLastInstructionForBlock(pb, instrs);

      if (DEBUG_LEVEL > 1) {
        System.err.println("Last instruction is : " + inst);
      }
      if (inst instanceof SSAAbstractInvokeInstruction) {
        if (CALL_TO_RETURN_EDGES) {
          // Add a "normal" edge from the predecessor block to this block.
          BasicBlockInContext<T> p = new BasicBlockInContext<>(n, pb);
          BasicBlockInContext<T> b = new BasicBlockInContext<>(n, bb);
          g.addEdge(p, b);
        }
      } else {
        // previous instruction is not a call instruction.
        BasicBlockInContext<T> p = new BasicBlockInContext<>(n, pb);
        BasicBlockInContext<T> b = new BasicBlockInContext<>(n, bb);
        if (!g.containsNode(p) || !g.containsNode(b)) {
          assert g.containsNode(p) : "IPCFG does not contain " + p;
          assert g.containsNode(b) : "IPCFG does not contain " + b;
        }
        g.addEdge(p, b);
      }
    }
  }

  protected SSAInstruction getLastInstructionForBlock(T pb, SSAInstruction[] instrs) {
    int index = pb.getLastInstructionIndex();
    SSAInstruction inst = instrs[index];
    return inst;
  }


  /**
   * Add an edge from the exit() block of a callee to a return site in the caller
   * 
   * @param returnBlock the return site for a call
   * @param targetCFG the called method
   */
  @SuppressWarnings("unused")
  private void addEdgesFromExitToReturn(CGNode caller, T returnBlock, CGNode target,
      ControlFlowGraph<SSAInstruction, ? extends T> targetCFG) {
    T texit = targetCFG.exit();
    BasicBlockInContext<T> exit = new BasicBlockInContext<>(target, texit);
    addNodeForBasicBlockIfNeeded(exit);
    BasicBlockInContext<T> ret = new BasicBlockInContext<>(caller, returnBlock);
    if (!g.containsNode(exit) || !g.containsNode(ret)) {
      assert g.containsNode(exit) : "IPCFG does not contain " + exit;
      assert g.containsNode(ret) : "IPCFG does not contain " + ret;
    }
    if (DEBUG_LEVEL > 1) {
      System.err.println("addEdgeFromExitToReturn " + exit + ret);
    }
    g.addEdge(exit, ret);
  }

  /**
   * Add an edge from the exit() block of a callee to a return site in the caller
   * 
   * @param callBlock the return site for a call
   * @param targetCFG the called method
   */
  @SuppressWarnings("unused")
  private void addEdgesFromCallToEntry(CGNode caller, T callBlock, CGNode target,
      ControlFlowGraph<SSAInstruction, ? extends T> targetCFG) {
    T tentry = targetCFG.entry();
    BasicBlockInContext<T> entry = new BasicBlockInContext<>(target, tentry);
    addNodeForBasicBlockIfNeeded(entry);
    BasicBlockInContext<T> call = new BasicBlockInContext<>(caller, callBlock);
    if (!g.containsNode(entry) || !g.containsNode(call)) {
      assert g.containsNode(entry) : "IPCFG does not contain " + entry;
      assert g.containsNode(call) : "IPCFG does not contain " + call;
    }
    if (DEBUG_LEVEL > 1) {
      System.err.println("addEdgeFromCallToEntry " + call + " " + entry);
    }
    g.addEdge(call, entry);
  }

  /**
   * Add the incoming edges to the entry() block and the outgoing edges from the exit() block for a call graph node.
   * 
   * @param n a node in the call graph
   * @param bb the entry() block for n
   */
  @SuppressWarnings("unused")
  private void addInterproceduralEdgesForEntryAndExitBlocks(CGNode n, ControlFlowGraph<SSAInstruction, ? extends T> cfg) {

    T entryBlock = cfg.entry();
    T exitBlock = cfg.exit();
    if (DEBUG_LEVEL > 0) {
      System.err.println("addInterproceduralEdgesForEntryAndExitBlocks " + n);
    }

    for (CGNode caller : Iterator2Iterable.make(cg.getPredNodes(n))) {
      if (DEBUG_LEVEL > 1) {
        System.err.println("got caller " + caller);
      }
      if (relevant.test(caller)) {
        addEntryAndExitEdgesToCaller(n, entryBlock, exitBlock, caller);
      }
    }
  }

  @SuppressWarnings("unused")
  private void addEntryAndExitEdgesToCaller(CGNode n, T entryBlock, T exitBlock, CGNode caller) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("caller " + caller + "is relevant");
    }
    ControlFlowGraph<SSAInstruction, T> ccfg = getCFG(caller);
    if (ccfg != null) {
      SSAInstruction[] cinsts = ccfg.getInstructions();

      if (DEBUG_LEVEL > 1) {
        System.err.println("Visiting " + cinsts.length + " instructions");
      }
      for (int i = 0; i < cinsts.length; i++) {
        if (cinsts[i] instanceof SSAAbstractInvokeInstruction) {
          if (DEBUG_LEVEL > 1) {
            System.err.println("Checking invokeinstruction: " + cinsts[i]);
          }
          SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) cinsts[i];
          CallSiteReference site = call.getCallSite();
          assert site.getProgramCounter() == ccfg.getProgramCounter(i);
          if (cg.getPossibleTargets(caller, site).contains(n)) {
            if (DEBUG_LEVEL > 1) {
              System.err.println("Adding edge " + ccfg.getBlockForInstruction(i) + " to " + entryBlock);
            }
            T callerBB = ccfg.getBlockForInstruction(i);
            BasicBlockInContext<T> b1 = new BasicBlockInContext<>(caller, callerBB);
            // need to add a node for caller basic block, in case we haven't processed caller yet
            addNodeForBasicBlockIfNeeded(b1);
            BasicBlockInContext<T> b2 = new BasicBlockInContext<>(n, entryBlock);
            g.addEdge(b1, b2);
            // also add edges from exit node to all return nodes (successor of call bb)
            for (T returnBB : Iterator2Iterable.make(ccfg.getSuccNodes(callerBB))) {
              BasicBlockInContext<T> b3 = new BasicBlockInContext<>(n, exitBlock);
              BasicBlockInContext<T> b4 = new BasicBlockInContext<>(caller, returnBB);
              addNodeForBasicBlockIfNeeded(b4);
              g.addEdge(b3, b4);
            }
          }
        }
      }
    }
  }

  /**
   * Add a node to the IPCFG for each node in a CFG. side effect: populates the hasCallVector
   * 
   * @param cfg a control-flow graph
   */
  @SuppressWarnings("unused")
  private void addNodeForEachBasicBlock(ControlFlowGraph<? extends SSAInstruction, ? extends T> cfg, CGNode N) {
    for (T bb : cfg) {
      if (DEBUG_LEVEL > 1) {
        System.err.println("IPCFG Add basic block " + bb);
      }
      BasicBlockInContext<T> b = new BasicBlockInContext<>(N, bb);
      addNodeForBasicBlockIfNeeded(b);
    }
  }

  private void addNodeForBasicBlockIfNeeded(BasicBlockInContext<T> b) {
    if (!g.containsNode(b)) {
      g.addNode(b);
      ControlFlowGraph<SSAInstruction, T> cfg = getCFG(b);
      if (hasCall(b, cfg)) {
        hasCallVector.set(g.getNumber(b));
      }
    }
  }

  /**
   * @return the original CFG from whence B came
   * @throws IllegalArgumentException if B == null
   */
  public ControlFlowGraph<SSAInstruction, T> getCFG(BasicBlockInContext B) throws IllegalArgumentException {
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    return getCFG(getCGNode(B));
  }

  /**
   * @return the original CGNode from whence B came
   * @throws IllegalArgumentException if B == null
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
  @Override
  public void removeNodeAndEdges(BasicBlockInContext N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  @Override
  public Iterator<BasicBlockInContext<T>> iterator() {
    if (WARN_ON_EAGER_CONSTRUCTION) {
      System.err.println("WARNING: forcing full ICFG construction by calling iterator()");
    }
    if (FAIL_ON_EAGER_CONSTRUCTION) {
      throw new UnimplementedError();
    }
    constructFullGraph();
    return g.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  @Override
  public int getNumberOfNodes() {
    if (WARN_ON_EAGER_CONSTRUCTION) {
      System.err.println("WARNING: forcing full ICFG construction by calling getNumberOfNodes()");
    }
    if (FAIL_ON_EAGER_CONSTRUCTION) {
      throw new UnimplementedError();
    }
    constructFullGraph();
    return g.getNumberOfNodes();
  }

  private boolean constructedFullGraph = false;

  private void constructFullGraph() {
    if (!constructedFullGraph) {
      for (CGNode n : cg) {
        addIntraproceduralNodesAndEdgesForCGNodeIfNeeded(n);
        addEdgesToCallees(n);
      }
      for (int i = 0; i < g.getMaxNumber(); i++) {
        addedSuccs.add(i);
        addedPreds.add(i);
      }
      constructedFullGraph = true;
    }
  }

  /**
   * add interprocedural edges to nodes in callees of n
   * 
   * @param n
   */
  private void addEdgesToCallees(CGNode n) {
    ControlFlowGraph<SSAInstruction, T> cfg = getCFG(n);
    if (cfg != null) {
      for (T bb : cfg) {
        BasicBlockInContext<T> block = new BasicBlockInContext<>(n, bb);
        if (hasCall(block)) {
          addCalleeEdgesForCall(n, block);
        }
      }
    }
  }

  /**
   * add edges to callees for return block and corresponding call block(s)
   */
  private void addCalleeEdgesForReturn(CGNode node, BasicBlockInContext<T> returnBlock) {
    final int num = g.getNumber(returnBlock);
    if (!handledReturns.contains(num)) {
      handledReturns.add(num);
      // compute calls for return
      ControlFlowGraph<SSAInstruction, T> cfg = getCFG(returnBlock);
      for (Iterator<? extends T> it = cfg.getPredNodes(returnBlock.getDelegate()); it.hasNext();) {
        T b = it.next();
        final BasicBlockInContext<T> block = new BasicBlockInContext<>(node, b);
        if (hasCall(block)) {
          addCalleeEdgesForCall(node, block);
        }
      }
    }
  }

  /**
   * add edges to callee entry for call block, and edges from callee exit to corresponding return blocks
   */
  @SuppressWarnings("unused")
  private void addCalleeEdgesForCall(CGNode n, BasicBlockInContext<T> callBlock) {
    int num = g.getNumber(callBlock);
    if (!handledCalls.contains(num)) {
      handledCalls.add(num);
      ControlFlowGraph<SSAInstruction, T> cfg = getCFG(n);
      CallSiteReference site = getCallSiteForCallBlock(callBlock, cfg);
      if (DEBUG_LEVEL > 1) {
        System.err.println("got Site: " + site);
      }
      boolean irrelevantTargets = false;
      for (CGNode tn : cg.getPossibleTargets(n, site)) {
        if (!relevant.test(tn)) {
          if (DEBUG_LEVEL > 1) {
            System.err.println("Irrelevant target: " + tn);
          }
          irrelevantTargets = true;
          continue;
        }

        if (DEBUG_LEVEL > 1) {
          System.err.println("Relevant target: " + tn);
        }
        // add an edge from tn exit to this node
        ControlFlowGraph<SSAInstruction, ? extends T> tcfg = getCFG(tn);
        // tcfg might be null if tn is an unmodelled native method
        if (tcfg != null) {
          final T cbDelegate = callBlock.getDelegate();
          addEdgesFromCallToEntry(n, cbDelegate, tn, tcfg);
          for (Iterator<? extends T> returnBlocks = cfg.getSuccNodes(cbDelegate); returnBlocks.hasNext();) {
            T retBlock = returnBlocks.next();
            addEdgesFromExitToReturn(n, retBlock, tn, tcfg);
            if (irrelevantTargets) {
              // Add a "normal" edge from the call block to the return block.
              g.addEdge(callBlock, new BasicBlockInContext<>(n, retBlock));
            }
          }
        }
      }
    }
  }

  /**
   * add edges to nodes in callers of n
   * 
   * @param n
   */
  private void addCallerEdges(CGNode n) {
    final int num = cg.getNumber(n);
    if (!cgNodesWithCallerEdges.contains(num)) {
      cgNodesWithCallerEdges.add(num);
      ControlFlowGraph<SSAInstruction, T> cfg = getCFG(n);
      addInterproceduralEdgesForEntryAndExitBlocks(n, cfg);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void addNode(BasicBlockInContext n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(BasicBlockInContext n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<BasicBlockInContext<T>> getPredNodes(BasicBlockInContext<T> N) {
    initForPred(N);
    return g.getPredNodes(N);
  }

  /**
   * add enough nodes and edges to the graph to allow for computing predecessors of N
   */
  private void initForPred(BasicBlockInContext<T> N) {
    CGNode node = getCGNode(N);
    addIntraproceduralNodesAndEdgesForCGNodeIfNeeded(node);
    int num = g.getNumber(N);
    if (!addedPreds.contains(num)) {
      addedPreds.add(num);
      if (N.getDelegate().isEntryBlock()) {
        addCallerEdges(node);
      }
      if (isReturn(N)) {
        addCalleeEdgesForReturn(node, N);
      }
    }
  }

  /**
   * add enough nodes and edges to the graph to allow for computing successors of N
   */
  private void initForSucc(BasicBlockInContext<T> N) {
    CGNode node = getCGNode(N);
    addIntraproceduralNodesAndEdgesForCGNodeIfNeeded(node);
    int num = g.getNumber(N);
    if (!addedSuccs.contains(num)) {
      addedSuccs.add(num);
      if (N.getDelegate().isExitBlock()) {
        addCallerEdges(node);
      }
      if (hasCall(N)) {
        addCalleeEdgesForCall(node, N);
      }
    }
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getPredNodeCount(BasicBlockInContext<T> N) {
    initForPred(N);
    return g.getPredNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<BasicBlockInContext<T>> getSuccNodes(BasicBlockInContext<T> N) {
    initForSucc(N);
    return g.getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getSuccNodeCount(BasicBlockInContext<T> N) {
    initForSucc(N);
    return g.getSuccNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node, com.ibm.wala.util.graph.Node)
   */
  @Override
  public void addEdge(BasicBlockInContext src, BasicBlockInContext dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEdge(BasicBlockInContext src, BasicBlockInContext dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeAllIncidentEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return g.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(BasicBlockInContext<T> N) {
    return g.containsNode(N);
  }

  /**
   * @param B
   * @return true iff basic block B ends in a call instuction
   */
  public boolean hasCall(BasicBlockInContext<T> B) {
    addNodeForBasicBlockIfNeeded(B);
    return hasCallVector.get(getNumber(B));
  }

  /**
   * @return true iff basic block B ends in a call instuction
   */
  protected boolean hasCall(BasicBlockInContext<T> B, ControlFlowGraph<SSAInstruction, T> cfg) {
    SSAInstruction[] statements = cfg.getInstructions();

    int lastIndex = B.getLastInstructionIndex();
    if (lastIndex >= 0) {

      if (statements.length <= lastIndex) {
        System.err.println(statements.length);
        System.err.println(cfg);
        assert lastIndex < statements.length : "bad BB " + B + " and CFG for " + getCGNode(B);
      }
      SSAInstruction last = statements[lastIndex];
      return (last instanceof SSAAbstractInvokeInstruction);
    } else {
      return false;
    }
  }

  /**
   * @param B
   * @return the set of CGNodes that B may call, according to the governing call graph.
   * @throws IllegalArgumentException if B is null
   */
  public Set<CGNode> getCallTargets(BasicBlockInContext<T> B) {
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    ControlFlowGraph<SSAInstruction, T> cfg = getCFG(B);
    return getCallTargets(B, cfg, getCGNode(B));
  }

  /**
   * @return the set of CGNodes that B may call, according to the governing call graph.
   */
  private Set<CGNode> getCallTargets(IBasicBlock<SSAInstruction> B, ControlFlowGraph<SSAInstruction, T> cfg, CGNode Bnode) {
    CallSiteReference site = getCallSiteForCallBlock(B, cfg);
    return cg.getPossibleTargets(Bnode, site);
  }

  /**
   * get the {@link CallSiteReference} corresponding to the last instruction in B (assumed to be a call)
   */
  protected CallSiteReference getCallSiteForCallBlock(IBasicBlock<SSAInstruction> B, ControlFlowGraph<SSAInstruction, T> cfg) {
    SSAInstruction[] statements = cfg.getInstructions();
    SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) statements[B.getLastInstructionIndex()];
    int pc = cfg.getProgramCounter(B.getLastInstructionIndex());
    CallSiteReference site = call.getCallSite();
    assert site.getProgramCounter() == pc;
    return site;
  }

  @Override
  public void removeIncomingEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeOutgoingEdges(BasicBlockInContext node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEdge(BasicBlockInContext<T> src, BasicBlockInContext<T> dst) {
    if (!addedSuccs.contains(getNumber(src))) {
      if (!src.getNode().equals(dst.getNode())) {
        if (src.getDelegate().isExitBlock()) {
          // checking for an exit to return edge
          CGNode callee = src.getNode();
          if (!cgNodesWithCallerEdges.contains(cg.getNumber(callee))) {
            CGNode caller = dst.getNode();
            T exitBlock = src.getDelegate();
            T entryBlock = getCFG(callee).entry();
            addEntryAndExitEdgesToCaller(callee, entryBlock, exitBlock, caller);
          }
        } else if (hasCall(src) && dst.getDelegate().isEntryBlock()) {
          // checking for a call to entry edge
          CGNode callee = dst.getNode();
          if (!cgNodesWithCallerEdges.contains(cg.getNumber(callee))) {
            CGNode caller = src.getNode();
            T entryBlock = dst.getDelegate();
            T exitBlock = getCFG(callee).exit();
            addEntryAndExitEdgesToCaller(callee, entryBlock, exitBlock, caller);
          }
        }
      } else {
        // if it exists, edge must be intraprocedural
        addIntraproceduralNodesAndEdgesForCGNodeIfNeeded(src.getNode());
      }
    }
    addedSuccs.add(getNumber(src));
    return g.hasEdge(src, dst);
  }

  @Override
  public int getNumber(BasicBlockInContext<T> N) {
    addNodeForBasicBlockIfNeeded(N);
    return g.getNumber(N);
  }

  @Override
  public BasicBlockInContext<T> getNode(int number) throws UnimplementedError {
    return g.getNode(number);
  }

  @Override
  public int getMaxNumber() {
    if (WARN_ON_EAGER_CONSTRUCTION) {
      System.err.println("WARNING: forcing full ICFG construction by calling getMaxNumber()");
    }
    if (FAIL_ON_EAGER_CONSTRUCTION) {
      throw new UnimplementedError();
    }
    constructFullGraph();
    return g.getMaxNumber();
  }

  @Override
  public Iterator<BasicBlockInContext<T>> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getSuccNodeNumbers(BasicBlockInContext<T> node) {
    initForSucc(node);
    return g.getSuccNodeNumbers(node);
  }

  @Override
  public IntSet getPredNodeNumbers(BasicBlockInContext<T> node) {
    initForPred(node);
    return g.getPredNodeNumbers(node);
  }

  public BasicBlockInContext<T> getEntry(CGNode n) {
    ControlFlowGraph<SSAInstruction, ? extends T> cfg = getCFG(n);
    if (cfg != null) {
      T entry = cfg.entry();
      return new BasicBlockInContext<>(n, entry);
    } else {
      return null;
    }
  }

  public BasicBlockInContext<T> getExit(CGNode n) {
    ControlFlowGraph<SSAInstruction, ? extends T> cfg = getCFG(n);
    T entry = cfg.exit();
    return new BasicBlockInContext<>(n, entry);
  }

  /**
   * @param callBlock node in the IPCFG that ends in a call
   * @return the nodes that are return sites for this call.
   * @throws IllegalArgumentException if bb is null
   */
  public Iterator<BasicBlockInContext<T>> getReturnSites(BasicBlockInContext<T> callBlock) {
    if (callBlock == null) {
      throw new IllegalArgumentException("bb is null");
    }
    final CGNode node = callBlock.getNode();

    // a successor node is a return site if it is in the same
    // procedure, and is not the entry() node.
    Predicate<BasicBlockInContext> isReturn = other -> !other.isEntryBlock() && node.equals(other.getNode());
    return new FilterIterator<>(getSuccNodes(callBlock), isReturn);
  }

  /**
   * get the basic blocks which are call sites that may call callee and return to returnBlock if callee is null, answer return sites
   * for which no callee was found.
   */
  public Iterator<BasicBlockInContext<T>> getCallSites(BasicBlockInContext<T> returnBlock, final CGNode callee) {
    if (returnBlock == null) {
      throw new IllegalArgumentException("bb is null");
    }
    final ControlFlowGraph<SSAInstruction, T> cfg = getCFG(returnBlock);
    Iterator<? extends T> it = cfg.getPredNodes(returnBlock.getDelegate());
    final CGNode node = returnBlock.getNode();

    Predicate<T> dispatchFilter = callBlock -> {
      BasicBlockInContext<T> bb = new BasicBlockInContext<>(node, callBlock);
      if (!hasCall(bb, cfg)) {
        return false;
      }
      if (callee != null) {
        return getCallTargets(bb).contains(callee);
      } else {
        return getCallTargets(bb).isEmpty();
      }
    };
    it = new FilterIterator<T>(it, dispatchFilter);

    Function<T, BasicBlockInContext<T>> toContext = object -> {
      T b = object;
      return new BasicBlockInContext<>(node, b);
    };
    MapIterator<T, BasicBlockInContext<T>> m = new MapIterator<>(it, toContext);
    return new FilterIterator<>(m, isCall);
  }

  private final Predicate<BasicBlockInContext<T>> isCall = this::hasCall;

  public boolean isReturn(BasicBlockInContext<T> bb) throws IllegalArgumentException {
    if (bb == null) {
      throw new IllegalArgumentException("bb == null");
    }
    ControlFlowGraph<SSAInstruction, T> cfg = getCFG(bb);
    for (T b : Iterator2Iterable.make(cfg.getPredNodes(bb.getDelegate()))) {
      if (hasCall(new BasicBlockInContext<>(bb.getNode(), b))) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the governing {@link CallGraph} used to build this ICFG
   */
  public CallGraph getCallGraph() {
    return cg;
  }
}
