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
package com.ibm.wala.analysis.exceptionanalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.InterproceduralExceptionFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public class IntraproceduralResult {
  private Set<TypeReference> exceptions;
  private Map<CGNode, Set<TypeReference>> intraproceduralExceptions;
  private CallGraph callGraph;
  private PointerAnalysis<InstanceKey> pointerAnalysis;
  private ClassHierarchy classHierachy;
  private InterproceduralExceptionFilter<SSAInstruction> filter;

  public IntraproceduralResult(CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha,
      InterproceduralExceptionFilter<SSAInstruction> filter) {
    this.callGraph = cg;
    this.pointerAnalysis = pointerAnalysis;
    this.classHierachy = cha;
    this.filter = filter;
    intraproceduralExceptions = new HashMap<>();
    exceptions = new HashSet<>();
    compute();
  }

  private void compute() {
    for (final CGNode node : callGraph) {
      intraproceduralExceptions.put(node, new HashSet<TypeReference>());

      IR ir = node.getIR();

      if (ir != null) {
        for (ISSABasicBlock block : ir.getControlFlowGraph()) {
          SSAInstruction throwingInstruction = getThrowingInstruction(block);
          if (throwingInstruction != null) {
            Set<TypeReference> thrownExceptions = collectThrownExceptions(node, throwingInstruction);
            Set<TypeReference> caughtExceptions = collectCaughtExceptions(node, block);
            Set<TypeReference> filteredExceptions = collectFilteredExceptions(node, throwingInstruction);

            thrownExceptions.removeAll(filteredExceptions);
            thrownExceptions.removeAll(caughtExceptions);
            intraproceduralExceptions.get(node).addAll(thrownExceptions);
          }
        }
      }
    }

    for (Set<TypeReference> exceptions : intraproceduralExceptions.values()) {
      this.exceptions.addAll(exceptions);
    }
  }

  private Set<TypeReference> collectFilteredExceptions(CGNode node, SSAInstruction throwingInstruction) {
    if (filter != null) {
      Set<TypeReference> filtered = new LinkedHashSet<>();
      Collection<FilteredException> filters = filter.getFilter(node).filteredExceptions(throwingInstruction);
      for (FilteredException filter : filters) {
        if (filter.isSubclassFiltered()) {
          for (IClass iclass : this.classHierachy.computeSubClasses(filter.getException())) {
            filtered.add(iclass.getReference());
          }
        } else {
          filtered.add(filter.getException());
        }
      }
      return filtered;
    } else {
      return Collections.emptySet();
    }

  }

  /**
   * Returns a set of exceptions, which might be thrown from this instruction
   * within this method.
   * 
   * This does include exceptions dispatched by throw instructions, but not
   * exceptions from method calls.
   * 
   * @param node
   * @param throwingInstruction
   * @return a set of exceptions, which might be thrown from this instruction
   *         within this method
   */
  private Set<TypeReference> collectThrownExceptions(final CGNode node, SSAInstruction throwingInstruction) {
    final LinkedHashSet<TypeReference> result = new LinkedHashSet<>();
    result.addAll(throwingInstruction.getExceptionTypes());

    throwingInstruction.visit(new Visitor() {
      @Override
      public void visitThrow(SSAThrowInstruction instruction) {
        addThrown(result, node, instruction);
      }
    });

    return result;
  }

  /**
   * Collects all exceptions, which could be dispatched by the throw
   * instruction, by using the pointer analysis. Adds the collected exceptions
   * to addTo.
   * 
   * @param addTo
   *          set to add the result
   * @param node
   *          node of the instruction
   * @param instruction
   *          the throw instruction
   */
  private void addThrown(LinkedHashSet<TypeReference> addTo, CGNode node, SSAThrowInstruction instruction) {
    int exceptionVariable = instruction.getException();
    PointerKey pointerKey = pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, exceptionVariable);
    Iterator it = pointerAnalysis.getHeapGraph().getSuccNodes(pointerKey);
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof InstanceKey) {
        InstanceKey instanceKey = (InstanceKey) next;
        IClass iclass = instanceKey.getConcreteType();
        addTo.add(iclass.getReference());
      } else {
        throw new IllegalStateException("Internal error: Expected InstanceKey, got " + next.getClass().getName());
      }
    }
  }

  /**
   * 
   * @param block
   * @return an instruction which may throw exceptions, or null if this block
   *         can't throw exceptions
   */
  private SSAInstruction getThrowingInstruction(ISSABasicBlock block) {
    SSAInstruction result = null;
    if (block.getLastInstructionIndex() >= 0) {
      SSAInstruction lastInstruction = block.getLastInstruction();
      if (lastInstruction != null) {
        result = lastInstruction;
      }
    }
    return result;
  }

  /**
   * @param node
   * @param block
   * @return a set of all exceptions which will be caught, if thrown by the
   *         given block.
   */
  private Set<TypeReference> collectCaughtExceptions(CGNode node, ISSABasicBlock block) {
    LinkedHashSet<TypeReference> result = new LinkedHashSet<TypeReference>();
    List<ISSABasicBlock> exceptionalSuccessors = node.getIR().getControlFlowGraph().getExceptionalSuccessors(block);
    for (ISSABasicBlock succ : exceptionalSuccessors) {
      if (succ.isCatchBlock()) {
        Iterator<TypeReference> it = succ.getCaughtExceptionTypes();
        while (it.hasNext()) {
          result.add(it.next());
        }
      }
    }

    Set<TypeReference> subClasses = new LinkedHashSet<>();
    for (TypeReference caught : result) {
      for (IClass iclass : this.classHierachy.computeSubClasses(caught)) {
        subClasses.add(iclass.getReference());
      }
    }

    result.addAll(subClasses);

    return result;
  }

  public Set<TypeReference> getCaughtExceptions(CGNode node, CallSiteReference callsite) {
    Set<TypeReference> result = new LinkedHashSet<>();

    IntSet iindices = node.getIR().getCallInstructionIndices(callsite);
    IntIterator it = iindices.intIterator();
    while (it.hasNext()) {
      int iindex = it.next();

      SSAInstruction instruction = node.getIR().getInstructions()[iindex];
      if (!((instruction instanceof SSAInvokeInstruction))) {
        throw new IllegalArgumentException("The given callsite dose not correspond to an invoke instruction." + instruction);
      }

      ISSABasicBlock block = node.getIR().getBasicBlockForInstruction(instruction);
      if (result.isEmpty()) {
        result.addAll(collectCaughtExceptions(node, block));
      } else {
        result.retainAll(collectCaughtExceptions(node, block));
      }
    }
    return result;
  }

  public Set<TypeReference> getIntraproceduralExceptions(CGNode node) {
    if (!callGraph.containsNode(node)) {
      throw new IllegalArgumentException("The given CG node has to be part " + "of the call graph given during construction.");
    }

    Set<TypeReference> result = intraproceduralExceptions.get(node);
    if (result == null) {
      throw new RuntimeException("Internal Error: No result for the given node.");
    }
    return result;
  }

  public Set<TypeReference> getExceptions() {
    return exceptions;
  }
}
