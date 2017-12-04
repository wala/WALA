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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PointerParamValueNumIterator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
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
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;

/**
 * A graph representation of statements flowing pointer values, but <em>not</em> primitive values. Nodes are variables, and edges
 * are <em>against</em> value flow; assignment x = y yields edge from x to y with label {@link AssignLabel#noFilter()}
 */
public class DemandPointerFlowGraph extends AbstractDemandFlowGraph implements IFlowGraph {

  public DemandPointerFlowGraph(CallGraph cg, HeapModel heapModel, MemoryAccessMap mam, IClassHierarchy cha) {
    super(cg, heapModel, mam, cha);
  }

  /**
   * add nodes for parameters and return values
   */
  @Override
  protected void addNodesForParameters(CGNode node, IR ir) {
    for (int parameter : Iterator2Iterable.make(new PointerParamValueNumIterator(node))) {
      PointerKey paramPk = heapModel.getPointerKeyForLocal(node, parameter);
      addNode(paramPk);
      params.put(paramPk, node);
    }
    PointerKey returnKey = heapModel.getPointerKeyForReturnValue(node);
    addNode(returnKey);
    returns.put(returnKey, node);
    PointerKey exceptionReturnKey = heapModel.getPointerKeyForExceptionalReturnValue(node);
    addNode(exceptionReturnKey);
    returns.put(exceptionReturnKey, node);
  }

  @Override
  protected FlowStatementVisitor makeVisitor(CGNode node) {
    return new StatementVisitor(heapModel, this, cha, cg, node);
  }

  /**
   * A visitor that generates graph nodes and edges for an IR.
   * 
   * strategy: when visiting a statement, for each use of that statement, add a graph edge from def to use.
   * 
   * TODO: special treatment for parameter passing, etc.
   */
  public static class StatementVisitor extends SSAInstruction.Visitor implements FlowStatementVisitor {

    private final HeapModel heapModel;

    private final IFlowGraph g;

    private final IClassHierarchy cha;

    private final CallGraph cg;

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

    public StatementVisitor(HeapModel heapModel, IFlowGraph g, IClassHierarchy cha, CallGraph cg, CGNode node) {
      super();
      this.heapModel = heapModel;
      this.g = g;
      this.cha = cha;
      this.cg = cg;
      this.node = node;
      this.ir = node.getIR();
      this.du = node.getDU();
      this.symbolTable = ir.getSymbolTable();
      assert symbolTable != null;
    }

    /*
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitArrayLoad(com.ibm.domo.ssa.SSAArrayLoadInstruction)
     */
    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      // skip arrays of primitive type
      if (instruction.typeIsPrimitive()) {
        return;
      }
      PointerKey result = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      PointerKey arrayRef = heapModel.getPointerKeyForLocal(node, instruction.getArrayRef());
      // TODO optimizations for purely local stuff
      g.addNode(result);
      g.addNode(arrayRef);
      g.addEdge(result, arrayRef, GetFieldLabel.make(ArrayContents.v()));
    }

    /*
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
      PointerKey arrayRef = heapModel.getPointerKeyForLocal(node, instruction.getArrayRef());
      // TODO purely local optimizations
      g.addNode(value);
      g.addNode(arrayRef);
      g.addEdge(arrayRef, value, PutFieldLabel.make(ArrayContents.v()));
    }

    /*
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
      
      FilteredPointerKey.MultipleClassesFilter filter = new FilteredPointerKey.MultipleClassesFilter(types.toArray(new IClass[ types.size() ]));
      PointerKey result = heapModel.getPointerKeyForLocal(node, instruction.getResult());
      PointerKey value = heapModel.getPointerKeyForLocal(node, instruction.getVal());
      g.addNode(result);
      g.addNode(value);
      g.addEdge(result, value, AssignLabel.make(filter));
    }

    /*
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
        g.addNode(def);
        PointerKey returnValue = heapModel.getPointerKeyForReturnValue(node);
        g.addNode(returnValue);
        g.addEdge(returnValue, def, AssignLabel.noFilter());
      }
    }

    /*
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitGet(com.ibm.domo.ssa.SSAGetInstruction)
     */
    @Override
    public void visitGet(SSAGetInstruction instruction) {
      visitGetInternal(instruction.getDef(), instruction.getRef(), instruction.isStatic(), instruction.getDeclaredField());
    }

    protected void visitGetInternal(int lval, int ref, boolean isStatic, FieldReference field) {

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
        g.addNode(def);
        g.addNode(fKey);
        g.addEdge(def, fKey, AssignGlobalLabel.v());
      } else {
        PointerKey refKey = heapModel.getPointerKeyForLocal(node, ref);
        g.addNode(def);
        g.addNode(refKey);
        // TODO purely local optimizations
        g.addEdge(def, refKey, GetFieldLabel.make(f));
      }
    }

    /*
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitPut(com.ibm.domo.ssa.PutInstruction)
     */
    @Override
    public void visitPut(SSAPutInstruction instruction) {
      visitPutInternal(instruction.getVal(), instruction.getRef(), instruction.isStatic(), instruction.getDeclaredField());
    }

    public void visitPutInternal(int rval, int ref, boolean isStatic, FieldReference field) {
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
        g.addNode(use);
        g.addNode(fKey);
        g.addEdge(fKey, use, AssignGlobalLabel.v());
      } else {
        PointerKey refKey = heapModel.getPointerKeyForLocal(node, ref);
        g.addNode(use);
        g.addNode(refKey);
        g.addEdge(refKey, use, PutFieldLabel.make(f));
      }

    }

    /*
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitInvoke(com.ibm.domo.ssa.InvokeInstruction)
     */
    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {

      // for (int i = 0; i < instruction.getNumberOfUses(); i++) {
      // // just make nodes for parameters; we'll get to them when
      // // traversing
      // // from the callee
      // PointerKey use = heapModel.getPointerKeyForLocal(node, instruction.getUse(i));
      // g.addNode(use);
      // Set<SSAInvokeInstruction> s = MapUtil.findOrCreateSet(callParams, use);
      // s.add(instruction);
      // }
      //
      // // for any def'd values, keep track of the fact that they are def'd
      // // by a call
      // if (instruction.hasDef()) {
      // PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      // g.addNode(def);
      // callDefs.put(def, instruction);
      // }
      // PointerKey exc = heapModel.getPointerKeyForLocal(node, instruction.getException());
      // g.addNode(exc);
      // callDefs.put(exc, instruction);
    }

    /*
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
      g.addNode(iKey);
      g.addNode(def);
      g.addEdge(def, iKey, NewLabel.v());

      NewMultiDimInfo multiDimInfo = getInfoForNewMultiDim(instruction, heapModel, node);
      if (multiDimInfo != null) {
        for (Pair<PointerKey, InstanceKey> newInstr : multiDimInfo.newInstrs) {
          g.addNode(newInstr.fst);
          g.addNode(newInstr.snd);
          g.addEdge(newInstr.fst, newInstr.snd, NewLabel.v());
        }
        for (Pair<PointerKey, PointerKey> arrStoreInstr : multiDimInfo.arrStoreInstrs) {
          g.addNode(arrStoreInstr.fst);
          g.addNode(arrStoreInstr.snd);
          g.addEdge(arrStoreInstr.fst, arrStoreInstr.snd, PutFieldLabel.make(ArrayContents.v()));
        }
      }
    }

    /*
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitThrow(com.ibm.domo.ssa.ThrowInstruction)
     */
    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
      // don't do anything: we handle exceptional edges
      // in a separate pass
    }

    /*
     * @see com.ibm.domo.ssa.Instruction.Visitor#visitGetCaughtException(com.ibm.domo.ssa.GetCaughtExceptionInstruction)
     */
    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
      List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, getBasicBlock());
      PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());

      Set<IClass> types = SSAPropagationCallGraphBuilder.getCaughtExceptionTypes(instruction, ir);
      addExceptionDefConstraints(ir, node, peis, def, types);
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
          g.addNode(exceptionVar);
          g.addNode(e);
          g.addEdge(exceptionVar, e, AssignLabel.noFilter());

        } else if (pei instanceof SSAAbstractThrowInstruction) {
          SSAAbstractThrowInstruction s = (SSAAbstractThrowInstruction) pei;
          PointerKey e = heapModel.getPointerKeyForLocal(node, s.getException());
          g.addNode(exceptionVar);
          g.addNode(e);
          g.addEdge(exceptionVar, e, AssignLabel.noFilter());
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
                // probably due to exclusions
                continue;
              }
              assert ik instanceof ConcreteTypeKey : "uh oh: need to implement getCaughtException constraints for instance " + ik;
              ConcreteTypeKey ck = (ConcreteTypeKey) ik;
              IClass klass = ck.getType();
              if (PropagationCallGraphBuilder.catches(catchClasses, klass, cha)) {
                g.addNode(exceptionVar);
                g.addNode(ik);
                g.addEdge(exceptionVar, ik, NewLabel.v());
              }
            }
          }
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.domo.ssa.SSAInstruction.Visitor#visitPi(com.ibm.domo.ssa.SSAPiInstruction)
     */
    @Override
    public void visitPi(SSAPiInstruction instruction) {
      // for now, ignore condition and just treat it as a copy
      PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      PointerKey use = heapModel.getPointerKeyForLocal(node, instruction.getVal());
      g.addNode(def);
      g.addNode(use);
      g.addEdge(def, use, AssignLabel.noFilter());
    }

    public ISSABasicBlock getBasicBlock() {
      return basicBlock;
    }

    /**
     * The calling loop must call this in each iteration!
     */
    @Override
    public void setBasicBlock(ISSABasicBlock block) {
      basicBlock = block;
    }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
      PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());
      assert instruction.getType() == TypeReference.JavaLangClass;
      InstanceKey iKey = heapModel.getInstanceKeyForMetadataObject(instruction.getToken(), (TypeReference) instruction.getToken());

      g.addNode(iKey);
      g.addNode(def);
      g.addEdge(def, iKey, NewLabel.v());
    }
  }

  public static class NewMultiDimInfo {

    public final Collection<Pair<PointerKey, InstanceKey>> newInstrs;

    // pairs of (base pointer, stored val)
    public final Collection<Pair<PointerKey, PointerKey>> arrStoreInstrs;

    public NewMultiDimInfo(Collection<Pair<PointerKey, InstanceKey>> newInstrs,
        Collection<Pair<PointerKey, PointerKey>> arrStoreInstrs) {
      this.newInstrs = newInstrs;
      this.arrStoreInstrs = arrStoreInstrs;
    }

  }

  /**
   * collect information about the new instructions and putfield instructions used to model an allocation of a multi-dimensional
   * array. excludes the new instruction itself (i.e., the allocation of the top-level multi-dim array).
   */
  public static NewMultiDimInfo getInfoForNewMultiDim(SSANewInstruction instruction, HeapModel heapModel, CGNode node) {
    if (heapModel == null) {
      throw new IllegalArgumentException("null heapModel");
    }
    Collection<Pair<PointerKey, InstanceKey>> newInstrs = HashSetFactory.make();
    Collection<Pair<PointerKey, PointerKey>> arrStoreInstrs = HashSetFactory.make();
    InstanceKey iKey = heapModel.getInstanceKeyForAllocation(node, instruction.getNewSite());
    if (iKey == null) {
      // something went wrong. I hope someone raised a warning.
      return null;
    }
    IClass klass = iKey.getConcreteType();
    // if not a multi-dim array allocation, return null
    if (!klass.isArrayClass() || ((ArrayClass) klass).getElementClass() == null
        || !((ArrayClass) klass).getElementClass().isArrayClass()) {
      return null;
    }
    PointerKey def = heapModel.getPointerKeyForLocal(node, instruction.getDef());

    int dim = 0;
    InstanceKey lastInstance = iKey;
    PointerKey lastVar = def;
    while (klass != null && klass.isArrayClass()) {
      klass = ((ArrayClass) klass).getElementClass();
      // klass == null means it's a primitive
      if (klass != null && klass.isArrayClass()) {
        InstanceKey ik = heapModel.getInstanceKeyForMultiNewArray(node, instruction.getNewSite(), dim);
        PointerKey pk = heapModel.getPointerKeyForArrayContents(lastInstance);
        // if (DEBUG_MULTINEWARRAY) {
        // Trace.println("multinewarray constraint: ");
        // Trace.println(" pk: " + pk);
        // Trace.println(" ik: " + system.findOrCreateIndexForInstanceKey(ik)
        // + " concrete type " + ik.getConcreteType()
        // + " is " + ik);
        // Trace.println(" klass:" + klass);
        // }
        // g.addEdge(pk, ik, NewLabel.v());
        newInstrs.add(Pair.make(pk, ik));
        arrStoreInstrs.add(Pair.make(lastVar, pk));
        lastInstance = ik;
        lastVar = pk;
        dim++;
      }
    }

    return new NewMultiDimInfo(newInstrs, arrStoreInstrs);
  }
}
