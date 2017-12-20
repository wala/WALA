/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.demandpa.flowgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * The nodes in this graph are PointerKeys corresponding to local variables and static fields, InstanceKeys, and FieldRefs (see
 * below).
 * 
 * This graph is constructed on-demand during a traversal.
 * 
 * The edges represent
 * <ul>
 * <li>flow from local -&gt; local representing assignment (i.e. phi,pi)
 * <li>flow from instancekey -&gt; local for news
 * <li>flow from formal -&gt; actual parameter
 * <li>flow from return value -&gt; local
 * <li>match edges
 * <li>local -&gt; local edges representing loads/stores (e.g. x = y.f will have a edge x-&gt;y, labelled with f) for a getstatic x = Y.f,
 * we have an edge from x -&gt; Y.f.
 * </ul>
 * 
 * N.B: Edges go OPPOSITE the flow of values.
 * 
 * Edges carry labels if they arise from loads/stores, or calls
 */
public class SimpleDemandPointerFlowGraph extends SlowSparseNumberedGraph<Object> {

  private static final long serialVersionUID = 5208052568163692029L;

  private final static boolean DEBUG = false;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = 0;

  private final CallGraph cg;

  private final HeapModel heapModel;

  private final MemoryAccessMap fam;

  private final IClassHierarchy cha;

  /**
   * node numbers of CGNodes we have already visited
   */
  final BitVectorIntSet cgNodesVisited = new BitVectorIntSet();

  /**
   * Map: LocalPointerKey -&gt; IField. if we have (x,f), that means x was def'fed by a getfield on f.
   */
  final Map<PointerKey, IField> getFieldDefs = HashMapFactory.make();

  final Collection<PointerKey> arrayDefs = HashSetFactory.make();

  /**
   * Map: LocalPointerKey -&gt; SSAInvokeInstruction. If we have (x, foo()), that means that x was def'fed by the return value from a
   * call to foo()
   */
  final Map<PointerKey, SSAInvokeInstruction> callDefs = HashMapFactory.make();

  /**
   * Map: LocalPointerKey -&gt; CGNode. If we have (x, foo), then x is a parameter of method foo. For now, we have to re-discover the
   * parameter position.
   */
  final Map<PointerKey, CGNode> params = HashMapFactory.make();

  public SimpleDemandPointerFlowGraph(CallGraph cg, HeapModel heapModel, MemoryAccessMap fam, IClassHierarchy cha) {
    super();
    if (cg == null) {
      throw new IllegalArgumentException("null cg");
    }
    this.cg = cg;
    this.heapModel = heapModel;
    this.fam = fam;
    this.cha = cha;
  }

  public void addSubgraphForNode(CGNode node) {
    int n = cg.getNumber(node);
    if (!cgNodesVisited.contains(n)) {
      cgNodesVisited.add(n);
      unconditionallyAddConstraintsFromNode(node);
      addNodesForParameters(node);
    }
  }

  /**
   * add nodes for parameters and return values
   * 
   * @param node
   */
  private void addNodesForParameters(CGNode node) {
    // TODO Auto-generated method stub
    IR ir = node.getIR();
    TypeInference ti = TypeInference.make(ir, false);
    SymbolTable symbolTable = ir.getSymbolTable();
    for (int i = 0; i < symbolTable.getNumberOfParameters(); i++) {
      int parameter = symbolTable.getParameter(i);
      TypeAbstraction t = ti.getType(parameter);
      if (t != null) {
        PointerKey paramPk = heapModel.getPointerKeyForLocal(node, parameter);
        addNode(paramPk);
        params.put(paramPk, node);
      }
    }
    addNode(heapModel.getPointerKeyForReturnValue(node));
    addNode(heapModel.getPointerKeyForExceptionalReturnValue(node));
  }

  /**
   * @return Returns the heapModel.
   */
  protected HeapModel getHeapModel() {
    return heapModel;
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractNumberedGraph#getPredNodeNumbers(java.lang.Object)
   */
  @Override
  public IntSet getPredNodeNumbers(Object node) throws UnimplementedError {
    if (node instanceof StaticFieldKey) {
      Assertions.UNREACHABLE();
      return null;
    } else {
      return super.getPredNodeNumbers(node);
    }
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractNumberedGraph#getSuccNodeNumbers(java.lang.Object)
   */
  @Override
  public IntSet getSuccNodeNumbers(Object node) throws IllegalArgumentException {
    if (node instanceof com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey) {
      throw new IllegalArgumentException("node instanceof com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey");
    }
    return super.getSuccNodeNumbers(node);
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractGraph#getPredNodeCount(java.lang.Object)
   */
  @Override
  public int getPredNodeCount(Object N) throws UnimplementedError {
    if (N instanceof StaticFieldKey) {
      Assertions.UNREACHABLE();
      return -1;
    } else {
      return super.getPredNodeCount(N);
    }
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractGraph#getPredNodes(java.lang.Object)
   */
  @Override
  public Iterator<Object> getPredNodes(Object N) throws IllegalArgumentException {
    if (N instanceof com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey) {
      throw new IllegalArgumentException("N instanceof com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey");
    }
    return super.getPredNodes(N);
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractGraph#getSuccNodeCount(java.lang.Object)
   */
  @Override
  public int getSuccNodeCount(Object N) throws UnimplementedError {
    if (N instanceof StaticFieldKey) {
      Assertions.UNREACHABLE();
      return -1;
    } else {
      return super.getSuccNodeCount(N);
    }
  }

  /*
   * @see com.ibm.capa.util.graph.AbstractGraph#getSuccNodes(java.lang.Object)
   */
  @Override
  public Iterator<Object> getSuccNodes(Object N) {
    if (N instanceof StaticFieldKey) {
      addNodesThatWriteToStaticField(((StaticFieldKey) N).getField());
    } else {
      IField f = getFieldDefs.get(N);
      if (f != null) {
        addMatchEdges((LocalPointerKey) N, f);
      } else {
        SSAInvokeInstruction callInstr = callDefs.get(N);
        if (callInstr != null) {
          addReturnEdges((LocalPointerKey) N, callInstr);
        } else {
          CGNode node = params.get(N);
          if (node != null) {
            addParamEdges((LocalPointerKey) N, node);
          } else {
            if (arrayDefs.contains(N)) {
              addArrayMatchEdges((LocalPointerKey) N);
            }
          }
        }
      }
    }
    return super.getSuccNodes(N);
  }

  private void addArrayMatchEdges(LocalPointerKey pk) {
    Collection<MemoryAccess> arrayWrites = fam.getArrayWrites(null);
    for (MemoryAccess a : arrayWrites) {
      addSubgraphForNode(a.getNode());
    }
    for (MemoryAccess a : arrayWrites) {
      IR ir = a.getNode().getIR();
      SSAArrayStoreInstruction s = (SSAArrayStoreInstruction) ir.getInstructions()[a.getInstructionIndex()];
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getValue());
      assert containsNode(r);
      assert containsNode(pk);
      addMatchEdge(pk, r);
    }
  }

  private void addParamEdges(LocalPointerKey pk, CGNode node) {
    // get parameter position: value number - 1?
    int paramPos = pk.getValueNumber() - 1;
    // iterate over callers
    for (CGNode caller : cg) {
      // TODO we don't need to add the graph if null is passed
      // as the argument
      addSubgraphForNode(caller);
      IR ir = caller.getIR();
      for (CallSiteReference call : Iterator2Iterable.make(ir.iterateCallSites())) {
        if (cg.getPossibleTargets(caller, call).contains(node)) {
          SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
          for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
            PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
            assert containsNode(actualPk);
            assert containsNode(pk);
            addEdge(pk, actualPk);

          }
        }
      }
    }
  }

  /**
   * @param pk value being def'fed by a call instruction (either normal or exceptional)
   */
  private void addReturnEdges(LocalPointerKey pk, SSAInvokeInstruction callInstr) {
    boolean isExceptional = pk.getValueNumber() == callInstr.getException();

    // get call targets
    Collection<CGNode> possibleCallees = cg.getPossibleTargets(pk.getNode(), callInstr.getCallSite());
    // construct graph for each target
    for (CGNode callee : possibleCallees) {
      addSubgraphForNode(callee);
      PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
          .getPointerKeyForReturnValue(callee);
      assert containsNode(retVal);
      addEdge(pk, retVal);
    }
  }

  private void addMatchEdges(LocalPointerKey pk, IField f) {
    Collection<MemoryAccess> fieldWrites = fam.getFieldWrites(null, f);
    addMatchHelper(pk, fieldWrites);
  }

  private void addMatchHelper(LocalPointerKey pk, Collection<MemoryAccess> writes) {
    for (MemoryAccess a : writes) {
      addSubgraphForNode(a.getNode());
    }
    for (MemoryAccess a : writes) {
      IR ir = a.getNode().getIR();
      SSAPutInstruction s = (SSAPutInstruction) ir.getInstructions()[a.getInstructionIndex()];
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getVal());
      assert containsNode(r);
      assert containsNode(pk);
      addMatchEdge(pk, r);
    }
  }

  private void addMatchEdge(LocalPointerKey pk, PointerKey r) {
    addEdge(pk, r);
  }

  private void addNodesThatWriteToStaticField(IField field) {
    Collection<MemoryAccess> fieldWrites = fam.getStaticFieldWrites(field);
    for (MemoryAccess a : fieldWrites) {
      addSubgraphForNode(a.getNode());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.capa.util.graph.AbstractGraph#hasEdge(java.lang.Object, java.lang.Object)
   */
  public boolean hasEdge(PointerKey src, PointerKey dst) {
    // TODO Auto-generated method stub
    return super.hasEdge(src, dst);
  }

  protected void unconditionallyAddConstraintsFromNode(CGNode node) {

    if (DEBUG) {
      System.err.println(("Visiting CGNode " + node));
    }

    if (SSAPropagationCallGraphBuilder.PERIODIC_WIPE_SOFT_CACHES) {
      wipeCount++;
      if (wipeCount >= SSAPropagationCallGraphBuilder.WIPE_SOFT_CACHE_INTERVAL) {
        wipeCount = 0;
        ReferenceCleanser.clearSoftCaches();
      }
    }

    IR ir = node.getIR();
    debugPrintIR(ir);

    if (ir == null) {
      return;
    }

    DefUse du = node.getDU();
    addNodeInstructionConstraints(node, ir, du);
    addNodePassthruExceptionConstraints(node, ir);
  }

  /**
   * Add constraints to represent the flow of exceptions to the exceptional return value for this node
   */
  protected void addNodePassthruExceptionConstraints(CGNode node, IR ir) {
    // add constraints relating to thrown exceptions that reach the exit block.
    List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, ir.getExitBlock());
    PointerKey exception = heapModel.getPointerKeyForExceptionalReturnValue(node);
    IClass c = node.getClassHierarchy().lookupClass(TypeReference.JavaLangThrowable);

    addExceptionDefConstraints(ir, node, peis, exception, Collections.singleton(c));
  }

  /**
   * Generate constraints which assign exception values into an exception pointer
   * 
   * @param node governing node
   * @param peis list of PEI instructions
   * @param exceptionVar PointerKey representing a pointer to an exception value
   * @param catchClasses the types "caught" by the exceptionVar
   */
  private void addExceptionDefConstraints(IR ir, CGNode node, List<ProgramCounter> peis, PointerKey exceptionVar,
      Set<IClass> catchClasses) {
    for (ProgramCounter peiLoc : peis) {
      SSAInstruction pei = ir.getPEI(peiLoc);

      if (pei instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction s = (SSAAbstractInvokeInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e);

      } else if (pei instanceof SSAAbstractThrowInstruction) {
        SSAAbstractThrowInstruction s = (SSAAbstractThrowInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e);
      }

      // Account for those exceptions for which we do not actually have a
      // points-to set for
      // the pei, but just instance keys
      Collection<TypeReference> types = pei.getExceptionTypes();
      if (types != null) {
        for (TypeReference type : types) {
          if (type != null) {
            InstanceKey ik = heapModel.getInstanceKeyForPEI(node, peiLoc, type);
            assert ik instanceof ConcreteTypeKey : "uh oh: need to implement getCaughtException constraints for instance " + ik;
            ConcreteTypeKey ck = (ConcreteTypeKey) ik;
            IClass klass = ck.getType();
            if (PropagationCallGraphBuilder.catches(catchClasses, klass, cha)) {
              addNode(exceptionVar);
              addNode(ik);
              addEdge(exceptionVar, ik);
            }
          }
        }
      }
    }
  }

  /**
   * Add pointer flow constraints based on instructions in a given node
   */
  protected void addNodeInstructionConstraints(CGNode node, IR ir, DefUse du) {
    StatementVisitor v = makeVisitor((ExplicitCallGraph.ExplicitNode) node, ir, du);
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
    for (ISSABasicBlock b : cfg) {
      addBlockInstructionConstraints(node, cfg, b, v);
    }
  }

  /**
   * Add constraints for a particular basic block.
   */
  protected void addBlockInstructionConstraints(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg,
      ISSABasicBlock b, StatementVisitor v) {
    v.setBasicBlock(b);

    // visit each instruction in the basic block.
    for (SSAInstruction s : b) {
      if (s != null) {
        s.visit(v);
      }
    }

    addPhiConstraints(node, cfg, b);
  }

  private void addPhiConstraints(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, ISSABasicBlock b) {

    // visit each phi instruction in each successor block
    for (ISSABasicBlock sb : Iterator2Iterable.make(cfg.getSuccNodes(b))) {
      if (sb.isExitBlock()) {
        // an optimization based on invariant that exit blocks should have no
        // phis.
        continue;
      }
      int n = 0;
      // set n to be whichPred(this, sb);
      for (IBasicBlock back : Iterator2Iterable.make(cfg.getPredNodes(sb))) {
        if (back == b) {
          break;
        }
        ++n;
      }
      assert n < cfg.getPredNodeCount(sb);
      for (SSAPhiInstruction phi : Iterator2Iterable.make(sb.iteratePhis())) {
        if (phi == null) {
          continue;
        }
        PointerKey def = heapModel.getPointerKeyForLocal(node, phi.getDef());
        if (phi.getUse(n) > 0) {
          PointerKey use = heapModel.getPointerKeyForLocal(node, phi.getUse(n));
          addNode(def);
          addNode(use);
          addEdge(def, use);
        }
        // }
        // }
      }
    }
  }

  protected StatementVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node, IR ir, DefUse du) {
    return new StatementVisitor(node, ir, du);
  }

  private static void debugPrintIR(IR ir) {
    if (DEBUG) {
      if (ir == null) {
        System.err.println("\n   No statements\n");
      } else {
        try {
          System.err.println(ir.toString());
        } catch (Error e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * A visitor that generates graph nodes and edges for an IR.
   * 
   * strategy: when visiting a statement, for each use of that statement, add a graph edge from def to use.
   * 
   * TODO: special treatment for parameter passing, etc.
   */
  protected class StatementVisitor extends SSAInstruction.Visitor {

    /**
     * The node whose statements we are currently traversing
     */
    protected final CGNode node;

    /**
     * The governing IR
     */
    protected final IR ir;

    /**
     * The basic block currently being processed
     */
    private ISSABasicBlock basicBlock;

    /**
     * Governing symbol table
     */
    protected final SymbolTable symbolTable;

    /**
     * Def-use information
     */
    protected final DefUse du;

    public StatementVisitor(CGNode node, IR ir, DefUse du) {
      this.node = node;
      this.ir = ir;
      this.symbolTable = ir.getSymbolTable();
      assert symbolTable != null;
      this.du = du;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitArrayLoad(com.ibm.domo.ssa.SSAArrayLoadInstruction)
     */
    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      // skip arrays of primitive type
      if (instruction.typeIsPrimitive()) {
        return;
      }
      PointerKey result = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      arrayDefs.add(result);
      // PointerKey arrayRef = getPointerKeyForLocal(node,
      // instruction.getArrayRef());
      // if (hasNoInterestingUses(instruction.getDef(), du)) {
      // system.recordImplicitPointsToSet(result);
      // } else {
      // if (contentsAreInvariant(symbolTable, du, instruction.getArrayRef())) {
      // system.recordImplicitPointsToSet(arrayRef);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getArrayRef(),
      // SSAPropagationCallGraphBuilder.this);
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // PointerKey p = getPointerKeyForArrayContents(ik[i]);
      // if (p == null) {
      // getWarnings().add(ResolutionFailure.create(node,
      // ik[i].getConcreteType()));
      // } else {
      // system.newConstraint(result, assignOperator, p);
      // }
      // }
      // } else {
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(!system.isUnified(result));
      // Assertions._assert(!system.isUnified(arrayRef));
      // }
      // system.newSideEffect(new
      // ArrayLoadOperator(system.findOrCreatePointsToSet(result)), arrayRef);
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitArrayStore(com.ibm.domo.ssa.SSAArrayStoreInstruction)
     */
    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
      // Assertions.UNREACHABLE();
      // skip arrays of primitive type
      if (instruction.typeIsPrimitive()) {
        return;
      }
      // make node for used value
      PointerKey value = heapModel.getPointerKeyForLocal(node, instruction.getValue());
      addNode(value);
      //
      // // (requires the creation of assign constraints as
      // // the set points-to(a[]) grows.)
      // PointerKey arrayRef = getPointerKeyForLocal(node,
      // instruction.getArrayRef());
      // // if (!supportFullPointerFlowGraph &&
      // // contentsAreInvariant(instruction.getArrayRef())) {
      // if (contentsAreInvariant(symbolTable, du, instruction.getArrayRef())) {
      // system.recordImplicitPointsToSet(arrayRef);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getArrayRef(),
      // SSAPropagationCallGraphBuilder.this);
      //
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // PointerKey p = getPointerKeyForArrayContents(ik[i]);
      // IClass contents = ((ArrayClass)
      // ik[i].getConcreteType()).getElementClass();
      // if (p == null) {
      // getWarnings().add(ResolutionFailure.create(node,
      // ik[i].getConcreteType()));
      // } else {
      // if (DEBUG_TRACK_INSTANCE) {
      // if (system.findOrCreateIndexForInstanceKey(ik[i]) ==
      // DEBUG_INSTANCE_KEY) {
      // Assertions.UNREACHABLE();
      // }
      // }
      // if (contentsAreInvariant(symbolTable, du, instruction.getValue())) {
      // system.recordImplicitPointsToSet(value);
      // InstanceKey[] vk = getInvariantContents(symbolTable, du, node,
      // instruction.getValue(),
      // SSAPropagationCallGraphBuilder.this);
      // for (int j = 0; j < vk.length; j++) {
      // system.findOrCreateIndexForInstanceKey(vk[j]);
      // if (vk[j].getConcreteType() != null) {
      // if (contents.isInterface()) {
      // if (getClassHierarchy().implementsInterface(vk[j].getConcreteType(),
      // contents.getReference())) {
      // system.newConstraint(p, vk[j]);
      // }
      // } else {
      // if (getClassHierarchy().isSubclassOf(vk[j].getConcreteType(),
      // contents)) {
      // system.newConstraint(p, vk[j]);
      // }
      // }
      // }
      // }
      // } else {
      // if (isRootType(contents)) {
      // system.newConstraint(p, assignOperator, value);
      // } else {
      // system.newConstraint(p, filterOperator, value);
      // }
      // }
      // }
      // }
      // } else {
      // if (contentsAreInvariant(symbolTable, du, instruction.getValue())) {
      // system.recordImplicitPointsToSet(value);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getValue(),
      // SSAPropagationCallGraphBuilder.this);
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(!system.isUnified(arrayRef));
      // }
      // system.newSideEffect(new InstanceArrayStoreOperator(ik[i]), arrayRef);
      // }
      // } else {
      // system.newSideEffect(new
      // ArrayStoreOperator(system.findOrCreatePointsToSet(value)), arrayRef);
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitCheckCast(com.ibm.domo.ssa.SSACheckCastInstruction)
     */
    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
    Set<IClass> types = HashSetFactory.make();
      
      for(TypeReference t : instruction.getDeclaredResultTypes()) {
        IClass cls = cha.lookupClass(t);
        if (cls == null) {
          return;
        } else {
          types.add(cls);
        }
      }
      

      PointerKey result = heapModel.getFilteredPointerKeyForLocal(node, 
          instruction.getResult(), 
          new FilteredPointerKey.MultipleClassesFilter(types.toArray(new IClass[ types.size() ])) );
        
      PointerKey value = heapModel.getPointerKeyForLocal(node, instruction.getVal());
      // TODO actually use the cast type
      addNode(result);
      addNode(value);
      addEdge(result, value);
      //
      // if (hasNoInterestingUses(instruction.getDef(), du)) {
      // system.recordImplicitPointsToSet(result);
      // } else {
      // if (contentsAreInvariant(symbolTable, du, instruction.getVal())) {
      // system.recordImplicitPointsToSet(value);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getVal(), SSAPropagationCallGraphBuilder.this);
      // if (cls.isInterface()) {
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // if (getClassHierarchy().implementsInterface(ik[i].getConcreteType(),
      // cls.getReference())) {
      // system.newConstraint(result, ik[i]);
      // }
      // }
      // } else {
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // if (getClassHierarchy().isSubclassOf(ik[i].getConcreteType(), cls)) {
      // system.newConstraint(result, ik[i]);
      // }
      // }
      // }
      // } else {
      // if (cls == null) {
      // getWarnings().add(ResolutionFailure.create(node,
      // instruction.getDeclaredResultType()));
      // cls = getJavaLangObject();
      // }
      // if (isRootType(cls)) {
      // system.newConstraint(result, assignOperator, value);
      // } else {
      // system.newConstraint(result, filterOperator, value);
      // }
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitReturn(com.ibm.domo.ssa.SSAReturnInstruction)
     */
    @Override
    public void visitReturn(SSAReturnInstruction instruction) {
      // skip returns of primitive type
      if (instruction.returnsPrimitiveType() || instruction.returnsVoid()) {
        return;
      } else {
        // just make a node for the def'd value
        PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getResult());
        addNode(def);
        PointerKey returnValue = heapModel.getPointerKeyForReturnValue(node);
        addNode(returnValue);
        addEdge(returnValue, def);
      }
      // PointerKey returnValue = getPointerKeyForReturnValue(node);
      // PointerKey result = getPointerKeyForLocal(node,
      // instruction.getResult());
      // // if (!supportFullPointerFlowGraph &&
      // // contentsAreInvariant(instruction.getResult())) {
      // if (contentsAreInvariant(symbolTable, du, instruction.getResult())) {
      // system.recordImplicitPointsToSet(result);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getResult(), SSAPropagationCallGraphBuilder.this);
      // for (int i = 0; i < ik.length; i++) {
      // system.newConstraint(returnValue, ik[i]);
      // }
      // } else {
      // system.newConstraint(returnValue, assignOperator, result);
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitGet(com.ibm.domo.ssa.SSAGetInstruction)
     */
    @Override
    public void visitGet(SSAGetInstruction instruction) {
      visitGetInternal(instruction.getDef(), instruction.isStatic(), instruction.getDeclaredField());
    }

    protected void visitGetInternal(int lval, boolean isStatic, FieldReference field) {

      // skip getfields of primitive type (optimisation)
      if (field.getFieldType().isPrimitiveType()) {
        return;
      }
      IField f = cg.getClassHierarchy().resolveField(field);
      if (f == null) {
        return;
      }
      PointerKey def = heapModel.getPointerKeyForLocal(node, lval);
      assert def != null;

      if (isStatic) {
        PointerKey fKey = heapModel.getPointerKeyForStaticField(f);
        addNode(def);
        addNode(fKey);
        addEdge(def, fKey);
      } else {
        addNode(def);
        getFieldDefs.put(def, f);
      }
      // system.newConstraint(def, assignOperator, fKey);
      // IClass klass = getClassHierarchy().lookupClass(field.getType());
      // if (klass == null) {
      // getWarnings().add(ResolutionFailure.create(node, field.getType()));
      // } else {
      // // side effect of getstatic: may call class initializer
      // if (DEBUG) {
      // Trace.guardedPrintln("getstatic call class init " + klass,
      // DEBUG_METHOD_SUBSTRING);
      // }

      //
      // if (hasNoInterestingUses(lval, du)) {
      // system.recordImplicitPointsToSet(def);
      // } else {
      // if (isStatic) {
      // PointerKey fKey = getPointerKeyForStaticField(f);
      // system.newConstraint(def, assignOperator, fKey);
      // IClass klass = getClassHierarchy().lookupClass(field.getType());
      // if (klass == null) {
      // getWarnings().add(ResolutionFailure.create(node, field.getType()));
      // } else {
      // // side effect of getstatic: may call class initializer
      // if (DEBUG) {
      // Trace.guardedPrintln("getstatic call class init " + klass,
      // DEBUG_METHOD_SUBSTRING);
      // }
      // processClassInitializer(klass);
      // }
      // } else {
      // PointerKey refKey = getPointerKeyForLocal(node, ref);
      // // if (!supportFullPointerFlowGraph &&
      // // contentsAreInvariant(ref)) {
      // if (contentsAreInvariant(symbolTable, du, ref)) {
      // system.recordImplicitPointsToSet(refKey);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node, ref,
      // SSAPropagationCallGraphBuilder.this);
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // PointerKey p = getPointerKeyForInstanceField(ik[i], f);
      // system.newConstraint(def, assignOperator, p);
      // }
      // } else {
      // system.newSideEffect(new GetFieldOperator(f,
      // system.findOrCreatePointsToSet(def)), refKey);
      // }
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitPut(com.ibm.domo.ssa.PutInstruction)
     */
    @Override
    public void visitPut(SSAPutInstruction instruction) {
      visitPutInternal(instruction.getVal(), instruction.isStatic(), instruction.getDeclaredField());
    }

    public void visitPutInternal(int rval, boolean isStatic, FieldReference field) {
      // skip putfields of primitive type (optimisation)
      if (field.getFieldType().isPrimitiveType()) {
        return;
      }
      IField f = cg.getClassHierarchy().resolveField(field);
      if (f == null) {
        return;
      }
      PointerKey use = heapModel.getPointerKeyForLocal(node, rval);
      assert use != null;

      if (isStatic) {
        PointerKey fKey = heapModel.getPointerKeyForStaticField(f);
        addNode(use);
        addNode(fKey);
        addEdge(fKey, use);
      } else {
        addNode(use);
      }
    }

    /*
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitInvoke(com.ibm.domo.ssa.InvokeInstruction)
     */
    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {

      for (int i = 0; i < instruction.getNumberOfUses(); i++) {
        // just make nodes for parameters; we'll get to them when traversing
        // from the callee
        PointerKey use = heapModel.getPointerKeyForLocal(node, instruction.getUse(i));
        addNode(use);
      }

      // for any def'd values, keep track of the fact that they are def'd
      // by a call
      if (instruction.hasDef()) {
        PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());
        addNode(def);
        callDefs.put(def, instruction);
      }
      PointerKey exc = heapModel.getPointerKeyForLocal(node, instruction.getException());
      addNode(exc);
      callDefs.put(exc, instruction);
      // TODO handle exceptions
      // if (DEBUG && debug) {
      // Trace.println("visitInvoke: " + instruction);
      // }
      //
      // PointerKey uniqueCatch = null;
      // if (hasUniqueCatchBlock(instruction, ir)) {
      // uniqueCatch = getUniqueCatchKey(instruction, ir, node);
      // }
      //
      // if (instruction.getCallSite().isStatic()) {
      // CGNode n = getTargetForCall(node, instruction.getCallSite(),
      // (InstanceKey) null);
      // if (n == null) {
      // getWarnings().add(ResolutionFailure.create(node, instruction));
      // } else {
      // processResolvedCall(node, instruction, n,
      // computeInvariantParameters(instruction), uniqueCatch);
      // if (DEBUG) {
      // Trace.guardedPrintln("visitInvoke class init " + n,
      // DEBUG_METHOD_SUBSTRING);
      // }
      //
      // // side effect of invoke: may call class initializer
      // processClassInitializer(n.getMethod().getDeclaringClass());
      // }
      // } else {
      // // Add a side effect that will fire when we determine a value
      // // for the receiver. This side effect will create a new node
      // // and new constraints based on the new callee context.
      // // NOTE: This will not be adequate for CPA-style context selectors,
      // // where the callee context may depend on state other than the
      // // receiver. TODO: rectify this when needed.
      // PointerKey receiver = getPointerKeyForLocal(node,
      // instruction.getReceiver());
      // // if (!supportFullPointerFlowGraph &&
      // // contentsAreInvariant(instruction.getReceiver())) {
      // if (contentsAreInvariant(symbolTable, du, instruction.getReceiver())) {
      // system.recordImplicitPointsToSet(receiver);
      // InstanceKey[] ik = getInvariantContents(symbolTable, du, node,
      // instruction.getReceiver(),
      // SSAPropagationCallGraphBuilder.this);
      // for (int i = 0; i < ik.length; i++) {
      // system.findOrCreateIndexForInstanceKey(ik[i]);
      // CGNode n = getTargetForCall(node, instruction.getCallSite(), ik[i]);
      // if (n == null) {
      // getWarnings().add(ResolutionFailure.create(node, instruction));
      // } else {
      // processResolvedCall(node, instruction, n,
      // computeInvariantParameters(instruction), uniqueCatch);
      // // side effect of invoke: may call class initializer
      // processClassInitializer(n.getMethod().getDeclaringClass());
      // }
      // }
      // } else {
      // if (DEBUG && debug) {
      // Trace.println("Add side effect, dispatch to " + instruction + ",
      // receiver " + receiver);
      // }
      // DispatchOperator dispatchOperator = new DispatchOperator(instruction,
      // node, computeInvariantParameters(instruction),
      // uniqueCatch);
      // system.newSideEffect(dispatchOperator, receiver);
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitNew(com.ibm.domo.ssa.NewInstruction)
     */
    @Override
    public void visitNew(SSANewInstruction instruction) {

      InstanceKey iKey = heapModel.getInstanceKeyForAllocation(node, instruction.getNewSite());
      if (iKey == null) {
        // something went wrong. I hope someone raised a warning.
        return;
      }
      PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      addNode(iKey);
      addNode(def);
      addEdge(def, iKey);
      // IClass klass = iKey.getConcreteType();
      //
      // if (DEBUG && debug) {
      // Trace.println("visitNew: " + instruction + " " + iKey + " " +
      // system.findOrCreateIndexForInstanceKey(iKey));
      // }
      //
      // if (klass == null) {
      // getWarnings().add(ResolutionFailure.create(node,
      // instruction.getConcreteType()));
      // return;
      // }
      //
      // if (!contentsAreInvariant(symbolTable, du, instruction.getDef())) {
      // system.newConstraint(def, iKey);
      // } else {
      // system.findOrCreateIndexForInstanceKey(iKey);
      // system.recordImplicitPointsToSet(def);
      // }
      //
      // // side effect of new: may call class initializer
      // if (DEBUG) {
      // Trace.guardedPrintln("visitNew call clinit: " + klass,
      // DEBUG_METHOD_SUBSTRING);
      // }
      // processClassInitializer(klass);
      //
      // // add instance keys and pointer keys for array contents
      // int dim = 0;
      // InstanceKey lastInstance = iKey;
      // while (klass != null && klass.isArrayClass()) {
      // klass = ((ArrayClass) klass).getElementClass();
      // // klass == null means it's a primitive
      // if (klass != null && klass.isArrayClass()) {
      // InstanceKey ik = getInstanceKeyForMultiNewArray(node,
      // instruction.getNewSite(), dim);
      // PointerKey pk = getPointerKeyForArrayContents(lastInstance);
      // if (DEBUG_MULTINEWARRAY) {
      // Trace.println("multinewarray constraint: ");
      // Trace.println(" pk: " + pk);
      // Trace.println(" ik: " + system.findOrCreateIndexForInstanceKey(ik) + "
      // concrete type " + ik.getConcreteType()
      // + " is " + ik);
      // Trace.println(" klass:" + klass);
      // }
      // system.newConstraint(pk, ik);
      // lastInstance = ik;
      // dim++;
      // }
      // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitThrow(com.ibm.domo.ssa.ThrowInstruction)
     */
    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
      // Assertions.UNREACHABLE();
      // don't do anything: we handle exceptional edges
      // in a separate pass
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitGetCaughtException(com.ibm.domo.ssa.GetCaughtExceptionInstruction)
     */
    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
      List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, getBasicBlock());
      PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());

      Set<IClass> types = SSAPropagationCallGraphBuilder.getCaughtExceptionTypes(instruction, ir);
      addExceptionDefConstraints(ir, node, peis, def, types);
    }

    // private int booleanConstantTest(SSAConditionalBranchInstruction c, int v)
    // {
    // int result = 0;
    //
    // // right for OPR_eq
    // if ((symbolTable.isZero(c.getUse(0)) && c.getUse(1) == v) ||
    // (symbolTable.isZero(c.getUse(1)) && c.getUse(0) == v)) {
    // result = -1;
    // } else if ((symbolTable.isOne(c.getUse(0)) && c.getUse(1) == v) ||
    // (symbolTable.isOne(c.getUse(1)) && c.getUse(0) == v)) {
    // result = 1;
    // }
    //
    // if (c.getOperator() == Constants.OPR_ne) {
    // result = -result;
    // }
    //
    // return result;
    // }
    //
    // private int nullConstantTest(SSAConditionalBranchInstruction c, int v) {
    // if ((symbolTable.isNullConstant(c.getUse(0)) && c.getUse(1) == v)
    // || (symbolTable.isNullConstant(c.getUse(1)) && c.getUse(0) == v)) {
    // if (c.getOperator() == Constants.OPR_eq) {
    // return 1;
    // } else {
    // return -1;
    // }
    // } else {
    // return 0;
    // }
    // }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitPi(com.ibm.domo.ssa.SSAPiInstruction)
     */
    @Override
    public void visitPi(SSAPiInstruction instruction) {
      Assertions.UNREACHABLE();
      // int dir;
      // ControlFlowGraph CFG = ir.getControlFlowGraph();
      // PointerKey src = getPointerKeyForLocal(node, instruction.getVal());
      // if (hasNoInterestingUses(instruction.getDef(), du)) {
      // PointerKey dst = getPointerKeyForLocal(node, instruction.getDef());
      // system.recordImplicitPointsToSet(dst);
      // } else {
      // if (com.ibm.domo.cfg.Util.endsWithConditionalBranch(CFG,
      // getBasicBlock()) && CFG.getSuccNodeCount(getBasicBlock()) == 2) {
      // SSAConditionalBranchInstruction cond =
      // com.ibm.domo.cfg.Util.getConditionalBranch(CFG, getBasicBlock());
      // SSAInstruction cause = instruction.getCause();
      // BasicBlock target = (BasicBlock)
      // CFG.getNode(instruction.getSuccessor());
      // if ((cause instanceof SSAInstanceofInstruction) && ((dir =
      // booleanConstantTest(cond, cause.getDef())) != 0)) {
      // TypeReference type = ((SSAInstanceofInstruction)
      // cause).getCheckedType();
      // IClass cls = cha.lookupClass(type);
      // if (cls == null) {
      // getWarnings().add(ResolutionFailure.create(node, type));
      // PointerKey dst = getPointerKeyForLocal(node, instruction.getDef());
      // system.newConstraint(dst, assignOperator, src);
      // } else {
      // PointerKey dst = getFilteredPointerKeyForLocal(node,
      // instruction.getDef(), cls);
      // if ((target == com.ibm.domo.cfg.Util.getTrueSuccessor(CFG,
      // getBasicBlock()) && dir == 1)
      // || (target == com.ibm.domo.cfg.Util.getFalseSuccessor(CFG,
      // getBasicBlock()) && dir == -1)) {
      // system.newConstraint(dst, filterOperator, src);
      // // System.err.println("PI " + dst + " " + src);
      // } else {
      // system.newConstraint(dst, inverseFilterOperator, src);
      // }
      // }
      // } else if ((dir = nullConstantTest(cond, instruction.getVal())) != 0) {
      // if ((target == com.ibm.domo.cfg.Util.getTrueSuccessor(CFG,
      // getBasicBlock()) && dir == -1)
      // || (target == com.ibm.domo.cfg.Util.getFalseSuccessor(CFG,
      // getBasicBlock()) && dir == 1)) {
      // PointerKey dst = getPointerKeyForLocal(node, instruction.getDef());
      // system.newConstraint(dst, assignOperator, src);
      // }
      // } else {
      // PointerKey dst = getPointerKeyForLocal(node, instruction.getDef());
      // system.newConstraint(dst, assignOperator, src);
      // }
      // } else {
      // PointerKey dst = getPointerKeyForLocal(node, instruction.getDef());
      // system.newConstraint(dst, assignOperator, src);
      // }
      // }
    }

    public ISSABasicBlock getBasicBlock() {
      return basicBlock;
    }

    /**
     * The calling loop must call this in each iteration!
     */
    public void setBasicBlock(ISSABasicBlock block) {
      basicBlock = block;
    }

    // /**
    // * Side effect: records invariant parameters as implicit points-to-sets.
    // *
    // * @return if non-null, then result[i] holds the set of instance keys
    // which
    // * may be passed as the ith parameter. (which must be invariant)
    // */
    // protected InstanceKey[][]
    // computeInvariantParameters(SSAAbstractInvokeInstruction call) {
    // InstanceKey[][] constParams = null;
    // for (int i = 0; i < call.getNumberOfUses(); i++) {
    // // not sure how getUse(i) <= 0 .. dead code?
    // // TODO: investigate
    // if (call.getUse(i) > 0) {
    // if (contentsAreInvariant(symbolTable, du, call.getUse(i))) {
    // system.recordImplicitPointsToSet(getPointerKeyForLocal(node,
    // call.getUse(i)));
    // if (constParams == null) {
    // constParams = new InstanceKey[call.getNumberOfUses()][];
    // }
    // constParams[i] = getInvariantContents(symbolTable, du, node,
    // call.getUse(i), SSAPropagationCallGraphBuilder.this);
    // for (int j = 0; j < constParams[i].length; j++) {
    // system.findOrCreateIndexForInstanceKey(constParams[i][j]);
    // }
    // }
    // }
    // }
    // return constParams;
    // }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
      Assertions.UNREACHABLE();
      // PointerKey def = getPointerKeyForLocal(node, instruction.getDef());
      // InstanceKey iKey =
      // getInstanceKeyForClassObject(instruction.getLoadedClass());
      //
      // if (!contentsAreInvariant(symbolTable, du, instruction.getDef())) {
      // system.newConstraint(def, iKey);
      // } else {
      // system.findOrCreateIndexForInstanceKey(iKey);
      // system.recordImplicitPointsToSet(def);
      // }
    }
  }

  // private final class FieldExpression {
  // LocalPointerKey x;
  //
  // IField f;
  //
  // public FieldExpression(IField f, LocalPointerKey x) {
  // this.f = f;
  // this.x = x;
  // }
  //
  // /*
  // * (non-Javadoc)
  // *
  // * @see java.lang.Object#equals(java.lang.Object)
  // */
  // public boolean equals(Object obj) {
  // if (obj instanceof FieldExpression) {
  // FieldExpression other = (FieldExpression) obj;
  // return x.equals(other.x) && f.equals(other.f);
  // } else {
  // return false;
  // }
  // }
  //
  // /*
  // * (non-Javadoc)
  // *
  // * @see java.lang.Object#hashCode()
  // */
  // public int hashCode() {
  // return x.hashCode() + 4729 * f.hashCode();
  // }
  // }

}
