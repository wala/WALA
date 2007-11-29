/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PointerParamValueNumIterator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ReferenceCleanser;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.BitVectorIntSet;

/**
 * A graph representing program flow, constructed method-by-method on demand
 * 
 * @author Manu Sridharan
 * 
 */
public abstract class DemandFlowGraph extends FlowLabelGraph {
  private final static boolean DEBUG = false;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = 0;

  protected final CallGraph cg;

  protected final HeapModel heapModel;

  protected final MemoryAccessMap mam;

  protected final ClassHierarchy cha;

  /**
   * Map: LocalPointerKey -> SSAInvokeInstruction. If we have (x, foo()), that
   * means that x was def'fed by the return value from the call to foo()
   */
  final Map<PointerKey, SSAInvokeInstruction> callDefs = HashMapFactory.make();

  /**
   * Map: {@link LocalPointerKey} -> Set<{@link SSAInvokeInstruction}>. If we
   * have (x, foo()), that means x was passed as a parameter to the call to
   * foo(). The parameter position is not represented and must be recovered.
   */
  final Map<PointerKey, Set<SSAInvokeInstruction>> callParams = HashMapFactory.make();

  /**
   * Map: LocalPointerKey -> CGNode. If we have (x, foo), then x is a parameter
   * of method foo. For now, we have to re-discover the parameter position. TODO
   * this should just be a set; we can get the CGNode from the
   * {@link LocalPointerKey}
   */
  final Map<PointerKey, CGNode> params = HashMapFactory.make();

  /**
   * Map: {@link LocalPointerKey} -> {@link CGNode}. If we have (x, foo), then
   * x is a return value of method foo. Must re-discover if x is normal or
   * exceptional return value.
   */
  final Map<PointerKey, CGNode> returns = HashMapFactory.make();

  /**
   * node numbers of CGNodes we have already visited
   */
  final BitVectorIntSet cgNodesVisited = new BitVectorIntSet();

  /**
   * add representation of flow for a node, if not already present
   * 
   * @param node
   * @throws IllegalArgumentException
   *             if node == null
   */
  public void addSubgraphForNode(CGNode node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node == null");
    }
    if (node.getIR() == null) {
      throw new IllegalArgumentException("no ir for node " + node);
    }
    int n = cg.getNumber(node);
    if (!cgNodesVisited.contains(n)) {
      cgNodesVisited.add(n);
      unconditionallyAddConstraintsFromNode(node);
      addNodesForParameters(node);
    }
  }

  public boolean hasSubgraphForNode(CGNode node) {
    return cgNodesVisited.contains(cg.getNumber(node));
  }

  public boolean isParam(LocalPointerKey pk) {
    return params.get(pk) != null;
  }

  /**
   * get actuals passed into some formal parameter
   * 
   * @return an Iterator<PointerKeyAndCallSite>
   */
  public Iterator<PointerKeyAndCallSite> getParamSuccs(LocalPointerKey pk) {
    // TODO cache this result
    // TODO take some cgnode as parameter if we have calling context?
    CGNode cgNode = params.get(pk);
    if (cgNode == null) {
      return EmptyIterator.instance();
    }
    int paramPos = pk.getValueNumber() - 1;
    ArrayList<PointerKeyAndCallSite> paramSuccs = new ArrayList<PointerKeyAndCallSite>();
    // iterate over callers
    for (CGNode caller : cg) {
      // TODO optimization: we don't need to add the graph if null is passed
      // as the argument
      addSubgraphForNode(caller);
      IR ir = caller.getIR();
      for (Iterator<CallSiteReference> iterator = ir.iterateCallSites(); iterator.hasNext();) {
        CallSiteReference call = iterator.next();
        if (cg.getPossibleTargets(caller, call).contains(cgNode)) {
          SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
          for (int i = 0; i < callInstrs.length; i++) {
            SSAAbstractInvokeInstruction callInstr = callInstrs[i];
            PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
            if (Assertions.verifyAssertions) {
              Assertions._assert(containsNode(actualPk));
              Assertions._assert(containsNode(pk));
            }
            paramSuccs.add(new PointerKeyAndCallSite(actualPk, call));
          }
        }
      }
    }
    return paramSuccs.iterator();
  }

  /**
   * 
   * 
   * @param pk
   * @return the {@link SSAInvokeInstruction}s passing some pointer as a
   *         parameter
   */
  public Iterator<SSAInvokeInstruction> getInstrsPassingParam(LocalPointerKey pk) {
    Set<SSAInvokeInstruction> instrs = callParams.get(pk);
    if (instrs == null) {
      return EmptyIterator.instance();
    } else {
      return instrs.iterator();
    }
  }

  /**
   * 
   * 
   * @return formals to which some actual parameter is passed
   */
  public Iterator<PointerKeyAndCallSite> getParamPreds(LocalPointerKey pk) {
    // TODO
    Set<SSAInvokeInstruction> instrs = callParams.get(pk);
    if (instrs == null) {
      return EmptyIterator.instance();
    }
    ArrayList<PointerKeyAndCallSite> paramPreds = new ArrayList<PointerKeyAndCallSite>();
    for (SSAInvokeInstruction callInstr : instrs) {
      for (int i = 0; i < callInstr.getNumberOfUses(); i++) {
        if (pk.getValueNumber() != callInstr.getUse(i))
          continue;
        CallSiteReference callSiteRef = callInstr.getSite();
        // get call targets
        Collection<CGNode> possibleCallees = cg.getPossibleTargets(pk.getNode(), callSiteRef);
        // construct graph for each target
        for (CGNode callee : possibleCallees) {
          addSubgraphForNode(callee);
          // TODO test this!!!
          // TODO test passing null as an argument
          PointerKey paramVal = heapModel.getPointerKeyForLocal(callee, i + 1);
          if (Assertions.verifyAssertions) {
            Assertions._assert(containsNode(paramVal));
          }
          paramPreds.add(new PointerKeyAndCallSite(paramVal, callSiteRef));
        }
      }
    }
    return paramPreds.iterator();

  }

  /**
   * get the {@link SSAInvokeInstruction} whose return value is assigned to a
   * pointer key.
   * 
   * @param pk
   * @return the instruction, or <code>null</code> if no return value is
   *         assigned to pk
   */
  public SSAInvokeInstruction getInstrReturningTo(LocalPointerKey pk) {
    return callDefs.get(pk);
  }

  /**
   * 
   * 
   * @return return values assigned to some node
   */
  public Iterator<PointerKeyAndCallSite> getReturnSuccs(LocalPointerKey pk) {
    SSAInvokeInstruction callInstr = callDefs.get(pk);
    if (callInstr == null)
      return EmptyIterator.instance();
    ArrayList<PointerKeyAndCallSite> returnSuccs = new ArrayList<PointerKeyAndCallSite>();
    boolean isExceptional = pk.getValueNumber() == callInstr.getException();

    CallSiteReference callSiteRef = callInstr.getSite();
    // get call targets
    Collection<CGNode> possibleCallees = cg.getPossibleTargets(pk.getNode(), callSiteRef);
    // construct graph for each target
    for (CGNode callee : possibleCallees) {
      addSubgraphForNode(callee);
      PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
          .getPointerKeyForReturnValue(callee);
      if (Assertions.verifyAssertions) {
        Assertions._assert(containsNode(retVal));
      }
      returnSuccs.add(new PointerKeyAndCallSite(retVal, callSiteRef));
    }

    return returnSuccs.iterator();
  }

  public boolean isReturnVal(LocalPointerKey pk) {
    return returns.get(pk) != null;
  }

  /**
   * 
   * 
   * @return nodes to which some return value is assigned
   */
  public Iterator<PointerKeyAndCallSite> getReturnPreds(LocalPointerKey pk) {
    CGNode cgNode = returns.get(pk);
    if (cgNode == null) {
      return EmptyIterator.instance();
    }
    boolean isExceptional = pk == heapModel.getPointerKeyForExceptionalReturnValue(cgNode);
    ArrayList<PointerKeyAndCallSite> returnPreds = new ArrayList<PointerKeyAndCallSite>();
    // iterate over callers
    for (CGNode caller : cg) {
      // TODO we don't need to add the graph if null is passed
      // as the argument
      addSubgraphForNode(caller);
      IR ir = caller.getIR();
      for (Iterator<CallSiteReference> iterator = ir.iterateCallSites(); iterator.hasNext();) {
        CallSiteReference call = iterator.next();
        if (cg.getPossibleTargets(caller, call).contains(cgNode)) {
          SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
          for (int i = 0; i < callInstrs.length; i++) {
            SSAAbstractInvokeInstruction callInstr = callInstrs[i];
            PointerKey returnPk = heapModel.getPointerKeyForLocal(caller, isExceptional ? callInstr.getException() : callInstr
                .getDef());
            if (Assertions.verifyAssertions) {
              Assertions._assert(containsNode(returnPk));
              Assertions._assert(containsNode(pk));
            }
            returnPreds.add(new PointerKeyAndCallSite(returnPk, call));
          }
        }
      }
    }
    return returnPreds.iterator();
  }

  /**
   * @param sfk
   *            the static field
   * @return all the variables whose values are written to sfk
   * @throws IllegalArgumentException
   *             if sfk == null
   */
  public Iterator<? extends Object> getWritesToStaticField(StaticFieldKey sfk) throws IllegalArgumentException {
    if (sfk == null) {
      throw new IllegalArgumentException("sfk == null");
    }
    Collection<MemoryAccess> fieldWrites = mam.getFieldWrites(sfk.getField());
    for (MemoryAccess a : fieldWrites) {
      addSubgraphForNode(a.getNode());
    }
    return getSuccNodes(sfk, AssignGlobalLabel.v());
  }

  /**
   * @param sfk
   *            the static field
   * @return all the variables that get the value of sfk
   * @throws IllegalArgumentException
   *             if sfk == null
   */
  public Iterator<? extends Object> getReadsOfStaticField(StaticFieldKey sfk) throws IllegalArgumentException {
    if (sfk == null) {
      throw new IllegalArgumentException("sfk == null");
    }
    Collection<MemoryAccess> fieldReads = mam.getFieldReads(sfk.getField());
    for (MemoryAccess a : fieldReads) {
      addSubgraphForNode(a.getNode());
    }
    return getPredNodes(sfk, AssignGlobalLabel.v());
  }

  public Iterator<PointerKey> getWritesToInstanceField(IField f) {
    // TODO: cache this!!
    if (f == ArrayContents.v()) {
      return getArrayWrites();
    }
    Collection<MemoryAccess> writes = mam.getFieldWrites(f);
    for (MemoryAccess a : writes) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> written = new ArrayList<PointerKey>();
    for (MemoryAccess a : writes) {
      IR ir = a.getNode().getIR();
      SSAPutInstruction s = (SSAPutInstruction) ir.getInstructions()[a.getInstructionIndex()];
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getVal());
      if (Assertions.verifyAssertions) {
        Assertions._assert(containsNode(r));
      }
      written.add(r);
    }
    return written.iterator();
  }

  public Iterator<PointerKey> getReadsOfInstanceField(IField f) {
    // TODO: cache this!!
    if (f == ArrayContents.v()) {
      return getArrayReads();
    }
    Collection<MemoryAccess> reads = mam.getFieldReads(f);
    for (MemoryAccess a : reads) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> readInto = new ArrayList<PointerKey>();
    for (MemoryAccess a : reads) {
      IR ir = a.getNode().getIR();
      SSAGetInstruction s = (SSAGetInstruction) ir.getInstructions()[a.getInstructionIndex()];
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getDef());
      if (Assertions.verifyAssertions) {
        Assertions._assert(containsNode(r));
      }
      readInto.add(r);
    }
    return readInto.iterator();
  }

  private Iterator<PointerKey> getArrayWrites() {
    Collection<MemoryAccess> arrayWrites = mam.getArrayWrites();
    for (MemoryAccess a : arrayWrites) {
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> written = new ArrayList<PointerKey>();
    for (MemoryAccess a : arrayWrites) {
      final CGNode node = a.getNode();
      IR ir = node.getIR();
      SSAInstruction instruction = ir.getInstructions()[a.getInstructionIndex()];
      if (instruction instanceof SSAArrayStoreInstruction) {
        SSAArrayStoreInstruction s = (SSAArrayStoreInstruction) instruction;
        PointerKey r = heapModel.getPointerKeyForLocal(node, s.getValue());
        if (Assertions.verifyAssertions) {
          Assertions._assert(containsNode(r), "missing node for " + r);
        }
        written.add(r);
      } else if (instruction instanceof SSANewInstruction) {
        SSANewInstruction n = (SSANewInstruction) instruction;
        // should be allocated multi-dimentional array
        InstanceKey iKey = heapModel.getInstanceKeyForAllocation(node, n.getNewSite());
        IClass klass = iKey.getConcreteType();
        if (Assertions.verifyAssertions) {
          Assertions._assert(klass.isArrayClass() && ((ArrayClass) klass).getElementClass().isArrayClass());
        }
        int dim = 0;
        InstanceKey lastInstance = iKey;
        while (klass != null && klass.isArrayClass()) {
          klass = ((ArrayClass) klass).getElementClass();
          // klass == null means it's a primitive
          if (klass != null && klass.isArrayClass()) {
            InstanceKey ik = heapModel.getInstanceKeyForMultiNewArray(node, n.getNewSite(), dim);
            PointerKey pk = heapModel.getPointerKeyForArrayContents(lastInstance);
            written.add(pk);
            // if (DEBUG_MULTINEWARRAY) {
            // Trace.println("multinewarray constraint: ");
            // Trace.println(" pk: " + pk);
            // Trace.println(" ik: " +
            // system.findOrCreateIndexForInstanceKey(ik)
            // + " concrete type " + ik.getConcreteType()
            // + " is " + ik);
            // Trace.println(" klass:" + klass);
            // }
            // addNode(ik);
            // addNode(pk);
            // addEdge(pk, ik, NewLabel.v());
            lastInstance = ik;
            dim++;
          }
        }

      } else {
        Assertions.UNREACHABLE();
      }
    }
    return written.iterator();
  }

  private Iterator<PointerKey> getArrayReads() {
    Collection<MemoryAccess> arrayReads = mam.getArrayReads();
    for (Iterator<MemoryAccess> it = arrayReads.iterator(); it.hasNext();) {
      MemoryAccess a = it.next();
      addSubgraphForNode(a.getNode());
    }
    ArrayList<PointerKey> read = new ArrayList<PointerKey>();
    for (Iterator<MemoryAccess> it = arrayReads.iterator(); it.hasNext();) {
      MemoryAccess a = it.next();
      IR ir = a.getNode().getIR();
      SSAArrayLoadInstruction s = (SSAArrayLoadInstruction) ir.getInstructions()[a.getInstructionIndex()];
      PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getDef());
      if (Assertions.verifyAssertions) {
        Assertions._assert(containsNode(r));
      }
      read.add(r);
    }
    return read.iterator();
  }

  protected abstract void addNodesForParameters(CGNode node);

  public Iterator<Integer> pointerParamValueNums(CGNode node) {
    return new PointerParamValueNumIterator(node);
  }

  protected void unconditionallyAddConstraintsFromNode(CGNode node) {

    if (DEBUG) {
      Trace.println("Adding constraints for CGNode " + node);
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
    addNodeConstantConstraints(node, ir);
  }

  /**
   * add constraints for reference constants assigned to vars
   */
  private void addNodeConstantConstraints(CGNode node, IR ir) {
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

  /**
   * Add constraints to represent the flow of exceptions to the exceptional
   * return value for this node
   */
  protected void addNodePassthruExceptionConstraints(CGNode node, IR ir) {
    // add constraints relating to thrown exceptions that reach the exit
    // block.
    List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, ir.getExitBlock());
    PointerKey exception = heapModel.getPointerKeyForExceptionalReturnValue(node);

    addExceptionDefConstraints(ir, node, peis, exception, PropagationCallGraphBuilder.THROWABLE_SET);
  }

  /**
   * Generate constraints which assign exception values into an exception
   * pointer
   * 
   * @param node
   *            governing node
   * @param peis
   *            list of PEI instructions
   * @param exceptionVar
   *            PointerKey representing a pointer to an exception value
   * @param catchClasses
   *            the types "caught" by the exceptionVar
   */
  protected void addExceptionDefConstraints(IR ir, CGNode node, List<ProgramCounter> peis, PointerKey exceptionVar,
      Set<TypeReference> catchClasses) {
    for (Iterator<ProgramCounter> it = peis.iterator(); it.hasNext();) {
      ProgramCounter peiLoc = it.next();
      SSAInstruction pei = ir.getPEI(peiLoc);

      if (pei instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction s = (SSAAbstractInvokeInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e, AssignLabel.v());

      } else if (pei instanceof SSAAbstractThrowInstruction) {
        SSAAbstractThrowInstruction s = (SSAAbstractThrowInstruction) pei;
        PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
        addNode(exceptionVar);
        addNode(e);
        addEdge(exceptionVar, e, AssignLabel.v());
      }

      // Account for those exceptions for which we do not actually have a
      // points-to set for
      // the pei, but just instance keys
      Collection<TypeReference> types = pei.getExceptionTypes();
      if (types != null) {
        for (Iterator<TypeReference> it2 = types.iterator(); it2.hasNext();) {
          TypeReference type = it2.next();
          if (type != null) {
            InstanceKey ik = heapModel.getInstanceKeyForPEI(node, peiLoc, type);
            if (Assertions.verifyAssertions) {
              if (!(ik instanceof ConcreteTypeKey)) {
                Assertions._assert(ik instanceof ConcreteTypeKey,
                    "uh oh: need to implement getCaughtException constraints for instance " + ik);
              }
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
   * Add pointer flow constraints based on instructions in a given node
   */
  protected void addNodeInstructionConstraints(CGNode node, IR ir, DefUse du) {
    FlowStatementVisitor v = makeVisitor((ExplicitCallGraph.ExplicitNode) node, ir, du);
    ControlFlowGraph<ISSABasicBlock> cfg = ir.getControlFlowGraph();
    for (ISSABasicBlock b : cfg) {
      addBlockInstructionConstraints(node, cfg, b, v);
    }
  }

  /**
   * Add constraints for a particular basic block.
   */
  protected void addBlockInstructionConstraints(CGNode node, ControlFlowGraph<ISSABasicBlock> cfg, ISSABasicBlock b,
      FlowStatementVisitor v) {
    v.setBasicBlock(b);

    // visit each instruction in the basic block.
    for (Iterator<IInstruction> it = b.iterator(); it.hasNext();) {
      SSAInstruction s = (SSAInstruction) it.next();
      if (s != null) {
        s.visit(v);
      }
    }

    addPhiConstraints(node, cfg, b);
  }

  private void addPhiConstraints(CGNode node, ControlFlowGraph<ISSABasicBlock> cfg, ISSABasicBlock b) {

    // visit each phi instruction in each successor block
    for (Iterator<? extends IBasicBlock> iter = cfg.getSuccNodes(b); iter.hasNext();) {
      ISSABasicBlock sb = (ISSABasicBlock) iter.next();
      if (sb.isExitBlock()) {
        // an optimization based on invariant that exit blocks should
        // have no
        // phis.
        continue;
      }
      int n = 0;
      // set n to be whichPred(this, sb);
      for (Iterator<? extends IBasicBlock> back = cfg.getPredNodes(sb); back.hasNext(); n++) {
        if (back.next() == b) {
          break;
        }
      }
      if (DEBUG && Assertions.verifyAssertions) {
        Assertions._assert(n < cfg.getPredNodeCount(sb));
      }
      for (Iterator<SSAPhiInstruction> phis = sb.iteratePhis(); phis.hasNext();) {
        // Assertions.UNREACHABLE();
        SSAPhiInstruction phi = phis.next();
        if (phi == null) {
          continue;
        }
        PointerKey def = heapModel.getPointerKeyForLocal(node, phi.getDef());
        if (phi.getUse(n) > 0) {
          PointerKey use = heapModel.getPointerKeyForLocal(node, phi.getUse(n));
          addNode(def);
          addNode(use);
          addEdge(def, use, AssignLabel.v());
        }
        // }
        // }
      }
    }
  }

  protected abstract FlowStatementVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node, IR ir, DefUse du);

  private void debugPrintIR(IR ir) {
    if (DEBUG) {
      if (ir == null) {
        Trace.println("\n   No statements\n");
      } else {
        try {
          Trace.println(ir.toString());
        } catch (Error e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  protected interface FlowStatementVisitor extends SSAInstruction.IVisitor {
    void setBasicBlock(ISSABasicBlock b);
  }

  public DemandFlowGraph(final CallGraph cg, final HeapModel heapModel, final MemoryAccessMap mam, final ClassHierarchy cha) {
    this.cg = cg;
    this.heapModel = heapModel;
    this.mam = mam;
    this.cha = cha;
  }

}