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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
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
import com.ibm.wala.util.ssa.InstructionByIIndexMap;

public class IntraproceduralExceptionAnalysis {
  private Set<TypeReference> exceptions;
  private Set<TypeReference> possiblyCaughtExceptions;
  private PointerAnalysis<InstanceKey> pointerAnalysis;
  private CGNode node;
  private ClassHierarchy classHierachy;
  private ExceptionFilter<SSAInstruction> filter;
  private IR ir;
  private boolean dummy = false;
  private Map<SSAInstruction, Boolean> allExceptionsCaught;

  public static IntraproceduralExceptionAnalysis newDummy() {
    return new IntraproceduralExceptionAnalysis();
  }

  /**
   * Create a dummy analysis.
   */
  private IntraproceduralExceptionAnalysis() {
    this.dummy = true;
    this.exceptions = Collections.emptySet();
  }

  /**
   * You can use this method, if you don't have a call graph, but want some
   * exception analysis. But as no pointer analysis is given, we can not
   * consider throw instructions.
   * 
   * @param ir
   * @param filter
   * @param cha
   */
  @Deprecated
  public IntraproceduralExceptionAnalysis(IR ir, ExceptionFilter<SSAInstruction> filter, ClassHierarchy cha) {
    this(ir, filter, cha, null, null);
  }

  /**
   * Create and compute intraprocedural exception analysis. (IR from
   * node.getIR() will be used.)
   * 
   * @param node
   * @param filter
   * @param cha
   * @param pointerAnalysis
   */
  public IntraproceduralExceptionAnalysis(CGNode node, ExceptionFilter<SSAInstruction> filter, ClassHierarchy cha,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    this(node.getIR(), filter, cha, pointerAnalysis, node);
  }

  /**
   * Create and compute intraprocedural exception analysis.
   * 
   * @param ir
   * @param filter
   * @param cha
   * @param pointerAnalysis
   * @param node
   */
  public IntraproceduralExceptionAnalysis(IR ir, ExceptionFilter<SSAInstruction> filter, ClassHierarchy cha,
      PointerAnalysis<InstanceKey> pointerAnalysis, CGNode node) {
    this.pointerAnalysis = pointerAnalysis;
    this.classHierachy = cha;
    this.filter = filter;
    this.ir = ir;
    this.node = node;
    this.exceptions = new LinkedHashSet<>();
    this.possiblyCaughtExceptions = new LinkedHashSet<>();
    this.allExceptionsCaught = new InstructionByIIndexMap<>();
    compute();
  }

  /**
   * Computes thrown exceptions for each basic block of all call graph nodes.
   * Everything, but invoke instructions, will be considered. This includes
   * filtered and caught exceptions.
   */
  private void compute() {
    if (ir != null) {
      for (ISSABasicBlock block : ir.getControlFlowGraph()) {
        SSAInstruction throwingInstruction = getThrowingInstruction(block);
        if (throwingInstruction != null && throwingInstruction.isPEI()) {
          Set<TypeReference> thrownExceptions = collectThrownExceptions(throwingInstruction);
          Set<TypeReference> caughtExceptions = collectCaughtExceptions(block);
          Set<TypeReference> filteredExceptions = collectFilteredExceptions(throwingInstruction);

          thrownExceptions.removeAll(filteredExceptions);
          thrownExceptions.removeAll(caughtExceptions);
          this.allExceptionsCaught.put(throwingInstruction, thrownExceptions.isEmpty());
          exceptions.addAll(thrownExceptions);
        }

        if (block.isCatchBlock()) {
          Iterator<TypeReference> it = block.getCaughtExceptionTypes();
          while (it.hasNext()) {
            possiblyCaughtExceptions.add(it.next());
          }
        }
      }
    }

    Set<TypeReference> subClasses = new LinkedHashSet<>();
    for (TypeReference caught : possiblyCaughtExceptions) {
      for (IClass iclass : this.classHierachy.computeSubClasses(caught)) {
        subClasses.add(iclass.getReference());
      }
    }

    possiblyCaughtExceptions.addAll(subClasses);
  }

  /**
   * Return all exceptions that could be returned from getCaughtExceptions
   * 
   * @return all exceptions that could be returned from getCaughtExceptions
   */
  public Set<TypeReference> getPossiblyCaughtExceptions() {
    return possiblyCaughtExceptions;
  }

  /**
   * Returns the set of exceptions, which are to be filtered for
   * throwingInstruction.
   * 
   * @param node
   * @param throwingInstruction
   * @return exceptions, which are to be filtered
   */
  private Set<TypeReference> collectFilteredExceptions(SSAInstruction throwingInstruction) {
    if (filter != null) {
      Set<TypeReference> filtered = new LinkedHashSet<>();
      Collection<FilteredException> filters = filter.filteredExceptions(throwingInstruction);
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
   * @param throwingInstruction
   * @return a set of exceptions, which might be thrown from this instruction
   *         within this method
   */
  public Set<TypeReference> collectThrownExceptions(SSAInstruction throwingInstruction) {
    final LinkedHashSet<TypeReference> result = new LinkedHashSet<>();
    result.addAll(throwingInstruction.getExceptionTypes());

    throwingInstruction.visit(new Visitor() {
      @Override
      public void visitThrow(SSAThrowInstruction instruction) {
        addThrown(result, instruction);
      }
    });

    return result;
  }

  /**
   * Collects all exceptions, which could be dispatched by the throw
   * instruction, using the pointer analysis. Adds the collected exceptions to
   * addTo.
   * 
   * @param addTo
   *          set to add the result
   * @param instruction
   *          the throw instruction
   */
  private void addThrown(LinkedHashSet<TypeReference> addTo, SSAThrowInstruction instruction) {
    int exceptionVariable = instruction.getException();

    if (pointerAnalysis != null) {
      PointerKey pointerKey = pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, exceptionVariable);
      Iterator<Object> it = pointerAnalysis.getHeapGraph().getSuccNodes(pointerKey);
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
  }

  /**
   * 
   * @param block
   * @return an instruction which may throw exceptions, or null if this block
   *         can't throw exceptions
   */
  public static SSAInstruction getThrowingInstruction(ISSABasicBlock block) {
    SSAInstruction result = null;
    if (block.getLastInstructionIndex() >= 0) {
      SSAInstruction lastInstruction = block.getLastInstruction();
      if (lastInstruction != null && lastInstruction.isPEI()) {
        result = lastInstruction;
      }
    }
    return result;
  }

  /**
   * @param block
   * @return a set of all exceptions which will be caught, if thrown by the
   *         given block.
   */
  private Set<TypeReference> collectCaughtExceptions(ISSABasicBlock block) {
    LinkedHashSet<TypeReference> result = new LinkedHashSet<>();
    List<ISSABasicBlock> exceptionalSuccessors = ir.getControlFlowGraph().getExceptionalSuccessors(block);
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

  /**
   * Returns all exceptions for the given call site in the given call graph
   * node, which will be caught.
   * 
   * @param callsite
   * @return caught exceptions
   */
  public Set<TypeReference> getCaughtExceptions(CallSiteReference callsite) {
    Set<TypeReference> result = null;
    if (dummy) {
      result = Collections.emptySet();
    } else {
      IntSet iindices = ir.getCallInstructionIndices(callsite);
      IntIterator it = iindices.intIterator();
      while (it.hasNext()) {
        int iindex = it.next();

        SSAInstruction instruction = ir.getInstructions()[iindex];
        if (!((instruction instanceof SSAInvokeInstruction))) {
          throw new IllegalArgumentException("The given callsite dose not correspond to an invoke instruction." + instruction);
        }

        ISSABasicBlock block = ir.getBasicBlockForInstruction(instruction);
        if (result == null) {
          result = new LinkedHashSet<>();
          result.addAll(collectCaughtExceptions(block));
        } else {
          result.retainAll(collectCaughtExceptions(block));
        }
      }
    }
    return result;
  }

  public boolean hasUncaughtExceptions(SSAInstruction instruction) {
    Boolean allCaught = this.allExceptionsCaught.get(instruction);
    return (allCaught == null ? true : !allCaught.booleanValue());
  }

  /**
   * Returns all exceptions which might be created and thrown but not caught or
   * filtered. (So this does not contain exceptions from invoked methods.)
   * 
   * If constructed without points-to-analysis, it does not contain exceptions
   * thrown by throw statements.
   * 
   * @return all exceptions created and thrown intraprocedural
   */
  public Set<TypeReference> getExceptions() {
    return exceptions;
  }
}
