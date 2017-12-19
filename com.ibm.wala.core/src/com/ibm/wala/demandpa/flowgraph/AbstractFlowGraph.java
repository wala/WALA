/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.flowgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.demandpa.flowgraph.DemandPointerFlowGraph.NewMultiDimInfo;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;

/**
 * A graph whose edges are labeled with {@link IFlowLabel}s.
 * 
 * @author Manu Sridharan
 * 
 */
public abstract class AbstractFlowGraph extends SlowSparseNumberedLabeledGraph<Object, IFlowLabel> implements IFlowGraph {

  private final static IFlowLabel defaultLabel = new IFlowLabel() {
    @Override
    public IFlowLabel bar() {
      return defaultLabel;
    }

    @Override
    public boolean isBarred() {
      return false;
    }

    @Override
    public void visit(IFlowLabelVisitor v, Object dst) {
    }

  };

  /**
   * Map: LocalPointerKey -&gt; SSAInvokeInstruction. If we have (x, foo()), that means that x was def'fed by the return value from the
   * call to foo()
   */
  protected final Map<PointerKey, SSAAbstractInvokeInstruction> callDefs = HashMapFactory.make();

  /**
   * Map: {@link LocalPointerKey} -&gt; {@link Set}&lt;{@link SSAInvokeInstruction}&gt;.
   * If we have (x, foo()), that means x was passed as a parameter to the call to foo().
   * The parameter position is not represented and must be recovered.
   */
  protected final Map<PointerKey, Set<SSAAbstractInvokeInstruction>> callParams = HashMapFactory.make();

  /**
   * Map: LocalPointerKey -&gt; CGNode. If we have (x, foo), then x is a parameter of method foo. For now, we have to re-discover the
   * parameter position. TODO this should just be a set; we can get the CGNode from the {@link LocalPointerKey}
   */
  protected final Map<PointerKey, CGNode> params = HashMapFactory.make();

  /**
   * Map: {@link LocalPointerKey} -&gt; {@link CGNode}. If we have (x, foo), then x is a return value of method foo. Must re-discover
   * if x is normal or exceptional return value.
   */
  protected final Map<PointerKey, CGNode> returns = HashMapFactory.make();

  protected final MemoryAccessMap mam;

  protected final HeapModel heapModel;

  protected final IClassHierarchy cha;

  protected final CallGraph cg;

  public AbstractFlowGraph(MemoryAccessMap mam, HeapModel heapModel, IClassHierarchy cha, CallGraph cg) {
    super(defaultLabel);
    this.mam = mam;
    this.heapModel = heapModel;
    this.cha = cha;
    this.cg = cg;
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowLabelGraph#visitSuccs(java.lang.Object,
   * com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor)
   */
  @Override
  public void visitSuccs(Object node, IFlowLabelVisitor v) {
    for (final IFlowLabel label : Iterator2Iterable.make(getSuccLabels(node))) {
      for (Object succNode : Iterator2Iterable.make(getSuccNodes(node, label))) {
        label.visit(v, succNode);
      }
    }
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowLabelGraph#visitPreds(java.lang.Object,
   * com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor)
   */
  @Override
  public void visitPreds(Object node, IFlowLabelVisitor v) {
    for (final IFlowLabel label : Iterator2Iterable.make(getPredLabels(node))) {
      for (Object predNode : Iterator2Iterable.make(getPredNodes(node, label))) {
        label.visit(v, predNode);
      }
    }
  }

  /**
   * For each invocation in the method, add nodes for actual parameters and return values
   * 
   * @param node
   */
  protected void addNodesForInvocations(CGNode node, IR ir) {
    for (CallSiteReference site : Iterator2Iterable.make(ir.iterateCallSites())) {
      SSAAbstractInvokeInstruction[] calls = ir.getCalls(site);
      for (SSAAbstractInvokeInstruction invokeInstr : calls) {
        for (int i = 0; i < invokeInstr.getNumberOfUses(); i++) {
          // just make nodes for parameters; we'll get to them when
          // traversing
          // from the callee
          PointerKey use = heapModel.getPointerKeyForLocal(node, invokeInstr.getUse(i));
          addNode(use);
          Set<SSAAbstractInvokeInstruction> s = MapUtil.findOrCreateSet(callParams, use);
          s.add(invokeInstr);
        }

        // for any def'd values, keep track of the fact that they are def'd
        // by a call
        if (invokeInstr.hasDef()) {
          PointerKey def = heapModel.getPointerKeyForLocal(node, invokeInstr.getDef());
          addNode(def);
          callDefs.put(def, invokeInstr);
        }
        PointerKey exc = heapModel.getPointerKeyForLocal(node, invokeInstr.getException());
        addNode(exc);
        callDefs.put(exc, invokeInstr);

      }
    }
  }

  @Override
  public boolean isParam(LocalPointerKey pk) {
    return params.get(pk) != null;
  }

  @Override
  public Iterator<SSAAbstractInvokeInstruction> getInstrsPassingParam(LocalPointerKey pk) {
    Set<SSAAbstractInvokeInstruction> instrs = callParams.get(pk);
    if (instrs == null) {
      return EmptyIterator.instance();
    } else {
      return instrs.iterator();
    }
  }

  @Override
  public SSAAbstractInvokeInstruction getInstrReturningTo(LocalPointerKey pk) {
    return callDefs.get(pk);
  }

  @Override
  public Iterator<? extends Object> getWritesToStaticField(StaticFieldKey sfk) throws IllegalArgumentException {
    if (sfk == null) {
      throw new IllegalArgumentException("sfk == null");
    }
    Collection<MemoryAccess> fieldWrites = mam.getStaticFieldWrites(sfk.getField());
    for (MemoryAccess a : fieldWrites) {
      addSubgraphForNode(a.getNode());
    }
    return getSuccNodes(sfk, AssignGlobalLabel.v());
  }

  @Override
  public Iterator<? extends Object> getReadsOfStaticField(StaticFieldKey sfk) throws IllegalArgumentException {
    if (sfk == null) {
      throw new IllegalArgumentException("sfk == null");
    }
    Collection<MemoryAccess> fieldReads = mam.getStaticFieldReads(sfk.getField());
    for (MemoryAccess a : fieldReads) {
      addSubgraphForNode(a.getNode());
    }
    return getPredNodes(sfk, AssignGlobalLabel.v());
  }

  @Override
  public Iterator<PointerKey> getWritesToInstanceField(PointerKey pk, IField f) {
    // TODO: cache this!!
    if (f == ArrayContents.v()) {
      return getArrayWrites(pk);
    }
    pk = convertPointerKeyToHeapModel(pk, mam.getHeapModel());
    Collection<MemoryAccess> writes = mam.getFieldWrites(pk, f);
    for (MemoryAccess a : writes) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> written = new ArrayList<>();
    for (MemoryAccess a : writes) {
      IR ir = a.getNode().getIR();
      SSAPutInstruction s = (SSAPutInstruction) ir.getInstructions()[a.getInstructionIndex()];
      if (s == null) {
        // s can be null because the memory access map may be constructed from bytecode,
        // and the write instruction may have been eliminated from SSA because it's dead
        // TODO clean this up
        continue;
      }
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getVal());
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(containsNode(r));
      // }
      written.add(r);
    }
    return written.iterator();
  }

  /**
   * convert a pointer key to one in the memory access map's heap model
   * 
   * TODO move this somewhere more appropriate
   * 
   * @throws UnsupportedOperationException if it doesn't know how to handle a {@link PointerKey}
   */
  public static PointerKey convertPointerKeyToHeapModel(PointerKey pk, HeapModel h) {
    if (pk == null) {
      throw new IllegalArgumentException("null pk");
    }
    if (pk instanceof LocalPointerKey) {
      LocalPointerKey lpk = (LocalPointerKey) pk;
      return h.getPointerKeyForLocal(lpk.getNode(), lpk.getValueNumber());
    } else if (pk instanceof ArrayContentsKey) {
      ArrayContentsKey ack = (ArrayContentsKey) pk;
      InstanceKey ik = ack.getInstanceKey();
      if (ik instanceof NormalAllocationInNode) {
        NormalAllocationInNode nain = (NormalAllocationInNode) ik;
        ik = h.getInstanceKeyForAllocation(nain.getNode(), nain.getSite());
      } else {
        assert false : "need to handle " + ik.getClass();
      }
      return h.getPointerKeyForArrayContents(ik);
    } else if (pk instanceof ReturnValueKey) {
      ReturnValueKey rvk = (ReturnValueKey) pk;
      return h.getPointerKeyForReturnValue(rvk.getNode());
    }
    throw new UnsupportedOperationException("need to handle " + pk.getClass());
  }

  @Override
  public Iterator<PointerKey> getReadsOfInstanceField(PointerKey pk, IField f) {
    // TODO: cache this!!
    if (f == ArrayContents.v()) {
      return getArrayReads(pk);
    }
    pk = convertPointerKeyToHeapModel(pk, mam.getHeapModel());
    Collection<MemoryAccess> reads = mam.getFieldReads(pk, f);
    for (MemoryAccess a : reads) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> readInto = new ArrayList<>();
    for (MemoryAccess a : reads) {
      IR ir = a.getNode().getIR();
      SSAGetInstruction s = (SSAGetInstruction) ir.getInstructions()[a.getInstructionIndex()];
      if (s == null) {
        // actually dead code
        continue;
      }
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getDef());
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(containsNode(r));
      // }
      readInto.add(r);
    }
    return readInto.iterator();
  }

  Iterator<PointerKey> getArrayWrites(PointerKey arrayRef) {
    arrayRef = convertPointerKeyToHeapModel(arrayRef, mam.getHeapModel());
    Collection<MemoryAccess> arrayWrites = mam.getArrayWrites(arrayRef);
    for (MemoryAccess a : arrayWrites) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> written = new ArrayList<>();
    for (MemoryAccess a : arrayWrites) {
      final CGNode node = a.getNode();
      IR ir = node.getIR();
      SSAInstruction instruction = ir.getInstructions()[a.getInstructionIndex()];
      if (instruction == null) {
        // this means the array store found was in fact dead code
        // TODO detect this earlier and don't keep it in the MemoryAccessMap
        continue;
      }
      if (instruction instanceof SSAArrayStoreInstruction) {
        SSAArrayStoreInstruction s = (SSAArrayStoreInstruction) instruction;
        PointerKey r = heapModel.getPointerKeyForLocal(node, s.getValue());
        written.add(r);
      } else if (instruction instanceof SSANewInstruction) {
        NewMultiDimInfo multiDimInfo = DemandPointerFlowGraph.getInfoForNewMultiDim((SSANewInstruction) instruction, heapModel,
            node);
        for (Pair<PointerKey, PointerKey> arrStoreInstr : multiDimInfo.arrStoreInstrs) {
          written.add(arrStoreInstr.snd);
        }
      } else {
        Assertions.UNREACHABLE();
      }
    }
    return written.iterator();
  }

  protected Iterator<PointerKey> getArrayReads(PointerKey arrayRef) {
    arrayRef = convertPointerKeyToHeapModel(arrayRef, mam.getHeapModel());
    Collection<MemoryAccess> arrayReads = mam.getArrayReads(arrayRef);
    for (MemoryAccess a : arrayReads) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> read = new ArrayList<>();
    for (MemoryAccess a : arrayReads) {
      IR ir = a.getNode().getIR();
      SSAArrayLoadInstruction s = (SSAArrayLoadInstruction) ir.getInstructions()[a.getInstructionIndex()];
      if (s == null) {
        // actually dead code
        continue;
      }
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getDef());
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(containsNode(r));
      // }
      read.add(r);
    }
    return read.iterator();
  }

  /**
   * Add constraints to represent the flow of exceptions to the exceptional return value for this node
   */
  protected void addNodePassthruExceptionConstraints(CGNode node, IR ir) {
    // add constraints relating to thrown exceptions that reach the exit
    // block.
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
  protected void addExceptionDefConstraints(IR ir, CGNode node, List<ProgramCounter> peis, PointerKey exceptionVar,
      Set<IClass> catchClasses) {
    for (ProgramCounter peiLoc : peis) {
      SSAInstruction pei = ir.getPEI(peiLoc);

      if (pei instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction s = (SSAAbstractInvokeInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e, AssignLabel.noFilter());

      } else if (pei instanceof SSAAbstractThrowInstruction) {
        SSAAbstractThrowInstruction s = (SSAAbstractThrowInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e, AssignLabel.noFilter());
      }

      // Account for those exceptions for which we do not actually have a
      // points-to set for
      // the pei, but just instance keys
      Collection<TypeReference> types = pei.getExceptionTypes();
      if (types != null) {
        for (TypeReference type : types) {
          if (type != null) {
            InstanceKey ik = heapModel.getInstanceKeyForPEI(node, peiLoc, type);
            if (ik == null) {
              // ugh.  hope someone somewhere else knows what they are doing.
              // this is probably due to analysis scope exclusions.
              continue;
            }
            if (!(ik instanceof ConcreteTypeKey)) {
              assert ik instanceof ConcreteTypeKey : "uh oh: need to implement getCaughtException constraints for instance " + ik;
            }
            ConcreteTypeKey ck = (ConcreteTypeKey) ik;
            IClass klass = ck.getType();
            if (PropagationCallGraphBuilder.catches(catchClasses, klass, cha)) {
              addNode(exceptionVar);
              addNode(ik);
              addEdge(exceptionVar, ik, NewLabel.v());
            }
          }
        }
      }
    }
  }

  /**
   * add constraints for reference constants assigned to vars
   */
  protected void addNodeConstantConstraints(CGNode node, IR ir) {
    SymbolTable symbolTable = ir.getSymbolTable();
    for (int i = 1; i <= symbolTable.getMaxValueNumber(); i++) {
      if (symbolTable.isConstant(i)) {
        Object v = symbolTable.getConstantValue(i);
        if (!(v instanceof Number)) {
          Object S = symbolTable.getConstantValue(i);
          TypeReference type = node.getMethod().getDeclaringClass().getClassLoader().getLanguage().getConstantType(S);
          if (type != null) {
            InstanceKey ik = heapModel.getInstanceKeyForConstant(type, S);
            if (ik != null) {
              PointerKey pk = heapModel.getPointerKeyForLocal(node, i);
              addNode(pk);
              addNode(ik);
              addEdge(pk, ik, NewLabel.v());
            }
          }
        }
      }
    }
  }

}
