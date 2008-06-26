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
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IndiscriminateFilter;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.viz.IFDSExplorer;

/**
 * 
 * Interprocedural control-flow graph.
 * 
 * TODO: think about a better implementation; perhaps a lazy view of the constituent CFGs Lots of ways this can be
 * optimized?
 * 
 * @author sfink
 * @author Julian Dolby
 */
public abstract class AbstractInterproceduralCFG<T extends ISSABasicBlock> implements NumberedGraph<BasicBlockInContext<T>> {

  private static final int DEBUG_LEVEL = 0;

  /**
   * Should the graph include call-to-return edges? When set to <code>false</code>, the graphs output by
   * {@link IFDSExplorer} look incorrect
   */
  private final static boolean CALL_TO_RETURN_EDGES = true;

  /**
   * Graph implementation we delegate to.
   */
  final private NumberedGraph<BasicBlockInContext<T>> g = new SlowSparseNumberedGraph<BasicBlockInContext<T>>(2);

  /**
   * Governing call graph
   */
  private final CallGraph cg;

  /**
   * Filter that determines relevant call graph nodes
   */
  private final Filter<CGNode> relevant;

  /**
   * a cache: for each node (Basic Block), does that block end in a call?
   */
  private final BitVector hasCallVector = new BitVector();

  protected abstract ControlFlowGraph<T> getCFG(CGNode n);

  /**
   * Build an Interprocedural CFG from a call graph. This version defaults to using whatever CFGs the call graph
   * provides by default, and includes all nodes in the call graph.
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
   * @param relevant a filter which accepts those call graph nodes which should be included in the I-CFG. Other nodes
   *        are ignored.
   */
  public AbstractInterproceduralCFG(CallGraph CG, Filter<CGNode> relevant) {

    this.cg = CG;
    this.relevant = relevant;

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
  }

  /**
   * create the edges that define this graph.
   */
  private void createEdges() {
    for (Iterator ns = cg.iterator(); ns.hasNext();) {
      CGNode n = (CGNode) ns.next();
      if (relevant.accepts(n)) {
        // retrieve a cfg for node n.
        ControlFlowGraph<T> cfg = getCFG(n);
        if (cfg == null) {
          // n is an unmodelled native method
          continue;
        }
        IInstruction[] instrs = cfg.getInstructions();
        // create edges for node n.
        for (Iterator<T> bbs = cfg.iterator(); bbs.hasNext();) {
          T bb = bbs.next();
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
          System.err.println("Found a relevant node: " + n);
        }

        // retrieve a cfg for node n.
        ControlFlowGraph<? extends T> cfg = getCFG(n);
        if (cfg != null) {
          // create a node for each basic block.
          addNodeForEachBasicBlock(cfg, n);
        }
      }
    }
  }

  /**
   * Add edges to the IPCFG for the incoming edges incident on a basic block bb
   * 
   * @param n a call graph node
   * @param cfg the CFG for n
   * @param instrs the instructions for node n
   * @param bb a basic block in the CFG
   */
  private void addEdgesToNonEntryBlock(CGNode n, ControlFlowGraph<T> cfg, IInstruction[] instrs, T bb) {

    if (DEBUG_LEVEL > 0) {
      System.err.println("addEdgesToNonEntryBlock: " + bb);
      System.err.println("nPred: " + cfg.getPredNodeCount(bb));
    }

    for (Iterator<? extends T> ps = cfg.getPredNodes(bb); ps.hasNext();) {
      T pb = ps.next();
      if (DEBUG_LEVEL > 0) {
        System.err.println("Consider previous block: " + pb);
      }

      if (pb.getLastInstructionIndex() < 0) {
        // pb is the entry block for a cfg with
        // no instructions.
        BasicBlockInContext<T> p = new BasicBlockInContext<T>(n, pb);
        BasicBlockInContext<T> b = new BasicBlockInContext<T>(n, bb);
        g.addEdge(p, b);
        continue;
      }

      int index = pb.getLastInstructionIndex();
      IInstruction inst = instrs[index];
      if (DEBUG_LEVEL > 0) {
        System.err.println("Last instruction is : " + inst);
      }
      if (inst instanceof IInvokeInstruction) {
        // a previous instruction is a call instruction. If necessary,
        // add an edge from the exit() of each target of the call.
        IInvokeInstruction call = (IInvokeInstruction) inst;
        CallSiteReference site = makeCallSiteReference(n.getMethod().getDeclaringClass().getClassLoader().getReference(), cfg
            .getProgramCounter(index), call);
        if (DEBUG_LEVEL > 0) {
          System.err.println("got Site: " + site);
        }
        boolean irrelevantTargets = false;
        for (Iterator ts = cg.getPossibleTargets(n, site).iterator(); ts.hasNext();) {
          CGNode tn = (CGNode) ts.next();
          if (!relevant.accepts(tn)) {
            if (DEBUG_LEVEL > 0) {
              System.err.println("Irrelevant target: " + tn);
            }
            irrelevantTargets = true;
            continue;
          }

          if (DEBUG_LEVEL > 0) {
            System.err.println("Relevant target: " + tn);
          }
          // add an edge from tn exit to this node
          ControlFlowGraph<? extends T> tcfg = getCFG(tn);
          // tcfg might be null if tn is an unmodelled native method
          if (tcfg != null) {
            addEdgesFromExitToReturn(n, bb, tn, tcfg);
          }
        }

        if (irrelevantTargets || CALL_TO_RETURN_EDGES) {
          // at least one call target was ignored. So add a "normal" edge
          // from the predecessor block to this block.
          BasicBlockInContext<T> p = new BasicBlockInContext<T>(n, pb);
          BasicBlockInContext<T> b = new BasicBlockInContext<T>(n, bb);
          g.addEdge(p, b);
        }
      } else {
        // previous instruction is not a call instruction.
        BasicBlockInContext<T> p = new BasicBlockInContext<T>(n, pb);
        BasicBlockInContext<T> b = new BasicBlockInContext<T>(n, bb);
        if (Assertions.verifyAssertions) {
          if (!g.containsNode(p) || !g.containsNode(b)) {
            Assertions._assert(g.containsNode(p), "IPCFG does not contain " + p);
            Assertions._assert(g.containsNode(b), "IPCFG does not contain " + b);
          }
        }
        g.addEdge(p, b);
      }
    }
  }

  public static CallSiteReference makeCallSiteReference(ClassLoaderReference loader, int pc, IInvokeInstruction call)
      throws IllegalArgumentException, IllegalArgumentException {

    if (call == null) {
      throw new IllegalArgumentException("call == null");
    }
    if (!(call instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction)
        && !(call instanceof com.ibm.wala.shrikeBT.InvokeInstruction)) {
      throw new IllegalArgumentException(
          "(not ( call instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction ) ) and (not ( call instanceof com.ibm.wala.shrikeBT.InvokeInstruction ) )");
    }
    CallSiteReference site = null;
    if (call instanceof InvokeInstruction) {
      InvokeInstruction c = (InvokeInstruction) call;
      site = CallSiteReference.make(pc, MethodReference.findOrCreate(loader, c.getClassType(), c.getMethodName(), c
          .getMethodSignature()), call.getInvocationCode());
    } else {
      com.ibm.wala.ssa.SSAAbstractInvokeInstruction c = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) call;
      site = CallSiteReference.make(pc, c.getDeclaredTarget(), call.getInvocationCode());
    }
    return site;
  }

  /**
   * Add an edge from the exit() block of a callee to a return site in the caller
   * 
   * @param returnBlock the return site for a call
   * @param targetCFG the called method
   */
  private void addEdgesFromExitToReturn(CGNode caller, T returnBlock, CGNode target, ControlFlowGraph<? extends T> targetCFG) {
    T texit = targetCFG.exit();
    BasicBlockInContext<T> exit = new BasicBlockInContext<T>(target, texit);
    BasicBlockInContext<T> ret = new BasicBlockInContext<T>(caller, returnBlock);
    if (Assertions.verifyAssertions) {
      if (!g.containsNode(exit) || !g.containsNode(ret)) {
        Assertions._assert(g.containsNode(exit), "IPCFG does not contain " + exit);
        Assertions._assert(g.containsNode(ret), "IPCFG does not contain " + ret);
      }
    }
    if (DEBUG_LEVEL > 0) {
      System.err.println("addEdgeFromExitToReturn " + exit + ret);
    }
    g.addEdge(exit, ret);
  }

  /**
   * Add the incoming edges to the entry() block for a call graph node.
   * 
   * @param n a node in the call graph
   * @param bb the entry() block for n
   */
  private void addEdgesToEntryBlock(CGNode n, T bb) {

    if (DEBUG_LEVEL > 0) {
      System.err.println("addEdgesToEntryBlock " + bb);
    }

    for (Iterator callers = cg.getPredNodes(n); callers.hasNext();) {
      CGNode caller = (CGNode) callers.next();
      if (DEBUG_LEVEL > 0) {
        System.err.println("got caller " + caller);
      }
      if (relevant.accepts(caller)) {
        if (DEBUG_LEVEL > 0) {
          System.err.println("caller " + caller + "is relevant");
        }
        ControlFlowGraph<? extends T> ccfg = getCFG(caller);
        IInstruction[] cinsts = ccfg.getInstructions();

        if (DEBUG_LEVEL > 0) {
          System.err.println("Visiting " + cinsts.length + " instructions");
        }
        for (int i = 0; i < cinsts.length; i++) {
          if (cinsts[i] instanceof IInvokeInstruction) {
            if (DEBUG_LEVEL > 0) {
              System.err.println("Checking invokeinstruction: " + cinsts[i]);
            }
            IInvokeInstruction call = (IInvokeInstruction) cinsts[i];
            CallSiteReference site = makeCallSiteReference(n.getMethod().getDeclaringClass().getClassLoader().getReference(), ccfg
                .getProgramCounter(i), call);
            if (cg.getPossibleTargets(caller, site).contains(n)) {
              if (DEBUG_LEVEL > 0) {
                System.err.println("Adding edge " + ccfg.getBlockForInstruction(i) + " to " + bb);
              }
              T callerBB = ccfg.getBlockForInstruction(i);
              BasicBlockInContext<T> b1 = new BasicBlockInContext<T>(caller, callerBB);
              BasicBlockInContext<T> b2 = new BasicBlockInContext<T>(n, bb);
              g.addEdge(b1, b2);
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
  private void addNodeForEachBasicBlock(ControlFlowGraph<? extends T> cfg, CGNode N) {
    for (Iterator<? extends T> bbs = cfg.iterator(); bbs.hasNext();) {
      T bb = bbs.next();
      if (DEBUG_LEVEL > 0) {
        System.err.println("IPCFG Add basic block " + bb);
      }
      BasicBlockInContext<T> b = new BasicBlockInContext<T>(N, bb);
      g.addNode(b);
      if (hasCall(b, cfg)) {
        hasCallVector.set(getNumber(b));
      }
    }
  }

  /**
   * @return the original CFG from whence B came
   * @throws IllegalArgumentException if B == null
   */
  public ControlFlowGraph<T> getCFG(BasicBlockInContext B) throws IllegalArgumentException {
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
  public void removeNodeAndEdges(BasicBlockInContext N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator<BasicBlockInContext<T>> iterator() {
    return g.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return g.getNumberOfNodes();
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
  public Iterator<? extends BasicBlockInContext<T>> getPredNodes(BasicBlockInContext<T> N) {
    return g.getPredNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(BasicBlockInContext<T> N) {
    return g.getPredNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<? extends BasicBlockInContext<T>> getSuccNodes(BasicBlockInContext<T> N) {
    return g.getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(BasicBlockInContext<T> N) {
    return g.getSuccNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node, com.ibm.wala.util.graph.Node)
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
    return g.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(BasicBlockInContext<T> N) {
    return g.containsNode(N);
  }

  /**
   * @param B
   * @return true iff basic block B ends in a call instuction
   */
  public boolean hasCall(BasicBlockInContext<T> B) {
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
   * @return the set of CGNodes that B may call, according to the governing call graph.
   * @throws IllegalArgumentException if B is null
   */
  public Set<CGNode> getCallTargets(BasicBlockInContext B) {
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    ControlFlowGraph cfg = getCFG(B);
    return getCallTargets(B, cfg, getCGNode(B));
  }

  /**
   * @return the set of CGNodes that B may call, according to the governing call graph.
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

  public boolean hasEdge(BasicBlockInContext<T> src, BasicBlockInContext<T> dst) {
    return g.hasEdge(src, dst);
  }

  public int getNumber(BasicBlockInContext<T> N) {
    return g.getNumber(N);
  }

  public BasicBlockInContext<T> getNode(int number) throws UnimplementedError {
    return g.getNode(number);
  }

  public int getMaxNumber() {
    return g.getMaxNumber();
  }

  public Iterator<BasicBlockInContext<T>> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(BasicBlockInContext<T> node) {
    return g.getSuccNodeNumbers(node);
  }

  public IntSet getPredNodeNumbers(BasicBlockInContext<T> node) {
    return g.getPredNodeNumbers(node);
  }

  public BasicBlockInContext<T> getEntry(CGNode n) {
    ControlFlowGraph<? extends T> cfg = getCFG(n);
    T entry = cfg.entry();
    return new BasicBlockInContext<T>(n, entry);
  }

  public BasicBlockInContext<T> getExit(CGNode n) {
    ControlFlowGraph<? extends T> cfg = getCFG(n);
    T entry = cfg.exit();
    return new BasicBlockInContext<T>(n, entry);
  }

  /**
   * @param callBlock node in the IPCFG that ends in a call
   * @return the nodes that are return sites for this call.
   * @throws IllegalArgumentException if bb is null
   */
  public Iterator<BasicBlockInContext> getReturnSites(BasicBlockInContext<T> callBlock) {
    if (callBlock == null) {
      throw new IllegalArgumentException("bb is null");
    }
    final CGNode node = callBlock.getNode();

    // a successor node is a return site if it is in the same
    // procedure, and is not the entry() node.
    Filter isReturn = new Filter() {
      public boolean accepts(Object o) {
        BasicBlockInContext other = (BasicBlockInContext) o;
        return !other.isEntryBlock() && node.equals(other.getNode());
      }
    };
    return new FilterIterator<BasicBlockInContext>(getSuccNodes(callBlock), isReturn);
  }

  /**
   * get the basic blocks which are call sites that may call callee and return to returnBlock if callee is null, answer
   * return sites for which no callee was found.
   */
  public Iterator<BasicBlockInContext<T>> getCallSites(BasicBlockInContext<T> returnBlock, final CGNode callee) {
    if (returnBlock == null) {
      throw new IllegalArgumentException("bb is null");
    }
    final ControlFlowGraph<T> cfg = getCFG(returnBlock);
    Iterator<? extends T> it = cfg.getPredNodes(returnBlock.getDelegate());
    final CGNode node = returnBlock.getNode();

    Filter<? extends T> dispatchFilter = new Filter<T>() {
      public boolean accepts(T callBlock) {
        BasicBlockInContext<T> bb = new BasicBlockInContext<T>(node, callBlock);
        if (!hasCall(bb, cfg)) {
          return false;
        }
        if (callee != null) {
          return getCallTargets(bb).contains(callee);
        } else {
          return getCallTargets(bb).isEmpty();
        }
      }
    };
    it = new FilterIterator<T>(it, dispatchFilter);

    Function<T, BasicBlockInContext<T>> toContext = new Function<T, BasicBlockInContext<T>>() {
      public BasicBlockInContext<T> apply(T object) {
        T b = object;
        return new BasicBlockInContext<T>(node, b);
      }
    };
    MapIterator<T, BasicBlockInContext<T>> m = new MapIterator<T, BasicBlockInContext<T>>(it, toContext);
    return new FilterIterator<BasicBlockInContext<T>>(m, isCall);
  }

  private final Filter<BasicBlockInContext<T>> isCall = new Filter<BasicBlockInContext<T>>() {
    public boolean accepts(BasicBlockInContext<T> o) {
      return hasCall(o);
    }
  };

  public boolean isReturn(BasicBlockInContext<T> bb) throws IllegalArgumentException {
    if (bb == null) {
      throw new IllegalArgumentException("bb == null");
    }
    ControlFlowGraph<T> cfg = getCFG(bb);
    for (Iterator<? extends T> it = cfg.getPredNodes(bb.getDelegate()); it.hasNext();) {
      T b = it.next();
      if (hasCall(new BasicBlockInContext<T>(bb.getNode(), b))) {
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
